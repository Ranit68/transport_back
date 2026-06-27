package com.example.delhi.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.delhi.dto.RouteDto;
import com.example.delhi.dto.StopDto;
import com.example.delhi.dto.StopTimeDto;
import com.example.delhi.dto.TripDto;
import com.example.delhi.entity.Edge;
import com.example.delhi.entity.MetroGraph;
import com.example.delhi.util.DistanceUtil;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetroGraphService {

    private static final int PAGE_SIZE = 1000;

    private final SupabaseService supabaseService;

    private final MetroGraph graph = new MetroGraph();

    private final Map<String, StopDto> stopMap = new ConcurrentHashMap<>();
    private final Map<String, RouteDto> routeMap = new ConcurrentHashMap<>();
    private final Map<String, TripDto> tripMap = new ConcurrentHashMap<>();

    private final Map<String, List<StopTimeDto>> tripStopTimesMap
            = new ConcurrentHashMap<>();

    private final Set<String> edgeTracker
            = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void initialize() {
        buildGraph();
    }

    public synchronized void buildGraph() {

        log.info("======================================");
        log.info("Building Delhi Metro Graph...");
        log.info("======================================");

        clearData();

        loadStops();
        loadRoutes();
        loadTrips();
        loadStopTimes();

        createEdges();

        log.info("======================================");
        log.info("Metro Graph Built Successfully");
        log.info("Stops Loaded : {}", stopMap.size());
        log.info("Routes Loaded: {}", routeMap.size());
        log.info("Trips Loaded : {}", tripMap.size());
        log.info("Graph Nodes  : {}", graph.getNodes().size());
        log.info("Graph Edges  : {}", graph.getTotalEdges());
        log.info("======================================");
    }

    private void clearData() {

        graph.clear();

        stopMap.clear();
        routeMap.clear();
        tripMap.clear();
        tripStopTimesMap.clear();

        edgeTracker.clear();
    }

    private void loadStops() {

        log.info("Loading stops...");

        List<StopDto> stops = supabaseService.fetchStops();

        for (StopDto stop : stops) {
            stopMap.put(stop.getStop_id(), stop);
            graph.addNode(stop.getStop_id());
        }

        log.info("Loaded {} stops", stopMap.size());
    }

    private void loadRoutes() {

        log.info("Loading routes...");

        List<RouteDto> routes = supabaseService.fetchRoutes();

        for (RouteDto route : routes) {

            routeMap.put(route.getRoute_id(), route);
        }

        log.info("Loaded {} routes", routeMap.size());
    }

    private void loadTrips() {

        log.info("Loading trips...");

        int offset = 0;

        while (true) {

            List<TripDto> batch
                    = supabaseService.fetchTrips(offset, PAGE_SIZE);

            if (batch == null || batch.isEmpty()) {
                break;
            }

            for (TripDto trip : batch) {

                tripMap.put(trip.getTrip_id(), trip);
            }

            log.info("Trips Loaded: {}", tripMap.size());

            if (batch.size() < PAGE_SIZE) {
                break;
            }

            offset += PAGE_SIZE;
        }

        log.info("Total Trips Loaded: {}", tripMap.size());
    }

    private void loadStopTimes() {

        log.info("Loading stop_times...");

        int offset = 0;

        while (true) {

            List<StopTimeDto> batch
                    = supabaseService.fetchStopTimes(offset, PAGE_SIZE);

            if (batch == null || batch.isEmpty()) {
                break;
            }

            for (StopTimeDto stopTime : batch) {

                tripStopTimesMap
                        .computeIfAbsent(
                                stopTime.getTrip_id(),
                                k -> new ArrayList<>())
                        .add(stopTime);
            }

            log.info("StopTimes Processed: {}",
                    offset + batch.size());

            if (batch.size() < PAGE_SIZE) {
                break;
            }

            offset += PAGE_SIZE;
        }
        for (List<StopTimeDto> stopTimes
                : tripStopTimesMap.values()) {

            stopTimes.sort(
                    Comparator.comparingInt(
                            StopTimeDto::getStop_sequence
                    )
            );
        }

        log.info("Trip StopTime Groups: {}",
                tripStopTimesMap.size());
    }

    private void createEdges() {

        log.info("Creating graph edges...");

        for (Map.Entry<String, List<StopTimeDto>> entry
                : tripStopTimesMap.entrySet()) {

            String tripId = entry.getKey();

            TripDto trip = tripMap.get(tripId);

            if (trip == null) {
                continue;
            }

            List<StopTimeDto> stopTimes = entry.getValue();

            for (int i = 0; i < stopTimes.size() - 1; i++) {

                StopTimeDto current = stopTimes.get(i);
                StopTimeDto next = stopTimes.get(i + 1);

                addMetroEdge(
                        current.getStop_id(),
                        next.getStop_id(),
                        trip
                );
            }
        }

        log.info("Graph Edges Created");
    }

    private void addMetroEdge(
            String fromStopId,
            String toStopId,
            TripDto trip
    ) {

        if (fromStopId == null || toStopId == null) {
            return;
        }

        if (fromStopId.equals(toStopId)) {
            return;
        }

        StopDto from = stopMap.get(fromStopId);
        StopDto to = stopMap.get(toStopId);

        double distanceKm
                = DistanceUtil.calculateDistanceKm(
                        from.getStop_lat(),
                        from.getStop_lon(),
                        to.getStop_lat(),
                        to.getStop_lon());

        int durationMin
                = (int) Math.ceil(
                        distanceKm / 35.0 * 60);

        if (from == null || to == null) {
            return;
        }

        String routeId = trip.getRoute_id();
        String edgeKey
                = fromStopId
                + "|"
                + toStopId
                + "|"
                + routeId;

        if (!edgeTracker.add(edgeKey)) {
            return;
        }

        RouteDto route = routeMap.get(routeId);

        String routeName
                = route != null
                        ? route.getRoute_long_name()
                        : routeId;

        Edge forward = new Edge(
                fromStopId,
                toStopId,
                routeId,
                routeName,
                trip.getTrip_id(),
                distanceKm,
                durationMin
        );

        Edge backward = new Edge(
                toStopId,
                fromStopId,
                routeId,
                routeName,
                trip.getTrip_id(),
                distanceKm,
                durationMin
        );

        graph.addEdge(forward);
        graph.addEdge(backward);
    }

    public Optional<List<Edge>> findShortestPath(
            String sourceStopId,
            String targetStopId
    ) {
        if (sourceStopId == null || targetStopId == null) {
            return Optional.empty();
        }

        if (!containsStop(sourceStopId) || !containsStop(targetStopId)) {
            return Optional.empty();
        }

        if (sourceStopId.equals(targetStopId)) {
            return Optional.of(Collections.emptyList());
        }

        class NodeState {
            final String stopId;
            final String routeId;
            final int totalDuration;

            NodeState(String stopId, String routeId, int totalDuration) {
                this.stopId = stopId;
                this.routeId = routeId;
                this.totalDuration = totalDuration;
            }
        }

        PriorityQueue<NodeState> pq = new PriorityQueue<>(
                Comparator.comparingInt(ns -> ns.totalDuration)
        );

        Map<String, Integer> minDurations = new HashMap<>();
        Map<String, Edge> previousEdge = new HashMap<>();
        Map<String, String> previousStateKey = new HashMap<>();

        pq.add(new NodeState(sourceStopId, null, 0));
        minDurations.put(sourceStopId + "|null", 0);

        NodeState bestTargetState = null;

        while (!pq.isEmpty()) {
            NodeState current = pq.poll();
            String currentKey = current.stopId + "|" + current.routeId;

            if (current.totalDuration > minDurations.getOrDefault(currentKey, Integer.MAX_VALUE)) {
                continue;
            }

            if (current.stopId.equals(targetStopId)) {
                if (bestTargetState == null || current.totalDuration < bestTargetState.totalDuration) {
                    bestTargetState = current;
                }
                continue;
            }

            for (Edge edge : graph.getAdjacentEdges(current.stopId)) {
                String nextStopId = edge.getToStopId();
                String nextRouteId = edge.getRouteId();
                
                int transferPenalty = (current.routeId != null && !current.routeId.equals(nextRouteId)) ? 5 : 0;
                int newDuration = current.totalDuration + edge.getDurationMin() + transferPenalty;

                String nextKey = nextStopId + "|" + nextRouteId;
                if (newDuration < minDurations.getOrDefault(nextKey, Integer.MAX_VALUE)) {
                    minDurations.put(nextKey, newDuration);
                    previousEdge.put(nextKey, edge);
                    previousStateKey.put(nextKey, currentKey);
                    pq.add(new NodeState(nextStopId, nextRouteId, newDuration));
                }
            }
        }

        if (bestTargetState == null) {
            return Optional.empty();
        }

        List<Edge> path = new ArrayList<>();
        String currentKey = bestTargetState.stopId + "|" + bestTargetState.routeId;
        String startKey = sourceStopId + "|null";

        while (!currentKey.equals(startKey)) {
            Edge edge = previousEdge.get(currentKey);
            if (edge == null) break;
            path.add(edge);
            currentKey = previousStateKey.get(currentKey);
        }
        Collections.reverse(path);

        return Optional.of(path);
    }

    public Optional<List<Edge>> findMinInterchangePath(
            String sourceStopId,
            String targetStopId
    ) {
        if (sourceStopId == null || targetStopId == null) {
            return Optional.empty();
        }

        if (!containsStop(sourceStopId) || !containsStop(targetStopId)) {
            return Optional.empty();
        }

        if (sourceStopId.equals(targetStopId)) {
            return Optional.of(Collections.emptyList());
        }

        class State {

            final String stopId;
            final String routeId;
            final int interchangeCount;
            final int totalDuration;

            State(String stopId, String routeId, int interchangeCount, int totalDuration) {
                this.stopId = stopId;
                this.routeId = routeId;
                this.interchangeCount = interchangeCount;
                this.totalDuration = totalDuration;
            }
        }

        record StateKey(String stopId, String routeId) {

        }

        PriorityQueue<State> queue = new PriorityQueue<>(Comparator
                .comparingInt((State s) -> s.interchangeCount)
                .thenComparingInt(s -> s.totalDuration));

        Map<StateKey, State> bestState = new HashMap<>();
        Map<StateKey, Edge> previousEdge = new HashMap<>();
        Map<StateKey, StateKey> previousState = new HashMap<>();

        State start = new State(sourceStopId, null, 0, 0);
        StateKey startKey = new StateKey(sourceStopId, null);
        bestState.put(startKey, start);
        queue.add(start);

        State bestTargetState = null;
        StateKey bestTargetKey = null;

        while (!queue.isEmpty()) {
            State state = queue.poll();
            StateKey currentKey = new StateKey(state.stopId, state.routeId);
            State known = bestState.get(currentKey);
            if (known == null || known.interchangeCount != state.interchangeCount || known.totalDuration != state.totalDuration) {
                continue;
            }

            if (state.stopId.equals(targetStopId)) {
                bestTargetState = state;
                bestTargetKey = currentKey;
                break;
            }

            for (Edge edge : graph.getAdjacentEdges(state.stopId)) {
                String nextStop = edge.getToStopId();
                String nextRoute = edge.getRouteId();
                int nextInterchange = state.routeId == null || state.routeId.equals(nextRoute)
                        ? state.interchangeCount
                        : state.interchangeCount + 1;
                int nextDuration = state.totalDuration + edge.getDurationMin();

                StateKey nextKey = new StateKey(nextStop, nextRoute);
                State candidate = new State(nextStop, nextRoute, nextInterchange, nextDuration);

                State existing = bestState.get(nextKey);
                if (existing == null || candidate.interchangeCount < existing.interchangeCount
                        || (candidate.interchangeCount == existing.interchangeCount && candidate.totalDuration < existing.totalDuration)) {
                    bestState.put(nextKey, candidate);
                    previousEdge.put(nextKey, edge);
                    previousState.put(nextKey, currentKey);
                    queue.add(candidate);
                }
            }
        }

        if (bestTargetState == null || bestTargetKey == null) {
            return Optional.empty();
        }

        List<Edge> path = new ArrayList<>();
        StateKey currentKey = bestTargetKey;
        while (!currentKey.equals(startKey)) {
            Edge edge = previousEdge.get(currentKey);
            if (edge == null) {
                break;
            }
            path.add(edge);
            currentKey = previousState.get(currentKey);
            if (currentKey == null) {
                break;
            }
        }
        Collections.reverse(path);
        return Optional.of(path);
    }

    public MetroGraph getGraph() {
        return graph;
    }

    public StopDto getStop(String stopId) {
        return stopMap.get(stopId);
    }

    public RouteDto getRoute(String routeId) {
        return routeMap.get(routeId);
    }

    public TripDto getTrip(String tripId) {
        return tripMap.get(tripId);
    }

    public List<Edge> getAdjacentEdges(String stopId) {

        return graph.getAdjacentEdges(stopId);
    }

    public Collection<StopDto> getAllStops() {

        return stopMap.values();
    }

    public Collection<RouteDto> getAllRoutes() {

        return routeMap.values();
    }

    public Collection<TripDto> getAllTrips() {

        return tripMap.values();
    }

    public List<StopTimeDto> getTripStopTimes(String tripId) {

        return tripStopTimesMap.getOrDefault(
                tripId,
                Collections.emptyList()
        );
    }

    public boolean containsStop(String stopId) {

        return stopMap.containsKey(stopId);
    }

    public int getStopCount() {

        return stopMap.size();
    }

    public int getRouteCount() {

        return routeMap.size();
    }

    public int getTripCount() {

        return tripMap.size();
    }

    public int getTripGroupCount() {

        return tripStopTimesMap.size();
    }

    public int getEdgeCount() {

        return graph.getTotalEdges();
    }

    public Set<String> getStopIds() {

        return stopMap.keySet();
    }
}
