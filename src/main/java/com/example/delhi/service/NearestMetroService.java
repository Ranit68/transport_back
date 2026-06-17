package com.example.delhi.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.delhi.dto.NearestMetroResponse;
import com.example.delhi.dto.NearestMetroRouteResponse;
import com.example.delhi.dto.NearestMetroStationDto;
import com.example.delhi.dto.RouteResponse;
import com.example.delhi.dto.StopDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NearestMetroService {

    private final MetroGraphService metroGraphService;

    public NearestMetroResponse findNearestMetro(double userLat, double userLon) {
        Collection<StopDto> stops = metroGraphService.getAllStops();
        if (stops == null || stops.isEmpty()) {
            return null;
        }

        List<NearestMetroStationDto> nearestStations = stops.stream()
                .filter(s -> s.getStop_lat() != null && s.getStop_lon() != null)
                .map(s -> {

                    double d
                            = distanceKm(
                                    userLat,
                                    userLon,
                                    s.getStop_lat(),
                                    s.getStop_lon());

                    NearestMetroStationDto dto
                            = new NearestMetroStationDto();

                    dto.setStationName(
                            s.getStop_name());

                    dto.setDistanceKm(
                            round3(d));

                    dto.setLines(
                            inferLinesForStation(
                                    s.getStop_id()));

                    return dto;
                })
                .sorted(Comparator.comparingDouble(NearestMetroStationDto::getDistanceKm))
                .limit(5)
                .toList();

        if (nearestStations.isEmpty()) {
            return null;
        }

        NearestMetroStationDto closest = nearestStations.get(0);

        NearestMetroResponse resp = new NearestMetroResponse();
        resp.setUserLat(userLat);
        resp.setUserLon(userLon);
        StopDto nearestStop
                = metroGraphService.getAllStops()
                        .stream()
                        .filter(s
                                -> s.getStop_name()
                                .equals(
                                        closest.getStationName()))
                        .findFirst()
                        .orElse(null);

        resp.setNearestStation(nearestStop);
        resp.setDistanceKm(closest.getDistanceKm());
        resp.setLines(closest.getLines());
        resp.setNearestStations(nearestStations);
        resp.setDirectionText("Go to nearest station: " + closest.getStationName() + " (" + closest.getDistanceKm() + " km away)");

        return resp;
    }

    public NearestMetroRouteResponse findNearestMetroRoute(double userLat, double userLon, String toStationName, RouteService routeService)
            throws Exception {

        NearestMetroResponse nearest = findNearestMetro(userLat, userLon);
        if (nearest == null || nearest.getNearestStation() == null) {
            return null;
        }

        RouteResponse route = routeService.findRoute(nearest.getNearestStation().getStop_name(), toStationName);
        if (route == null) {
            return null;
        }

        NearestMetroRouteResponse resp = new NearestMetroRouteResponse();
        resp.setUserLat(userLat);
        resp.setUserLon(userLon);
        resp.setNearestStation(nearest.getNearestStation());
        resp.setDistanceKm(nearest.getDistanceKm());
        resp.setLines(nearest.getLines());
        resp.setRoute(route);
        return resp;
    }

    private List<String> inferLinesForStation(String stopId) {
        if (stopId == null) {
            return List.of();
        }

        List<com.example.delhi.entity.Edge> adjacent = metroGraphService.getAdjacentEdges(stopId);
        if (adjacent == null || adjacent.isEmpty()) {
            return List.of();
        }

        Set<String> lines = new HashSet<>();
        for (var edge : adjacent) {
            if (edge.getRouteName() != null && !edge.getRouteName().isBlank()) {
                lines.add(edge.getRouteName());
            }
        }
        return new ArrayList<>(lines);
    }
    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0088; 
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
