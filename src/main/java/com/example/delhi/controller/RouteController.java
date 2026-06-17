package com.example.delhi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.delhi.service.RouteService;

@RestController
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping(value = "/api/stations", params = "q")
    public List<String> searchStation(@RequestParam String q) throws Exception {
        return routeService.searchStations(q);
    }

    @GetMapping("/api/route")
    public com.example.delhi.dto.RouteResponse getRoute(
            @RequestParam String from,
            @RequestParam String to) {
        try {
            com.example.delhi.dto.RouteResponse response = routeService.findRoute(from, to);
            if (response == null) {
                return new com.example.delhi.dto.RouteResponse(
                        "",
                        "No Route Found",
                        "",
                        null,
                        0
                );
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return new com.example.delhi.dto.RouteResponse(
                    "",
                    "Error",
                    e.getMessage(),
                    null,
                    0
            );
        }
    }

    @GetMapping("/api/route/options")
    public List<com.example.delhi.dto.RouteResponse> getRouteOptions(
            @RequestParam String from,
            @RequestParam String to) {
        try {
            return routeService.findRouteOptions(from, to);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(new com.example.delhi.dto.RouteResponse(
                    "",
                    "Error",
                    e.getMessage(),
                    null,
                    0
            ));
        }
    }
}
