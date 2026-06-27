package com.example.delhi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

import org.springframework.stereotype.Service;

import com.example.delhi.dto.StopDto;
import com.example.delhi.entity.Edge;
import com.example.delhi.model.TransferPoint;
import com.example.delhi.util.DistanceUtil;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CombinedGraphService {

    private final MetroGraphService metroGraphService;
    private final BusGraphService busGraphService;
    private final TransferBuilderService transferBuilderService;

    private final Map<String, List<Edge>> graph = new HashMap<>();

    public void buildCombinedGraph() {

        graph.clear();

        loadMetroEdges();

        loadBusEdges();

        loadTransferEdges();
    }

    public Optional<List<Edge>> findShortestPath(
            String sourceNodeId,
            String targetNodeId) {
        if (sourceNodeId == null || targetNodeId == null) {
            return Optional.empty();
        }
        if (!graph.containsKey(sourceNodeId)) {

            return Optional.empty();
        }
        if (!graph.containsKey(targetNodeId)) {

            return Optional.empty();
        }
        if (sourceNodeId.equals(targetNodeId)) {
            return Optional.of(Collections.emptyList());
        }

        class NodeState {
            final String nodeId;
            final String routeId;
            final int totalDuration;

            NodeState(String nodeId, String routeId, int totalDuration) {
                this.nodeId = nodeId;
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

        pq.add(new NodeState(sourceNodeId, null, 0));
        minDurations.put(sourceNodeId + "|null", 0);

        NodeState bestTargetState = null;

        while (!pq.isEmpty()) {

            NodeState current = pq.poll();
            String currentKey = current.nodeId + "|" + current.routeId;

            if (current.totalDuration > minDurations.getOrDefault(currentKey, Integer.MAX_VALUE)) {
                continue;
            }

            if (current.nodeId.equals(targetNodeId)) {
                if (bestTargetState == null || current.totalDuration < bestTargetState.totalDuration) {
                    bestTargetState = current;
                }
                continue;
            }

            for (Edge edge : getAdjacentEdges(current.nodeId)) {

                String nextNodeId = edge.getToStopId();
                String nextRouteId = edge.getRouteId();
                
                int transferPenalty = (current.routeId != null && !current.routeId.equals(nextRouteId)) ? 10 : 0;
                int newDuration = current.totalDuration + edge.getDurationMin() + transferPenalty;

                String nextKey = nextNodeId + "|" + nextRouteId;
                if (newDuration < minDurations.getOrDefault(nextKey, Integer.MAX_VALUE)) {
                    minDurations.put(nextKey, newDuration);
                    previousEdge.put(nextKey, edge);
                    previousStateKey.put(nextKey, currentKey);
                    pq.add(new NodeState(nextNodeId, nextRouteId, newDuration));
                }
            }
        }

        if (bestTargetState == null) {
            return Optional.empty();
        }

        List<Edge> path = new ArrayList<>();

        String currentKey = bestTargetState.nodeId + "|" + bestTargetState.routeId;
        String startKey = sourceNodeId + "|null";

        while (!currentKey.equals(startKey)) {

            Edge edge = previousEdge.get(currentKey);

            if (edge == null) {
                break;
            }

            path.add(edge);

            currentKey = previousStateKey.get(currentKey);
            if (currentKey == null) break;
        }

        Collections.reverse(path);

        return Optional.of(path);
    }

    public Optional<List<Edge>> findMinInterchangePath(
            String sourceNodeId,
            String targetNodeId) {

        if (sourceNodeId == null || targetNodeId == null) {
            return Optional.empty();
        }

        if (!graph.containsKey(sourceNodeId)
                || !graph.containsKey(targetNodeId)) {

            return Optional.empty();
        }

        if (sourceNodeId.equals(targetNodeId)) {
            return Optional.of(Collections.emptyList());
        }

        class State {

            final String stopId;

            final String routeId;

            final int interchangeCount;

            final int totalDuration;

            State(String stopId,
                    String routeId,
                    int interchangeCount,
                    int totalDuration) {

                this.stopId = stopId;
                this.routeId = routeId;
                this.interchangeCount = interchangeCount;
                this.totalDuration = totalDuration;
            }
        }

        record StateKey(
                String stopId,
                String routeId) {

        }

        PriorityQueue<State> queue
                = new PriorityQueue<>(
                        Comparator
                                .comparingInt(
                                        (State s)
                                        -> s.interchangeCount)
                                .thenComparingInt(
                                        s -> s.totalDuration));

        Map<StateKey, State> bestState
                = new HashMap<>();

        Map<StateKey, Edge> previousEdge
                = new HashMap<>();

        Map<StateKey, StateKey> previousState
                = new HashMap<>();

        State start
                = new State(
                        sourceNodeId,
                        null,
                        0,
                        0);

        StateKey startKey
                = new StateKey(
                        sourceNodeId,
                        null);

        bestState.put(startKey, start);

        queue.add(start);

        State bestTargetState = null;

        StateKey bestTargetKey = null;

        while (!queue.isEmpty()) {

            State state = queue.poll();

            StateKey currentKey
                    = new StateKey(
                            state.stopId,
                            state.routeId);

            State known
                    = bestState.get(currentKey);

            if (known == null
                    || known.interchangeCount
                    != state.interchangeCount
                    || known.totalDuration
                    != state.totalDuration) {

                continue;
            }

            if (state.stopId.equals(targetNodeId)) {

                bestTargetState = state;

                bestTargetKey = currentKey;

                break;
            }

            for (Edge edge
                    : getAdjacentEdges(state.stopId)) {

                String nextStop
                        = edge.getToStopId();

                String nextRoute
                        = edge.getRouteId();

                int nextInterchange
                        = state.routeId == null
                        || state.routeId.equals(nextRoute)
                        ? state.interchangeCount
                        : state.interchangeCount + 1;

                int nextDuration
                        = state.totalDuration + edge.getDurationMin();

                StateKey nextKey
                        = new StateKey(
                                nextStop,
                                nextRoute);

                State candidate
                        = new State(
                                nextStop,
                                nextRoute,
                                nextInterchange,
                                nextDuration);

                State existing
                        = bestState.get(nextKey);

                if (existing == null
                        || candidate.interchangeCount
                        < existing.interchangeCount
                        || (candidate.interchangeCount
                        == existing.interchangeCount
                        && candidate.totalDuration
                        < existing.totalDuration)) {

                    bestState.put(
                            nextKey,
                            candidate);

                    previousEdge.put(
                            nextKey,
                            edge);

                    previousState.put(
                            nextKey,
                            currentKey);

                    queue.add(candidate);
                }
            }
        }

        if (bestTargetState == null
                || bestTargetKey == null) {

            return Optional.empty();
        }

        List<Edge> path
                = new ArrayList<>();

        StateKey currentKey
                = bestTargetKey;

        while (!currentKey.equals(startKey)) {

            Edge edge
                    = previousEdge.get(currentKey);

            if (edge == null) {
                break;
            }

            path.add(edge);

            currentKey
                    = previousState.get(currentKey);

            if (currentKey == null) {
                break;
            }
        }

        Collections.reverse(path);

        return Optional.of(path);
    }

    public List<Edge> getAdjacentEdges(String nodeId) {

        return graph.getOrDefault(
                nodeId,
                Collections.emptyList());
    }

    private void addEdge(Edge edge) {

        graph.computeIfAbsent(
                edge.getFromStopId(),
                k -> new ArrayList<>())
                .add(edge);
    }

    private void loadMetroEdges() {

        for (String stopId : metroGraphService.getStopIds()) {

            String metroNode = "METRO_" + stopId;

            for (Edge edge
                    : metroGraphService.getAdjacentEdges(stopId)) {

                addEdge(new Edge(
                        metroNode,
                        "METRO_" + edge.getToStopId(),
                        edge.getRouteId(),
                        edge.getRouteName(),
                        edge.getTripId(),
                        edge.getDistanceKm(),
                        edge.getDurationMin()
                ));
            }
        }
    }

    private void loadBusEdges() {

        for (String stopId : busGraphService.getStopIds()) {

            String busNode = "BUS_" + stopId;

            for (Edge edge
                    : busGraphService.getAdjacentEdges(stopId)) {

                addEdge(new Edge(
                        busNode,
                        "BUS_" + edge.getToStopId(),
                        edge.getRouteId(),
                        edge.getRouteName(),
                        edge.getTripId(),
                        edge.getDistanceKm(),
                        edge.getDurationMin()
                ));
            }
        }
    }

    private void loadTransferEdges() {

        List<TransferPoint> transfers
                = transferBuilderService.buildTransfers();

        for (TransferPoint transfer : transfers) {
            StopDto metroStop
                    = metroGraphService.getStop(
                            transfer.getMetroStopId());

            StopDto busStop
                    = busGraphService.getStop(
                            transfer.getBusStopId());

            double walkDistance
                    = DistanceUtil.calculateDistanceKm(
                            metroStop.getStop_lat(),
                            metroStop.getStop_lon(),
                            busStop.getStop_lat(),
                            busStop.getStop_lon());
            int walkMinutes
                    = (int) Math.ceil(
                            walkDistance / 5.0 * 60);

            String metroNode
                    = "METRO_"
                    + transfer.getMetroStopId();

            String busNode
                    = "BUS_"
                    + transfer.getBusStopId();

            Edge metroToBus = new Edge(
                    metroNode,
                    busNode,
                    "WALK",
                    "Walk to Bus",
                    "TRANSFER",
                    walkDistance,
                    walkMinutes
            );

            Edge busToMetro = new Edge(
                    busNode,
                    metroNode,
                    "WALK",
                    "Walk to Metro",
                    "TRANSFER",
                    walkDistance,
                    walkMinutes
            );
            addEdge(metroToBus);

            addEdge(busToMetro);
        }
    }

    @PostConstruct
    public void init() {
        Thread t = new Thread(() -> {
            log.info("CombinedGraphService waiting for dependencies...");
            try {
                while (!busGraphService.isGraphReady()) {
                    Thread.sleep(5000);
                }
                log.info("Dependencies ready. Building Combined Graph...");
                buildCombinedGraph();
                log.info("Combined Graph Built Successfully");
            } catch (InterruptedException e) {
                log.error("Combined graph builder interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Failed to build combined graph", e);
            }
        }, "combined-graph-builder");
        t.setDaemon(true);
        t.start();
    }
}
