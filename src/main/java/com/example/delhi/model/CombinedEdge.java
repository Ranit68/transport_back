package com.example.delhi.model;

public class CombinedEdge {
    private String from;

    private String to;

    private TransportMode mode;

    private String line;

    private int timeMinutes;

    public CombinedEdge() {}

    public CombinedEdge(String from,
                        String to,
                        TransportMode mode,
                        String line,
                        int timeMinutes) {

        this.from = from;
        this.to = to;
        this.mode = mode;
        this.line = line;
        this.timeMinutes = timeMinutes;
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

    public TransportMode getMode() {
        return mode;
    }

    public void setMode(TransportMode mode) {
        this.mode = mode;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public int getTimeMinutes() {
        return timeMinutes;
    }

    public void setTimeMinutes(int timeMinutes) {
        this.timeMinutes = timeMinutes;
    }
}
