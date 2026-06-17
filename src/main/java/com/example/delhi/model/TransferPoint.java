package com.example.delhi.model;

public class TransferPoint {
     private String metroStopId;
     private String metroName;
private String busName;

    private String busStopId;

    private double distanceMeters;

    int walkingMinutes;

    public TransferPoint() {
    }

    public TransferPoint(String metroStopId,
                         String busStopId,
                         double distanceMeters, String metroName, String busName, int walkingMinutes) {

        this.metroStopId = metroStopId;
        this.busStopId = busStopId;
        this.distanceMeters = distanceMeters;
        this.metroName = metroName;
        this.busName = busName;
        this.walkingMinutes = walkingMinutes;
    
    }

    public String getMetroStopId() {
        return metroStopId;
    }

    public void setMetroStopId(String metroStopId) {
        this.metroStopId = metroStopId;
    }

    public String getBusStopId() {
        return busStopId;
    }

    public void setBusStopId(String busStopId) {
        this.busStopId = busStopId;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
    public String  getMetroName() {
        return metroName;
    }
    public String getBusName() {
        return busName;
    }
    public void setMetroName(String metroName) {
        this.metroName = metroName;
    }
    public void setBusName(String busName) {
        this.busName = busName;
    }
    public int getWalkingMinutesString(){
        return walkingMinutes;
    }
    public void setWalkingMinutes(int walkingMinutes) {
        this.walkingMinutes = walkingMinutes;
    }
}
