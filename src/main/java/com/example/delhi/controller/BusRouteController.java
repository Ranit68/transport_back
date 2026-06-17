package com.example.delhi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.delhi.dto.RouteResponse;
import com.example.delhi.service.BusRouteService;

@RestController
@RequestMapping("/api/bus")
public class BusRouteController {

    private final BusRouteService busRouteService;

    public BusRouteController(BusRouteService busRouteService) {
        this.busRouteService = busRouteService;
    }

    @GetMapping(value = "/stations", params = "q")
    public List<String> searchStation(@RequestParam String q) throws Exception {
        return busRouteService.searchStations(q);
    }

    @GetMapping("/route")
    public RouteResponse getRoute(
            @RequestParam String from,
            @RequestParam String to) {
        try {
            RouteResponse response = busRouteService.findRoute(from, to);
            if (response == null) {
                return new RouteResponse(
                        "",
                        "No Route Found",
                        "",
                        "Best Route",
                        new java.util.ArrayList<>(),
                        0,
                        0,
                        false,
                        0,
                        new java.util.ArrayList<>(),
                        new java.util.ArrayList<>()
                );
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return new RouteResponse(
                    "",
                    "Error",
                    e.getMessage(),
                    "Best Route",
                    new java.util.ArrayList<>(),
                    0,
                    0,
                    false,
                    0,
                    new java.util.ArrayList<>(),
                    new java.util.ArrayList<>()
            );

        }

    }

    @GetMapping("/route/options")
    public List<RouteResponse> getRouteOptions(
            @RequestParam String from,
            @RequestParam String to) {
        try {
            return busRouteService.findRouteOptions(from, to);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(new RouteResponse(
                    "",
                    "Error",
                    e.getMessage(),
                    "Best Route",
                    new java.util.ArrayList<>(),
                    0,
                    0,
                    false,
                    0,
                    new java.util.ArrayList<>(),
                    new java.util.ArrayList<>()
            ));
        }
    }

}
