package com.example.delhi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.delhi.dto.CombinedJourneyResponse;
import com.example.delhi.dto.JourneyRequest;
import com.example.delhi.service.CombinedJourneyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class JourneyController {

    private final CombinedJourneyService combinedJourneyService;

    @PostMapping({
            "/api/journey/search",
            "/api/combined/search",
            "/api/combined/journey"
    })
    public CombinedJourneyResponse journey(@RequestBody JourneyRequest request) {
        return combinedJourneyService.findJourney(request);
    }

    @GetMapping({
            "/api/journey/search",
            "/api/combined/search",
            "/api/combined/journey"
    })
    public CombinedJourneyResponse journey(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam(required = false, defaultValue = "FASTEST") String strategy) {

        return combinedJourneyService.findJourney(
                new JourneyRequest(source, destination, strategy));
    }
}
