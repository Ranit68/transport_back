package com.example.delhi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.delhi.dto.NearestMetroResponse;
import com.example.delhi.dto.NearestMetroRouteResponse;
import com.example.delhi.service.NearestMetroService;
import com.example.delhi.service.RouteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NearestMetroController {

    private final NearestMetroService nearestMetroService;
    private final RouteService routeService;

    @GetMapping("/nearest-metro")
    public ResponseEntity<NearestMetroResponse> nearestMetro(
            @RequestParam double lat,
            @RequestParam double lon) {

        NearestMetroResponse resp = nearestMetroService.findNearestMetro(lat, lon);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/nearest-metro-route")
    public ResponseEntity<NearestMetroRouteResponse> nearestMetroRoute(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam String to) throws Exception {

        NearestMetroRouteResponse resp = nearestMetroService.findNearestMetroRoute(lat, lon, to, routeService);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }
}
