package com.example.delhi.service;

import java.io.File;
import java.util.ArrayList;
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

import com.example.delhi.dto.BusGraphCacheDto;
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
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusGraphService {

    private static final int PAGE_SIZE = 1000;
    private static final String CACHE_FILE = "/home/ubuntu/transport_back/bus_graph_cache.json";

    private final BusSupabaseService supabaseService;

    private final MetroGraph graph = new MetroGraph();

    private final Map<String, StopDto> stopMap = new ConcurrentHashMap<>();
    private final Map<String, RouteDto> routeMap = new ConcurrentHashMap<>();
    private final Map<String, TripDto> tripMap = new ConcurrentHashMap<>();
    private final Map<String, List<StopTimeDto>> tripStopTimesMap = new ConcurrentHashMap<>();
    private final Set<String> edgeTracker = ConcurrentHashMap.newKeySet();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile boolean graphReady = false;

    public boolean isGraphReady() {
        return graphReady;
    }

    public boolean hasEdges(String stopId) {
        return !graph.getAdjacentEdges(stopId).isEmpty();
    }

    @PostConstruct
    public void initialize() {
        graphReady = false;
        Thread t = new Thread(() -> {
            try {
                if (loadFromCache()) {
                    log.info("Bus graph loaded from local cache");
                    createEdges();
                    graphReady = true;
                } else {
                    buildGraph();
                    saveToCache();
                }
            } catch (Exception e) {
                log.error("Failed to initialize Delhi bus graph", e);
            }
        }, "bus-graph-builder");
        t.setDaemon(true);
        t.start();
    }

    public int edgeCount(String stopId) {
        return graph.getAdjacentEdges(stopId).size();

    }

    private boolean loadFromCache() {
        File file = new File(CACHE_FILE);
        if (!file.exists()) {
            return false;
        }
        try {
            log.info("Loading bus graph from cache file...");
            BusGraphCacheDto cache = objectMapper.readValue(file, BusGraphCacheDto.class);

            cache.getStops().forEach(s -> {
                stopMap.put(s.getStop_id(), s);
                graph.addNode(s.getStop_id());
            });
            cache.getRoutes().forEach(r -> routeMap.put(r.getRoute_id(), r));
            cache.getTrips().forEach(t -> tripMap.put(t.getTrip_id(), t));
            tripStopTimesMap.putAll(cache.getTripStopTimes());

            return true;
        } catch (Exception e) {
            log.warn("Failed to load bus graph from cache", e);
            return false;
        }
    }

    private void saveToCache() {
        try {
            log.info("Saving bus graph to cache file (this may take a moment)...");
            BusGraphCacheDto cache = new BusGraphCacheDto(
                    new ArrayList<>(stopMap.values()),
                    new ArrayList<>(routeMap.values()),
                    new ArrayList<>(tripMap.values()),
                    new HashMap<>(tripStopTimesMap)
            );
            objectMapper.writeValue(new File(CACHE_FILE), cache);
            log.info("Bus graph saved to cache");
        } catch (Exception e) {
            log.error("Failed to save bus graph to cache", e);
        }
    }

    public synchronized void buildGraph() {
        graphReady = false;
        log.info("======================================");
        log.info("Building Delhi Bus Graph from Supabase...");
        log.info("======================================");

        clearData();

        loadStops();
        loadRoutes();
        loadTrips();
        loadStopTimesByTrips();

        createEdges();

        log.info("======================================");
        graphReady = true;
        log.info("Bus Graph Built Successfully");
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
        log.info("Loading bus_stops...");
        List<StopDto> stops = supabaseService.fetchStops();
        for (StopDto stop : stops) {
            stopMap.put(stop.getStop_id(), stop);
            graph.addNode(stop.getStop_id());
        }
        log.info("Loaded {} bus stops", stopMap.size());
        stopMap.values()
                .stream()
                .filter(s -> s.getStop_name() != null)
                .filter(s
                        -> s.getStop_name()
                        .toLowerCase()
                        .contains("karol"))
                .forEach(s
                        -> System.out.println(
                        "STOPMAP => "
                        + s.getStop_name()
                        + " ID="
                        + s.getStop_id()));
    }

    private void loadRoutes() {
        log.info("Loading bus_routes...");
        List<RouteDto> routes = supabaseService.fetchRoutes();
        for (RouteDto route : routes) {
            routeMap.put(route.getRoute_id(), route);
        }
        log.info("Loaded {} bus routes", routeMap.size());
    }

    private void loadTrips() {
        log.info("Loading bus_trips...");
        int offset = 0;
        while (true) {
            List<TripDto> batch = supabaseService.fetchTrips(offset, PAGE_SIZE);
            if (batch == null || batch.isEmpty()) {
                break;
            }

            for (TripDto trip : batch) {
                tripMap.put(trip.getTrip_id(), trip);
            }
            log.info("Bus Trips Loaded: {}", tripMap.size());
            offset += batch.size();
            if (batch.size() < PAGE_SIZE) {
                break;
            }
        }
    }

    private void loadStopTimesByTrips() {
        log.info("Loading bus_stop_times by trip batches...");
        List<String> allTripIds = new ArrayList<>(tripMap.keySet());
        int totalTrips = allTripIds.size();
        int batchSize = 50;
        int processedTrips = 0;
        int totalStopTimes = 0;

        for (int i = 0; i < totalTrips; i += batchSize) {
            int end = Math.min(i + batchSize, totalTrips);
            List<String> batchIds = allTripIds.subList(i, end);

            try {
                List<StopTimeDto> stopTimes = supabaseService.fetchStopTimesByTripIds(batchIds);
                if (stopTimes != null) {
                    for (StopTimeDto st : stopTimes) {
                        tripStopTimesMap
                                .computeIfAbsent(st.getTrip_id(), k -> new ArrayList<>())
                                .add(st);
                        totalStopTimes++;
                    }
                }
                processedTrips += batchIds.size();
                if (processedTrips % 500 == 0 || processedTrips == totalTrips) {
                    log.info("Processed {}/{} trips, loaded {} stop times...", processedTrips, totalTrips, totalStopTimes);
                }
            } catch (Exception e) {

                log.error(
        "Failed batch {}. Retrying...",
        i,
        e
    ); 
    try {
        Thread.sleep(5000);
    } catch (InterruptedException ignored) {
    }

    i -= batchSize;

            }
        }

        log.info("Sorting stop sequences...");
        for (List<StopTimeDto> stopTimes : tripStopTimesMap.values()) {
            stopTimes.sort(Comparator.comparingInt(StopTimeDto::getStop_sequence));
        }
        log.info("Finished loading {} stop times across {} trips", totalStopTimes, tripStopTimesMap.size());
        log.info("Trips Loaded       : {}", tripMap.size());
        log.info("Trips StopTimes    : {}", tripStopTimesMap.size());

        long missingTrips
                = tripMap.keySet()
                        .stream()
                        .filter(id
                                -> !tripStopTimesMap.containsKey(id))
                        .count();

        log.warn("Trips without stop_times: {}", missingTrips);

        /*
 * GTFS often contains trips that are never used.
 * Do NOT fail graph creation.
         */
    }

    private void createEdges() {
        log.info("Creating bus graph edges...");
        edgeTracker.clear();
        for (Map.Entry<String, List<StopTimeDto>> entry : tripStopTimesMap.entrySet()) {
            String tripId = entry.getKey();
            TripDto trip = tripMap.get(tripId);
            if (trip == null) {
                continue;
            }

            List<StopTimeDto> stopTimes = entry.getValue();
            for (int i = 0; i < stopTimes.size() - 1; i++) {
                addBusEdge(stopTimes.get(i).getStop_id(), stopTimes.get(i + 1).getStop_id(), trip);
            }
        }
        log.info("Bus graph edges created: {}", graph.getTotalEdges());
        log.info("Graph Edges  : {}", graph.getTotalEdges());

        String sampleStop = stopMap.keySet().iterator().next();
        log.info("Sample Stop: {}", sampleStop);
        log.info("Sample Adjacent Edges: {}",
                graph.getAdjacentEdges(sampleStop).size());
    }

    private void addBusEdge(String fromStopId,
            String toStopId,
            TripDto trip) {

        if (fromStopId == null
                || toStopId == null
                || fromStopId.equals(toStopId)) {
            return;
        }

        StopDto from = stopMap.get(fromStopId);
        StopDto to = stopMap.get(toStopId);

        if (from == null || to == null) {
            return;
        }

        double distanceKm
                = DistanceUtil.calculateDistanceKm(
                        from.getStop_lat(),
                        from.getStop_lon(),
                        to.getStop_lat(),
                        to.getStop_lon());

        int durationMin
                = (int) Math.ceil(
                        distanceKm / 20.0 * 60);

        RouteDto route = routeMap.get(trip.getRoute_id());

        String routeName = trip.getRoute_id();

        if (route != null) {
            if (route.getRoute_long_name() != null
                    && !route.getRoute_long_name().isBlank()) {

                routeName = route.getRoute_long_name();

            } else if (route.getRoute_short_name() != null
                    && !route.getRoute_short_name().isBlank()) {

                routeName = route.getRoute_short_name();
            }
        }

        /*
     * IMPORTANT:
     * Deduplicate by TRIP instead of ROUTE.
         */
        String forwardKey
                = fromStopId + "|"
                + toStopId + "|"
                + trip.getTrip_id();

        if (edgeTracker.add(forwardKey)) {

            graph.addEdge(new Edge(
                    fromStopId,
                    toStopId,
                    trip.getRoute_id(),
                    routeName,
                    trip.getTrip_id(),
                    distanceKm,
                    durationMin
            ));
        }

        /*
     * Add reverse edge.
         */
        String reverseKey
                = toStopId + "|"
                + fromStopId + "|"
                + trip.getTrip_id();

        if (edgeTracker.add(reverseKey)) {

            graph.addEdge(new Edge(
                    toStopId,
                    fromStopId,
                    trip.getRoute_id(),
                    routeName,
                    trip.getTrip_id(),
                    distanceKm,
                    durationMin
            ));
        }
    }

    public Optional<List<Edge>> findShortestPath(String sourceStopId, String targetStopId) {
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

        // Key is "stopId|routeId" to allow reaching the same stop via different routes
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
                // Continue to find potentially better routes to the target
                continue;
            }

            for (Edge edge : graph.getAdjacentEdges(current.stopId)) {
                String nextStopId = edge.getToStopId();
                String nextRouteId = edge.getRouteId();
                
                // Penalty for changing routes (e.g., 5 minutes)
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

    public Optional<List<Edge>> findMinInterchangePath(String sourceStopId, String targetStopId) {
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

        java.util.PriorityQueue<State> queue = new java.util.PriorityQueue<>(
                java.util.Comparator.comparingInt((State s) -> s.interchangeCount).thenComparingInt(s -> s.totalDuration)
        );

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
                int nextInterchange = state.routeId == null || state.routeId.equals(nextRoute) ? state.interchangeCount : state.interchangeCount + 1;
                int nextDuration = state.totalDuration + edge.getDurationMin();

                StateKey nextKey = new StateKey(nextStop, nextRoute);
                State candidate = new State(nextStop, nextRoute, nextInterchange, nextDuration);

                State existing = bestState.get(nextKey);
                if (existing == null || candidate.interchangeCount < existing.interchangeCount || (candidate.interchangeCount == existing.interchangeCount && candidate.totalDuration < existing.totalDuration)) {
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

    public StopDto getStop(String stopId) {
        return stopMap.get(stopId);
    }

    public boolean containsStop(String stopId) {
        return stopMap.containsKey(stopId);
    }

    public Set<String> getStopIds() {

        return stopMap.keySet();
    }

    public List<Edge> getAdjacentEdges(String stopId) {

        return graph.getAdjacentEdges(stopId);
    }

}
