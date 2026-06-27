package com.example.delhi.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import com.example.delhi.dto.RouteResponse;
import com.example.delhi.dto.StopDto;
import com.example.delhi.dto.RouteSegment;
import com.example.delhi.entity.Edge;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class BusRouteService {

    private final BusGraphService busGraphService;
    private final FareService fareService;

  @Value("${supabase.url}")
private String SUPABASE_URL;

@Value("${supabase.key}")
private String API_KEY;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BusRouteService(BusGraphService busGraphService, FareService fareService) {
        this.busGraphService = busGraphService;
        this.fareService = fareService;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.setBearerAuth(API_KEY);
        return headers;
    }

    private String getResponse(String url) {
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    private StopDto getStopByName(String stopName) throws Exception {

        if (stopName == null || stopName.isBlank()) {
            return null;
        }

        String normalized = normalizeStopName(stopName);

        Optional<StopDto> exactStop = busGraphService.getStopIds()
                .stream()
                .map(busGraphService::getStop)
                .filter(Objects::nonNull)
                .filter(stop -> stop.getStop_name() != null)
                .filter(stop -> normalizeStopName(stop.getStop_name()).equals(normalized))
                .filter(stop -> busGraphService.hasEdges(stop.getStop_id()))
                .findFirst();

        if (exactStop.isPresent()) {
            return exactStop.get();
        }

        return busGraphService.getStopIds()
                .stream()
                .map(busGraphService::getStop)
                .filter(Objects::nonNull)
                .filter(stop -> stop.getStop_name() != null)
                .filter(stop -> normalizeStopName(stop.getStop_name()).contains(normalized))
                .filter(stop -> busGraphService.hasEdges(stop.getStop_id()))
                .findFirst()
                .orElse(null);
    }

    public List<String> searchStations(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20");
        String url = SUPABASE_URL + "/rest/v1/bus_stops" + "?stop_name=ilike.%" + encoded + "%" + "&select=stop_name" + "&order=stop_name" + "&limit=100";

        String json = getResponse(url);
        List<StopDto> stops = objectMapper.readValue(json, new TypeReference<List<StopDto>>() {
        });

        return stops.stream()
                .map(StopDto::getStop_name)
                .filter(Objects::nonNull)
                .distinct()
                .limit(20)
                .toList();
    }

    public RouteResponse findRoute(String from, String to) throws Exception {

        if (!busGraphService.isGraphReady()) {
            return new RouteResponse("", "System Initializing", "The bus graph is still being built. Please try again in a moment.", "System", new ArrayList<>(), 0, 0, false, 0, new ArrayList<>(), new ArrayList<>());

        }

        StopDto source = getStopByName(from);
        StopDto destination = getStopByName(to);

        log.info("From '{}' -> ID={}",
                from,
                source == null ? null : source.getStop_id()
        );
        log.info("To '{}' -> ID={}",
                to,
                destination == null ? null : destination.getStop_id()
        );

        if (source == null || destination == null) {

            return new RouteResponse(
                    "",
                    "No Route Found",
                    "Could not find active stops for the supplied names.",
                    "Best Route",
                    new ArrayList<>(),
                    0,
                    0,
                    false,
                    0,
                    new ArrayList<>(),
                    new ArrayList<>());
        }

        log.info("Source {} edge count={}",
                source.getStop_id(),
                busGraphService.edgeCount(source.getStop_id()));

        log.info("Destination {} edge count={}",
                destination.getStop_id(),
                busGraphService.edgeCount(destination.getStop_id()));

        Optional<List<Edge>> optionalPath
                = busGraphService.findShortestPath(
                        source.getStop_id(),
                        destination.getStop_id());
        log.info("Shortest path found={}",
                optionalPath.isPresent());

        log.info("Shortest path found={}",
                optionalPath.isPresent());
        if (optionalPath.isEmpty()) {
            optionalPath = busGraphService.findMinInterchangePath(source.getStop_id(), destination.getStop_id());
        }
        if (optionalPath.isEmpty()) {
            return null;
        }

        List<Edge> path = optionalPath.get();

        if (path.isEmpty()) {
            return null;
        }

        List<String> stations = new ArrayList<>();
        stations.add(source.getStop_name());
        for (Edge edge : path) {
            StopDto nextStop = busGraphService.getStop(edge.getToStopId());
            stations.add(nextStop != null ? nextStop.getStop_name() : edge.getToStopId());
        }

        List<RouteSegment> segments = new ArrayList<>();
        List<String> interchangePoints = new ArrayList<>();

        String currentRouteId = path.get(0).getRouteId();
        String currentLine = normalizeLineName(path.get(0).getRouteName());

        String segmentFrom = source.getStop_name();
        List<String> segmentStations = new ArrayList<>();
        segmentStations.add(source.getStop_name());

        for (Edge edge : path) {
            StopDto nextStop = busGraphService.getStop(edge.getToStopId());
            String nextStopName = nextStop != null ? nextStop.getStop_name() : edge.getToStopId();

            if (!Objects.equals(edge.getRouteId(), currentRouteId) || !Objects.equals(normalizeLineName(edge.getRouteName()), currentLine)) {
                StopDto transferStop = busGraphService.getStop(edge.getFromStopId());

                String transferStation = transferStop != null ? transferStop.getStop_name() : edge.getFromStopId();

                interchangePoints.add(transferStation);
                segments.add(new RouteSegment(currentLine, segmentFrom, transferStation, new ArrayList<>(segmentStations)));

                currentRouteId = edge.getRouteId();
                currentLine = normalizeLineName(edge.getRouteName());
                segmentFrom = transferStation;
                segmentStations = new ArrayList<>();
                segmentStations.add(transferStation);
            }

            segmentStations.add(nextStopName);
        }

        String lastStop = destination.getStop_name();
        if (!segmentStations.isEmpty()) {
            segments.add(new RouteSegment(currentLine, segmentFrom, lastStop, new ArrayList<>(segmentStations)));
        }

        boolean direct = segments.size() == 1;
        String routeLines = String.join(" → ", segments.stream().map(RouteSegment::getLine).distinct().toList());

        double distanceKm = estimateDistance(path);
        int fare = fareService.calculateFare(distanceKm, LocalDate.now());

        int totalDuration = path.stream().mapToInt(Edge::getDurationMin).sum();

        return new RouteResponse(
                routeLines,
                routeLines,
                "Towards " + destination.getStop_name(),
                "Minimum Time",
                stations,
                totalDuration,
                fare,
                direct,
                Math.max(0, segments.size() - 1),
                interchangePoints,
                segments
        );
    }

    public List<RouteResponse> findRouteOptions(String from, String to) throws Exception {
        StopDto source = getStopByName(from);
        StopDto destination = getStopByName(to);
        if (source == null || destination == null) {
            return Collections.emptyList();
        }

        List<RouteResponse> options = new ArrayList<>();

        RouteResponse bestTime = findRoute(from, to);
        if (bestTime != null) {
            bestTime.setStrategy("Minimum Time");
            options.add(bestTime);

        }

        Optional<List<Edge>> minInterchangePath = busGraphService.findMinInterchangePath(source.getStop_id(), destination.getStop_id());
        if (minInterchangePath.isPresent()) {
            List<Edge> path = minInterchangePath.get();
            if (!path.isEmpty()) {
                RouteResponse minInterchange = buildRouteResponse(source, destination, path, "Minimum Interchange");
                if (minInterchange != null && !isSamePath(bestTime, minInterchange)) {
                    options.add(minInterchange);
                }
            }
        }

        return options;
    }

    private RouteResponse buildRouteResponse(StopDto source, StopDto destination, List<Edge> path, String strategy) {
        if (source == null || destination == null || path == null || path.isEmpty()) {
            return null;
        }

        List<String> stations = new ArrayList<>();
        stations.add(source.getStop_name());
        for (Edge edge : path) {
            StopDto nextStop = busGraphService.getStop(edge.getToStopId());
            stations.add(nextStop != null ? nextStop.getStop_name() : edge.getToStopId());
        }

        List<RouteSegment> segments = new ArrayList<>();
        List<String> interchangePoints = new ArrayList<>();

        String currentRouteId = path.get(0).getRouteId();
        String currentLine = normalizeLineName(path.get(0).getRouteName());
        String segmentFrom = source.getStop_name();
        List<String> segmentStations = new ArrayList<>();
        segmentStations.add(source.getStop_name());

        for (Edge edge : path) {
            StopDto nextStop = busGraphService.getStop(edge.getToStopId());
            String nextStopName = nextStop != null ? nextStop.getStop_name() : edge.getToStopId();

            if (!Objects.equals(edge.getRouteId(), currentRouteId) || !Objects.equals(normalizeLineName(edge.getRouteName()), currentLine)) {
                StopDto transferStop = busGraphService.getStop(edge.getFromStopId());
                String transferStation = transferStop != null ? transferStop.getStop_name() : edge.getFromStopId();

                interchangePoints.add(transferStation);
                segments.add(new RouteSegment(currentLine, segmentFrom, transferStation, new ArrayList<>(segmentStations)));

                currentRouteId = edge.getRouteId();
                currentLine = normalizeLineName(edge.getRouteName());
                segmentFrom = transferStation;
                segmentStations = new ArrayList<>();
                segmentStations.add(transferStation);
            }

            segmentStations.add(nextStopName);
        }

        String lastStop = destination.getStop_name();
        if (!segmentStations.isEmpty()) {
            segments.add(new RouteSegment(currentLine, segmentFrom, lastStop, new ArrayList<>(segmentStations)));
        }

        boolean direct = segments.size() == 1;
        String routeLines = String.join(" → ", segments.stream().map(RouteSegment::getLine).distinct().toList());

        double distanceKm = estimateDistance(path);
        int fare = fareService.calculateFare(distanceKm, LocalDate.now());

        int totalDuration = path.stream().mapToInt(Edge::getDurationMin).sum();

        return new RouteResponse(
                routeLines,
                routeLines,
                "Towards " + destination.getStop_name(),
                strategy,
                stations,
                totalDuration,
                fare,
                direct,
                Math.max(0, segments.size() - 1),
                interchangePoints,
                segments
        );
    }

    private boolean isSamePath(RouteResponse first, RouteResponse second) {
        if (first == null || second == null) {
            return false;
        }
        return Objects.equals(first.getStations(), second.getStations())
                && Objects.equals(first.getInterchangePoints(), second.getInterchangePoints())
                && Objects.equals(first.getLine(), second.getLine());
    }

    private double estimateDistance(List<Edge> path) {
        return path.stream().mapToDouble(Edge::getDistanceKm).sum();
    }

    private String normalizeLineName(String lineName) {
        if (lineName == null) {
            return "Unknown Line";
        }
        return lineName.toUpperCase(Locale.ROOT);
    }

    private String normalizeStopName(String stopName) {
        return stopName
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}
