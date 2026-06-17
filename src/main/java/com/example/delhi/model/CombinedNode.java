package com.example.delhi.model;

public class CombinedNode {
     private String id;

    private String name;

    private TransportMode mode;

    private double latitude;

    private double longitude;

    public CombinedNode() {}

    public CombinedNode(String id,
                        String name,
                        TransportMode mode,
                        double latitude,
                        double longitude) {

        this.id = id;
        this.name = name;
        this.mode = mode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransportMode getMode() {
        return mode;
    }

    public void setMode(TransportMode mode) {
        this.mode = mode;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
