package com.example.delhi.dto;

public class CombinedRouteResponse {

    private CombinedRouteOptionDto fastest;

    private CombinedRouteOptionDto minimumInterchange;

    public CombinedRouteResponse() {
    }

    public CombinedRouteResponse(
            CombinedRouteOptionDto fastest,
            CombinedRouteOptionDto minimumInterchange) {

        this.fastest = fastest;
        this.minimumInterchange = minimumInterchange;
    }

    public CombinedRouteOptionDto getFastest() {
        return fastest;
    }

    public void setFastest(
            CombinedRouteOptionDto fastest) {

        this.fastest = fastest;
    }

    public CombinedRouteOptionDto getMinimumInterchange() {
        return minimumInterchange;
    }

    public void setMinimumInterchange(
            CombinedRouteOptionDto minimumInterchange) {

        this.minimumInterchange = minimumInterchange;
    }
}