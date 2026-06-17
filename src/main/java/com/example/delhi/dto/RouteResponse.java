package com.example.delhi.dto;

import java.util.ArrayList;
import java.util.List;

public class RouteResponse {

    private String routeLongName;
    private String line;
    private String direction;
    private String strategy;
    private List<String> stations;
    private int estimatedMinutes;
    private int fare;
    private boolean direct;
    private int interchangeCount;
    private List<String> interchangePoints;
    private List<RouteSegment> segments;

    public RouteResponse() {
        this.stations = new ArrayList<>();
        this.interchangePoints = new ArrayList<>();
        this.segments = new ArrayList<>();
    }

    public RouteResponse(
            String routeLongName,
            String line,
            String direction,
            List<String> stations,
            int estimatedMinutes
    ) {
        this(routeLongName, line, direction, "Best Route", stations, estimatedMinutes, 0, true, 0, new ArrayList<>(), new ArrayList<>());
    }

    public RouteResponse(
            String routeLongName,
            String line,
            String direction,
            String strategy,
            List<String> stations,
            int estimatedMinutes
    ) {
        this(routeLongName, line, direction, strategy, stations, estimatedMinutes, 0, true, 0, new ArrayList<>(), new ArrayList<>());
    }

    public RouteResponse(
            String routeLongName,
            String line,
            String direction,
            List<String> stations,
            int estimatedMinutes,
            int fare,
            boolean direct,
            int interchangeCount,
            List<String> interchangePoints,
            List<RouteSegment> segments
    ) {
        this(routeLongName, line, direction, "Best Route", stations, estimatedMinutes, fare, direct, interchangeCount, interchangePoints, segments);
    }

    public RouteResponse(
            String routeLongName,
            String line,
            String direction,
            String strategy,
            List<String> stations,
            int estimatedMinutes,
            int fare,
            boolean direct,
            int interchangeCount,
            List<String> interchangePoints,
            List<RouteSegment> segments
    ) {
        this.routeLongName = routeLongName;
        this.line = line;
        this.direction = direction;
        this.strategy = strategy;
        this.stations = stations == null ? new ArrayList<>() : stations;
        this.estimatedMinutes = estimatedMinutes;
        this.fare = fare;
        this.direct = direct;
        this.interchangeCount = interchangeCount;
        this.interchangePoints = interchangePoints == null ? new ArrayList<>() : interchangePoints;
        this.segments = segments == null ? new ArrayList<>() : segments;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public List<String> getStations() {
        return stations;
    }

    public void setStations(List<String> stations) {
        this.stations = stations;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public int getFare() {
        return fare;
    }

    public void setFare(int fare) {
        this.fare = fare;
    }

    public String getRouteLongName() {
        return routeLongName;
    }

    public void setRouteLongName(String routeLongName) {
        this.routeLongName = routeLongName;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public int getInterchangeCount() {
        return interchangeCount;
    }

    public void setInterchangeCount(int interchangeCount) {
        this.interchangeCount = interchangeCount;
    }

    public List<String> getInterchangePoints() {
        return interchangePoints;
    }

    public void setInterchangePoints(List<String> interchangePoints) {
        this.interchangePoints = interchangePoints;
    }

    public List<RouteSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<RouteSegment> segments) {
        this.segments = segments;
    }
}
