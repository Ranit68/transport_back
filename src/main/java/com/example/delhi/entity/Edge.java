package com.example.delhi.entity;

import java.util.Objects;

public class Edge {

    private String fromStopId;
    private String toStopId;
    private String routeId;
    private String routeName;
    private String tripId;
    private double distanceKm;
    private int durationMin;  

    public Edge() {
    }

    public Edge(
            String fromStopId,
            String toStopId,
            String routeId,
            String routeName,
            String tripId,
            double distanceKm,
            int durationMin
    ) {
        this.fromStopId = fromStopId;
        this.toStopId = toStopId;
        this.routeId = routeId;
        this.routeName = routeName;
        this.tripId = tripId;
        this.distanceKm = distanceKm;
        this.durationMin = durationMin;
        
    
    }

    public Edge(
            String fromStopId,
            String toStopId,
            String routeId,
            String routeName,
            String tripId
    ) {
        this.fromStopId = fromStopId;
        this.toStopId = toStopId;
        this.routeId = routeId;
        this.routeName = routeName;
        this.tripId = tripId;
        
    
    }

    public String getFromStopId() {
        return fromStopId;
    }
    public double getDistanceKm() {
        return distanceKm;
    }
    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }
    public int getDurationMin() {
        return durationMin;
    }
    public void setDurationMin(int durationMin) {
        this.durationMin = durationMin;
    }

    public void setFromStopId(String fromStopId) {
        this.fromStopId = fromStopId;
    }

    public String getToStopId() {
        return toStopId;
    }

    public void setToStopId(String toStopId) {
        this.toStopId = toStopId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        Edge edge = (Edge) o;
        return Objects.equals(fromStopId, edge.fromStopId)
                && Objects.equals(toStopId, edge.toStopId)
                && Objects.equals(routeId, edge.routeId)
                && Objects.equals(routeName, edge.routeName)
                && Objects.equals(tripId, edge.tripId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromStopId, toStopId, routeId, routeName, tripId);
    }

    @Override
    public String toString() {
        return "Edge{" +
                "fromStopId='" + fromStopId + '\'' +
                ", toStopId='" + toStopId + '\'' +
                ", routeId='" + routeId + '\'' +
                ", routeName='" + routeName + '\'' +
                ", tripId='" + tripId + '\'' +
                '}';
    }
}
