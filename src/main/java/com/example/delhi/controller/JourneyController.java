package com.example.delhi.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.delhi.dto.CombinedJourneyResponse;
import com.example.delhi.dto.JourneyRequest;
import com.example.delhi.service.CombinedJourneyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/journey")
public class JourneyController {

    private final CombinedJourneyService combinedJourneyService;

    @PostMapping("/search")
    public CombinedJourneyResponse journey(@RequestBody JourneyRequest request) {
        return combinedJourneyService.findJourney(request);
    }
}
