package com.example.delhi.dto;

import java.util.List;

public class NearestMetroRouteResponse {

    private double userLat;
    private double userLon;

    private StopDto nearestStation;
    private double distanceKm;
    private List<String> lines;

    private RouteResponse route;

    public NearestMetroRouteResponse() {
    }

    public double getUserLat() {
        return userLat;
    }

    public void setUserLat(double userLat) {
        this.userLat = userLat;
    }

    public double getUserLon() {
        return userLon;
    }

    public void setUserLon(double userLon) {
        this.userLon = userLon;
    }

    public StopDto getNearestStation() {
        return nearestStation;
    }

    public void setNearestStation(StopDto nearestStation) {
        this.nearestStation = nearestStation;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public RouteResponse getRoute() {
        return route;
    }

    public void setRoute(RouteResponse route) {
        this.route = route;
    }
}

