package com.example.delhi.dto;

import java.util.ArrayList;
import java.util.List;

public class CombinedRouteOptionDto {

    private String strategy;

    private int estimatedMinutes;

    private int interchangeCount;

    private List<String> interchangePoints = new ArrayList<>();

    private List<String> stations = new ArrayList<>();

    private List<CombinedSegmentDto> segments = new ArrayList<>();

    public CombinedRouteOptionDto() {
    }

    public CombinedRouteOptionDto(
            String strategy,
            int estimatedMinutes,
            int interchangeCount,
            List<String> interchangePoints,
            List<String> stations,
            List<CombinedSegmentDto> segments) {

        this.strategy = strategy;
        this.estimatedMinutes = estimatedMinutes;
        this.interchangeCount = interchangeCount;
        this.interchangePoints = interchangePoints;
        this.stations = stations;
        this.segments = segments;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
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

    public List<String> getStations() {
        return stations;
    }

    public void setStations(List<String> stations) {
        this.stations = stations;
    }

    public List<CombinedSegmentDto> getSegments() {
        return segments;
    }

    public void setSegments(List<CombinedSegmentDto> segments) {
        this.segments = segments;
    }
}