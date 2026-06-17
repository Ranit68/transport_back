package com.example.delhi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StopDto {

    private String stop_id;
    private String stop_name;
    private Double stop_lat;
    private Double stop_lon;

    public StopDto() {
    }

    public String getStop_id() {
        return stop_id;
    }

    public void setStop_id(String stop_id) {
        this.stop_id = stop_id;
    }

    public String getStop_name() {
        return stop_name;
    }

    public void setStop_name(String stop_name) {
        this.stop_name = stop_name;
    }

    public Double getStop_lat() {
        return stop_lat;
    }

    public void setStop_lat(Double stop_lat) {
        this.stop_lat = stop_lat;
    }

    public Double getStop_lon() {
        return stop_lon;
    }

    public void setStop_lon(Double stop_lon) {
        this.stop_lon = stop_lon;
    }
}