package com.example.delhi.dto;

import java.util.ArrayList;
import java.util.List;

public class NearestMetroStationDto {

    private String stationName;
    private double distanceKm;
    private List<String> lines = new ArrayList<>();

    public NearestMetroStationDto() {
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
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
}

