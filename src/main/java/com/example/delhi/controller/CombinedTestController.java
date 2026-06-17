package com.example.delhi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.delhi.entity.Edge;
import com.example.delhi.service.CombinedGraphService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CombinedTestController {

    private final CombinedGraphService combinedGraphService;

    @GetMapping("/api/combined/test")
    public List<Edge> test() {

        return combinedGraphService
                .getAdjacentEdges("METRO_8");
    }

    @GetMapping("/api/combined/path")
    public Object test1() {

        return combinedGraphService
                .findShortestPath(
                        "METRO_8",
                        "BUS_750");
    }
}
