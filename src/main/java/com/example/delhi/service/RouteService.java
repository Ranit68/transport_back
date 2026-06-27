package com.example.delhi.service;

import com.example.delhi.dto.*;
import com.example.delhi.entity.Edge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class RouteService {

    @Autowired
    private MetroGraphService metroGraphService;

    @Autowired
    private FareService fareService;

    private static final String SUPABASE_URL
            = "https://pxjxzbbhrnozxmkxiyht.supabase.co";

    private static final String API_KEY
            = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB4anh6YmJocm5venhta3hpeWh0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODAyMDI5NTUsImV4cCI6MjA5NTc3ODk1NX0.gUX5enAnMKtvgU2HuunfoEooJFlGS9SP61_Klq0NGss";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String fullRouteName;

    private HttpHeaders getHeaders() {

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.setBearerAuth(API_KEY);

        return headers;
    }

    private String getResponse(String url) {

        HttpEntity<String> entity
                = new HttpEntity<>(getHeaders());

        ResponseEntity<String> response
                = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

        return response.getBody();
    }

    private StopDto getStopByName(String stopName)
            throws Exception {

        String encoded
                = URLEncoder.encode(stopName,
                        StandardCharsets.UTF_8);

        String url
                = SUPABASE_URL
                + "/rest/v1/stops"
                + "?stop_name=eq." + encoded
                + "&select=stop_id,stop_name";

        String json = getResponse(url);

        List<StopDto> stops
                = objectMapper.readValue(
                        json,
                        new TypeReference<List<StopDto>>() {
                });

        if (stops.isEmpty()) {
            return null;
        }

        return stops.get(0);
    }

    private List<StopTimeDto> getStopTimesByStop(
            String stopId) throws Exception {

        String url
                = SUPABASE_URL
                + "/rest/v1/stop_times"
                + "?stop_id=eq." + stopId
                + "&select=trip_id,stop_sequence";

        String json = getResponse(url);

        return objectMapper.readValue(
                json,
                new TypeReference<List<StopTimeDto>>() {
        });
    }

    private String getRouteName(String tripId)
            throws Exception {

        String tripUrl
                = SUPABASE_URL
                + "/rest/v1/trips"
                + "?trip_id=eq." + tripId
                + "&select=route_id";

        String tripJson
                = getResponse(tripUrl);

        List<TripDto> trips
                = objectMapper.readValue(
                        tripJson,
                        new TypeReference<List<TripDto>>() {
                });

        if (trips.isEmpty()) {
            return "";
        }

        String routeId
                = trips.get(0).getRoute_id();

        String routeUrl
                = SUPABASE_URL
                + "/rest/v1/routes"
                + "?route_id=eq." + routeId
                + "&select=route_long_name";

        String routeJson
                = getResponse(routeUrl);

        List<RouteDto> routes
                = objectMapper.readValue(
                        routeJson,
                        new TypeReference<List<RouteDto>>() {
                });

        if (routes.isEmpty()) {
            return "";
        }

        return routes.get(0).getRoute_long_name();
    }

    public RouteResponse findRoute(String from, String to)
            throws Exception {

        StopDto source = getStopByName(from);
        StopDto destination = getStopByName(to);

        if (source == null || destination == null) {
            return null;
        }

        Optional<List<Edge>> optionalPath = metroGraphService.findShortestPath(
                source.getStop_id(),
                destination.getStop_id()
        );

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
            StopDto nextStop = metroGraphService.getStop(edge.getToStopId());
            stations.add(nextStop != null ? nextStop.getStop_name() : edge.getToStopId());
        }

        List<RouteSegment> segments = new ArrayList<>();
        List<String> interchangePoints = new ArrayList<>();

        String currentLine = normalizeLineName(path.get(0).getRouteName());
        String segmentFrom = source.getStop_name();
        List<String> segmentStations = new ArrayList<>();
        segmentStations.add(source.getStop_name());

        for (Edge edge : path) {
            StopDto nextStop = metroGraphService.getStop(edge.getToStopId());
            String nextStopName = nextStop != null ? nextStop.getStop_name() : edge.getToStopId();

            String nextLine
                    = normalizeLineName(edge.getRouteName());

            if (!nextLine.equals(currentLine)) {

                String transferStation
                        = metroGraphService
                                .getStop(edge.getFromStopId())
                                .getStop_name();

                interchangePoints.add(transferStation);

                segments.add(
                        new RouteSegment(
                                currentLine,
                                segmentFrom,
                                transferStation,
                                new ArrayList<>(segmentStations)
                        )
                );

                currentLine = nextLine;

                segmentFrom = transferStation;

                segmentStations = new ArrayList<>();
                segmentStations.add(transferStation);
            }
            segmentStations.add(nextStopName);
        }

        String lastStop = destination.getStop_name();
        if (!segmentStations.isEmpty()) {
            segments.add(new RouteSegment(
                    currentLine,
                    segmentFrom,
                    lastStop,
                    new ArrayList<>(segmentStations)
            ));
        }

        boolean direct = segments.size() == 1;
        String routeLines = String.join(" → ",
                segments.stream()
                        .map(RouteSegment::getLine)
                        .distinct()
                        .toList());

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

    public List<RouteResponse> findRouteOptions(String from, String to)
            throws Exception {

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
        Optional<List<Edge>> minInterchangePath = metroGraphService.findMinInterchangePath(
                source.getStop_id(),
                destination.getStop_id()
        );

        if (minInterchangePath.isPresent()) {
            List<Edge> path = minInterchangePath.get();

            for (Edge edge : path) {

            }
            if (!path.isEmpty()) {
                RouteResponse minInterchange = buildRouteResponse(
                        source,
                        destination,
                        path,
                        "Minimum Interchange"
                );

                if (minInterchange != null && !isSamePath(bestTime, minInterchange)) {
                    options.add(minInterchange);
                }
            }
        }

        return options;
    }

    private boolean isSamePath(RouteResponse first, RouteResponse second) {
        if (first == null || second == null) {
            return false;
        }
        return Objects.equals(first.getStations(), second.getStations())
                && Objects.equals(first.getInterchangePoints(), second.getInterchangePoints())
                && Objects.equals(first.getLine(), second.getLine());
    }

    private RouteResponse buildRouteResponse(
            StopDto source,
            StopDto destination,
            List<Edge> path,
            String strategy
    ) throws Exception {
        if (source == null || destination == null || path == null || path.isEmpty()) {
            return null;
        }
        List<String> stations = new ArrayList<>();
        stations.add(source.getStop_name());
        for (Edge edge : path) {
            StopDto nextStop = metroGraphService.getStop(edge.getToStopId());
            stations.add(nextStop != null ? nextStop.getStop_name() : edge.getToStopId());
        }
        List<RouteSegment> segments = new ArrayList<>();
        List<String> interchangePoints = new ArrayList<>();
        String currentLine
                = normalizeLineName(path.get(0).getRouteName());

        String segmentFrom = source.getStop_name();

        List<String> segmentStations = new ArrayList<>();
        segmentStations.add(source.getStop_name());

        for (Edge edge : path) {

            StopDto nextStop
                    = metroGraphService.getStop(edge.getToStopId());

            String nextStopName
                    = nextStop != null
                            ? nextStop.getStop_name()
                            : edge.getToStopId();

            String nextLine
                    = normalizeLineName(edge.getRouteName());

            if (!nextLine.equals(currentLine)) {

                String transferStation
                        = metroGraphService
                                .getStop(edge.getFromStopId())
                                .getStop_name();

                interchangePoints.add(transferStation);

                segments.add(
                        new RouteSegment(
                                currentLine,
                                segmentFrom,
                                transferStation,
                                new ArrayList<>(segmentStations)
                        )
                );

                currentLine = nextLine;

                segmentFrom = transferStation;

                segmentStations = new ArrayList<>();
                segmentStations.add(transferStation);
            }

            segmentStations.add(nextStopName);
        }
        String lastStop = destination.getStop_name();
        if (!segmentStations.isEmpty()) {
            segments.add(new RouteSegment(
                    currentLine,
                    segmentFrom,
                    lastStop,
                    new ArrayList<>(segmentStations)
            ));
        }
        boolean direct = segments.size() == 1;
        String routeLines = String.join(" → ",
                segments.stream()
                        .map(RouteSegment::getLine)
                        .distinct()
                        .toList());
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

    private double estimateDistance(List<Edge> path) {
        return path.stream().mapToDouble(Edge::getDistanceKm).sum();
    }

    private double estimateDistance(int edgeCount) {
        return edgeCount * 1.4;
    }

    private String normalizeLineName(String routeLongName) {
        if (routeLongName == null) {
            return "Unknown Line";
        }

        String normalized = routeLongName.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("RED")) {
            return "Red Line";
        } else if (normalized.startsWith("BLUE")) {
            return "Blue Line";
        } else if (normalized.startsWith("YELLOW")) {
            return "Yellow Line";
        } else if (normalized.startsWith("GREEN")) {
            return "Green Line";
        } else if (normalized.startsWith("PINK")) {
            return "Pink Line";
        } else if (normalized.startsWith("MAGENTA")) {
            return "Magenta Line";
        } else if (normalized.startsWith("VIOLET")) {
            return "Violet Line";
        } else if (normalized.startsWith("ORANGE")) {
            return "Airport Express (Orange Line)";
        } else if (normalized.startsWith("GRAY")) {
            return "Gray Line";
        } else if (normalized.startsWith("RAPID")) {
            return "Rapid Metro";
        }
        return routeLongName;
    }

    public List<String> searchStations(String query)
            throws Exception {

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String url = SUPABASE_URL
                + "/rest/v1/stops"
                + "?stop_name=ilike.%"
                + encoded
                + "%"
                + "&select=stop_name"
                + "&order=stop_name"
                + "&limit=20";

        String json = getResponse(url);

        List<StopDto> stops = objectMapper.readValue(
                json,
                new TypeReference<List<StopDto>>() {
        }
        );

        List<String> result = new ArrayList<>();
        for (StopDto stop : stops) {
            result.add(stop.getStop_name());
        }
        return result;
    }

    public RouteResponse findDirectRoute(
            String from,
            String to) throws Exception {

        StopDto source = getStopByName(from);
        StopDto destination = getStopByName(to);

        if (source == null || destination == null) {
            return null;
        }

        List<StopTimeDto> sourceTrips
                = getStopTimesByStop(source.getStop_id());

        List<StopTimeDto> destinationTrips
                = getStopTimesByStop(destination.getStop_id());

        for (StopTimeDto s : sourceTrips) {

            for (StopTimeDto d : destinationTrips) {

                if (s.getTrip_id()
                        .equals(d.getTrip_id())) {

                    String tripId = s.getTrip_id();

                    int sourceSeq
                            = s.getStop_sequence();

                    int destinationSeq
                            = d.getStop_sequence();

                    String routeLongName
                            = getRouteName(tripId);
                    String line = routeLongName;

                    if (routeLongName.toUpperCase().startsWith("RED")) {
                        line = "Red Line";
                    } else if (routeLongName.toUpperCase().startsWith("BLUE")) {
                        line = "Blue Line";
                    } else if (routeLongName.toUpperCase().startsWith("YELLOW")) {
                        line = "Yellow Line";
                    } else if (routeLongName.toUpperCase().startsWith("GREEN")) {
                        line = "Green Line";
                    } else if (routeLongName.toUpperCase().startsWith("PINK")) {
                        line = "Pink Line";
                    } else if (routeLongName.toUpperCase().startsWith("MAGENTA")) {
                        line = "Magenta Line";
                    } else if (routeLongName.toUpperCase().startsWith("VIOLET")) {
                        line = "Violet Line";
                    } else if (routeLongName.toUpperCase().startsWith("ORANGE")) {
                        line = "Airport Express (Orange Line)";
                    } else if (routeLongName.toUpperCase().startsWith("GRAY")) {
                        line = "Gray Line";
                    } else if (routeLongName.toUpperCase().startsWith("RAPID")) {
                        line = "Rapid Metro";
                    }

                    List<String> stations
                            = new ArrayList<>();

                    String stopUrl
                            = SUPABASE_URL
                            + "/rest/v1/stop_times"
                            + "?trip_id=eq." + tripId
                            + "&select=stop_id,stop_sequence";

                    String stopJson
                            = getResponse(stopUrl);

                    List<StopTimeDto> tripStops
                            = objectMapper.readValue(
                                    stopJson,
                                    new TypeReference<List<StopTimeDto>>() {
                            });

                    tripStops.sort(
                            Comparator.comparingInt(
                                    StopTimeDto::getStop_sequence));

                    int min
                            = Math.min(sourceSeq,
                                    destinationSeq);

                    int max
                            = Math.max(sourceSeq,
                                    destinationSeq);

                    for (StopTimeDto st
                            : tripStops) {

                        if (st.getStop_sequence() >= min
                                && st.getStop_sequence() <= max) {

                            String stationUrl
                                    = SUPABASE_URL
                                    + "/rest/v1/stops"
                                    + "?stop_id=eq."
                                    + st.getStop_id()
                                    + "&select=stop_name";

                            String stationJson
                                    = getResponse(stationUrl);

                            List<StopDto> stopDtos
                                    = objectMapper.readValue(
                                            stationJson,
                                            new TypeReference<List<StopDto>>() {
                                    });

                            if (!stopDtos.isEmpty()) {

                                stations.add(
                                        stopDtos.get(0)
                                                .getStop_name());
                            }
                        }
                    }

                    String direction;

                    if (sourceSeq > destinationSeq) {
                        direction
                                = "Towards " + destination.getStop_name();
                    } else {
                        direction
                                = "Towards " + source.getStop_name();
                    }

                    int estimatedMinutes
                            = Math.abs(sourceSeq
                                    - destinationSeq) * 2;

                    int fare = fareService.calculateFare(
                            estimateDistance(Math.abs(sourceSeq - destinationSeq)),
                            LocalDate.now());

                    return new RouteResponse(
                            routeLongName,
                            line,
                            direction,
                            stations,
                            estimatedMinutes,
                            fare,
                            true,
                            0,
                            new ArrayList<>(),
                            new ArrayList<>()
                    );
                }
            }
        }
        return null;
    }
}
