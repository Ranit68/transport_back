package com.example.delhi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.delhi.dto.AlertRequest;
import com.example.delhi.dto.AlertResponse;
import com.example.delhi.dto.ReportRequest;
import com.example.delhi.dto.VoteRequest;
import com.example.delhi.service.CommunityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @PostMapping("/alerts")
    public AlertResponse postAlert(
            @RequestBody AlertRequest request) {

        return communityService.postAlert(request);
    }

    @PostMapping("/alerts/{id}/vote")
    public String voteAlert(
            @PathVariable String id,
            @RequestBody VoteRequest request) {

        communityService.voteAlert(
                id,
                request);

        return "Vote Added";
    }

    @PostMapping("/alerts/{id}/report")
    public String reportAlert(
            @PathVariable String id,
            @RequestBody ReportRequest request) {

        communityService.reportAlert(
                id,
                request);

        return "Report Added";
    }

    @GetMapping("/alerts")
    public List<AlertResponse> getAlerts() {

        return communityService.getAlerts();
    }
}
