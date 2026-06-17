package com.example.delhi.dto;

public class LiveTrainPosition {

    private String tripId;
    private String routeId;
    private String routeName;

    private String currentStopId;
    private String currentStopName;
    private Integer currentStopSequence;

    private String nextStopId;
    private String nextStopName;
    private Integer nextStopSequence;

    private String status;
    private Double progress;
    private Double currentLat;
    private Double currentLon;
    private Double bearing;
    private Long currentStopArrivalTimestamp;
    private Long currentStopDepartureTimestamp;
    private Long nextStopArrivalTimestamp;
    private Long nextStopDepartureTimestamp;
    private Long lastUpdatedEpochMs;

    public LiveTrainPosition() {
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

    public String getCurrentStopId() {
        return currentStopId;
    }

    public void setCurrentStopId(String currentStopId) {
        this.currentStopId = currentStopId;
    }

    public String getCurrentStopName() {
        return currentStopName;
    }

    public void setCurrentStopName(String currentStopName) {
        this.currentStopName = currentStopName;
    }

    public Integer getCurrentStopSequence() {
        return currentStopSequence;
    }

    public void setCurrentStopSequence(Integer currentStopSequence) {
        this.currentStopSequence = currentStopSequence;
    }

    public String getNextStopId() {
        return nextStopId;
    }

    public void setNextStopId(String nextStopId) {
        this.nextStopId = nextStopId;
    }

    public String getNextStopName() {
        return nextStopName;
    }

    public void setNextStopName(String nextStopName) {
        this.nextStopName = nextStopName;
    }

    public Integer getNextStopSequence() {
        return nextStopSequence;
    }

    public void setNextStopSequence(Integer nextStopSequence) {
        this.nextStopSequence = nextStopSequence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public Double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(Double currentLat) {
        this.currentLat = currentLat;
    }

    public Double getCurrentLon() {
        return currentLon;
    }

    public void setCurrentLon(Double currentLon) {
        this.currentLon = currentLon;
    }

    public Double getBearing() {
        return bearing;
    }

    public void setBearing(Double bearing) {
        this.bearing = bearing;
    }

    public Long getCurrentStopArrivalTimestamp() {
        return currentStopArrivalTimestamp;
    }

    public void setCurrentStopArrivalTimestamp(Long currentStopArrivalTimestamp) {
        this.currentStopArrivalTimestamp = currentStopArrivalTimestamp;
    }

    public Long getCurrentStopDepartureTimestamp() {
        return currentStopDepartureTimestamp;
    }

    public void setCurrentStopDepartureTimestamp(Long currentStopDepartureTimestamp) {
        this.currentStopDepartureTimestamp = currentStopDepartureTimestamp;
    }

    public Long getNextStopArrivalTimestamp() {
        return nextStopArrivalTimestamp;
    }

    public void setNextStopArrivalTimestamp(Long nextStopArrivalTimestamp) {
        this.nextStopArrivalTimestamp = nextStopArrivalTimestamp;
    }

    public Long getNextStopDepartureTimestamp() {
        return nextStopDepartureTimestamp;
    }

    public void setNextStopDepartureTimestamp(Long nextStopDepartureTimestamp) {
        this.nextStopDepartureTimestamp = nextStopDepartureTimestamp;
    }

    public Long getLastUpdatedEpochMs() {
        return lastUpdatedEpochMs;
    }

    public void setLastUpdatedEpochMs(Long lastUpdatedEpochMs) {
        this.lastUpdatedEpochMs = lastUpdatedEpochMs;
    }
}
