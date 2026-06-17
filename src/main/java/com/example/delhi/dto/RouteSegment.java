package com.example.delhi.dto;

import java.util.List;

public class RouteSegment {

    private String line;
    private String from;
    private String to;
    private List<String> stations;

    public RouteSegment() {
    }

    public RouteSegment(String line, String from, String to, List<String> stations) {
        this.line = line;
        this.from = from;
        this.to = to;
        this.stations = stations;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public List<String> getStations() {
        return stations;
    }

    public void setStations(List<String> stations) {
        this.stations = stations;
    }
}
