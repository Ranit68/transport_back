package com.example.delhi.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActiveTrip {

    private String tripId;
    private String routeId;
    private String routeName;
    private String serviceId;
    private List<StopTimeDto> stopTimes = new ArrayList<>();

    public ActiveTrip() {
    }

    public ActiveTrip(String tripId, String routeId, String serviceId, List<StopTimeDto> stopTimes) {
        this.tripId = tripId;
        this.routeId = routeId;
        this.serviceId = serviceId;
        this.stopTimes = stopTimes == null ? new ArrayList<>() : stopTimes;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
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

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public List<StopTimeDto> getStopTimes() {
        return Collections.unmodifiableList(stopTimes);
    }

    public void setStopTimes(List<StopTimeDto> stopTimes) {
        this.stopTimes = stopTimes == null ? new ArrayList<>() : stopTimes;
    }

    public boolean isValid() {
        return stopTimes != null && stopTimes.size() >= 2;
    }
}
