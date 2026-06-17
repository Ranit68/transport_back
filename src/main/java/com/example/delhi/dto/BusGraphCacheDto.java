package com.example.delhi.dto;

import java.util.List;
import java.util.Map;

public class BusGraphCacheDto {
    private List<StopDto> stops;
    private List<RouteDto> routes;
    private List<TripDto> trips;
    private Map<String, List<StopTimeDto>> tripStopTimes;

    public BusGraphCacheDto() {}

    public BusGraphCacheDto(List<StopDto> stops, List<RouteDto> routes, List<TripDto> trips, Map<String, List<StopTimeDto>> tripStopTimes) {
        this.stops = stops;
        this.routes = routes;
        this.trips = trips;
        this.tripStopTimes = tripStopTimes;
    }

    public List<StopDto> getStops() { return stops; }
    public void setStops(List<StopDto> stops) { this.stops = stops; }

    public List<RouteDto> getRoutes() { return routes; }
    public void setRoutes(List<RouteDto> routes) { this.routes = routes; }

    public List<TripDto> getTrips() { return trips; }
    public void setTrips(List<TripDto> trips) { this.trips = trips; }

    public Map<String, List<StopTimeDto>> getTripStopTimes() { return tripStopTimes; }
    public void setTripStopTimes(Map<String, List<StopTimeDto>> tripStopTimes) { this.tripStopTimes = tripStopTimes; }
}
