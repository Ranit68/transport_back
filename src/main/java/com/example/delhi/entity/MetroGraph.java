package com.example.delhi.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MetroGraph {

    private final Map<String, List<Edge>> adjacency = new ConcurrentHashMap<>();

    public void addNode(String stopId) {
        adjacency.computeIfAbsent(stopId, k -> new ArrayList<>());
    }

    public void addEdge(Edge edge) {
        if (edge == null || edge.getFromStopId() == null) {
            return;
        }

        adjacency.computeIfAbsent(edge.getFromStopId(), k -> new ArrayList<>())
                .add(edge);
    }

    public List<Edge> getAdjacentEdges(String stopId) {
        return adjacency.getOrDefault(stopId, Collections.emptyList());
    }

    public Set<String> getNodes() {
        return Collections.unmodifiableSet(adjacency.keySet());
    }

    public int getTotalEdges() {
        return adjacency.values().stream().mapToInt(List::size).sum();
    }

    public void clear() {
        adjacency.values().forEach(List::clear);
        adjacency.clear();
    }
}
