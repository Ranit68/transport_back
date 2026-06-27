package com.example.delhi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import com.example.delhi.dto.CombinedJourneyResponse;
import com.example.delhi.dto.JourneyRequest;
import com.example.delhi.dto.JourneySegment;
import com.example.delhi.entity.Edge;

import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class CombinedJourneyService {

    private final CombinedGraphService combinedGraphService;

    private final MetroGraphService metroGraphService;

    private final BusGraphService busGraphService;

    public CombinedJourneyResponse findJourney(
            JourneyRequest request) {
        
        log.info("Finding journey from {} to {} with strategy {}", 
                request.getSource(), request.getDestination(), request.getStrategy());

        List<String> sourceNodes
                = findAllNodeIds(request.getSource());

        List<String> destinationNodes
                = findAllNodeIds(request.getDestination());

        log.info("Found {} source nodes and {} destination nodes", 
                sourceNodes.size(), destinationNodes.size());

        if (sourceNodes.isEmpty()
                || destinationNodes.isEmpty()) {
            log.warn("No nodes found for source or destination");
            return null;
        }

        CombinedJourneyResponse best = null;

        for (String source : sourceNodes) {

            for (String destination : destinationNodes) {

                Optional<List<Edge>> optionalPath;

                if ("MIN_INTERCHANGE".equalsIgnoreCase(
                        request.getStrategy())) {

                    optionalPath
                            = combinedGraphService
                                    .findMinInterchangePath(
                                            source,
                                            destination);

                } else {

                    optionalPath
                            = combinedGraphService
                                    .findShortestPath(
                                            source,
                                            destination);
                }

                if (optionalPath.isEmpty()) {
                    continue;
                }

                CombinedJourneyResponse response
                        = buildResponse(
                                optionalPath.get());

                if (response == null) {
                    continue;
                }

                if (best == null) {

                    best = response;

                } else if ("MIN_INTERCHANGE"
                        .equalsIgnoreCase(request.getStrategy())) {

                    if (response.getInterchanges()
                            < best.getInterchanges()) {

                        best = response;

                    } else if (response.getInterchanges()
                            == best.getInterchanges()
                            && response.getTotalTimeMin()
                            < best.getTotalTimeMin()) {

                        best = response;
                    }

                } else {

                    if (response.getTotalTimeMin()
                            < best.getTotalTimeMin()) {

                        best = response;
                    }

                }
            }

        }

        return best;
    }

    private List<String> findAllNodeIds(String stopName) {

        List<String> nodes = new ArrayList<>();

        if (stopName == null || stopName.isBlank()) {
            return nodes;
        }

        String search = normalizeStopName(stopName);

        metroGraphService.getAllStops()
                .stream()
                .filter(s -> s.getStop_name() != null)
                .filter(s -> normalizeStopName(s.getStop_name()).equals(search))
                .forEach(s -> nodes.add("METRO_" + s.getStop_id()));

        busGraphService.getStopIds()
                .stream()
                .map(busGraphService::getStop)
                .filter(s -> s != null)
                .filter(s -> s.getStop_name() != null)
                .filter(s -> normalizeStopName(s.getStop_name()).equals(search))
                .filter(s -> busGraphService.hasEdges(s.getStop_id()))
                .forEach(s -> nodes.add("BUS_" + s.getStop_id()));

        if (!nodes.isEmpty()) {
            return nodes;
        }

        metroGraphService.getAllStops()
                .stream()
                .filter(s -> s.getStop_name() != null)
                .filter(s -> normalizeStopName(s.getStop_name()).contains(search))
                .forEach(s -> nodes.add("METRO_" + s.getStop_id()));

        busGraphService.getStopIds()
                .stream()
                .map(busGraphService::getStop)
                .filter(s -> s != null)
                .filter(s -> s.getStop_name() != null)
                .filter(s -> normalizeStopName(s.getStop_name()).contains(search))
                .filter(s -> busGraphService.hasEdges(s.getStop_id()))
                .forEach(s -> nodes.add("BUS_" + s.getStop_id()));

        return nodes;
    }

    private String normalizeStopName(String value) {
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private CombinedJourneyResponse buildResponse(List<Edge> path) {

        if (path == null || path.isEmpty()) {
            return null;
        }

        List<JourneySegment> segments = new ArrayList<>();
        List<String> interchangePoints = new ArrayList<>();

        double totalDistance = 0;
        int totalTime = 0;
        int fare = 0;

        Edge first = path.get(0);

        String currentMode = getMode(first);
        String currentLine = first.getRouteName();

        String segmentFrom = getStopName(first.getFromStopId());

        List<String> stops = new ArrayList<>();
        stops.add(segmentFrom);

        double segmentDistance = 0;
        int segmentDuration = 0;

        for (Edge edge : path) {

            totalDistance += edge.getDistanceKm();
            totalTime += edge.getDurationMin();

            segmentDistance += edge.getDistanceKm();
            segmentDuration += edge.getDurationMin();

            String nextMode = getMode(edge);
            String nextLine = edge.getRouteName();

            String nextStop = getStopName(edge.getToStopId());

            boolean segmentChanged
                    = !currentMode.equals(nextMode)
                    || !currentLine.equals(nextLine);

            if (segmentChanged) {

                String transferStop
                        = getStopName(edge.getFromStopId());

                interchangePoints.add(transferStop);

                segments.add(
                        new JourneySegment(
                                currentMode,
                                currentLine,
                                segmentFrom,
                                transferStop,
                                new ArrayList<>(stops),
                                segmentDistance
                                - edge.getDistanceKm(),
                                segmentDuration
                                - edge.getDurationMin()
                        ));

                segmentFrom = transferStop;

                stops = new ArrayList<>();
                stops.add(segmentFrom);

                currentMode = nextMode;
                currentLine = nextLine;

                segmentDistance = edge.getDistanceKm();
                segmentDuration = edge.getDurationMin();
            }

            stops.add(nextStop);
        }

        segments.add(
                new JourneySegment(
                        currentMode,
                        currentLine,
                        segmentFrom,
                        stops.get(stops.size() - 1),
                        new ArrayList<>(stops),
                        segmentDistance,
                        segmentDuration
                ));

        return new CombinedJourneyResponse(
                getStopName(first.getFromStopId()),
                getStopName(path.get(path.size() - 1).getToStopId()),
                totalDistance,
                totalTime,
                interchangePoints.size(),
                fare,
                interchangePoints,
                segments
        );
    }

    private String getMode(Edge edge) {

        if ("WALK".equals(edge.getRouteId())) {
            return "WALK";
        }

        if (edge.getFromStopId().startsWith("METRO_")) {
            return "METRO";
        }

        return "BUS";
    }

    private String getStopName(String nodeId) {

        if (nodeId == null) {
            return "Unknown";
        }

        if (nodeId.startsWith("METRO_")) {

            String id = nodeId.replace("METRO_", "");

            var stop = metroGraphService.getStop(id);

            return stop != null
                    ? stop.getStop_name()
                    : nodeId;
        }

        String id = nodeId.replace("BUS_", "");

        var stop = busGraphService.getStop(id);

        return stop != null
                ? stop.getStop_name()
                : nodeId;
    }
}
