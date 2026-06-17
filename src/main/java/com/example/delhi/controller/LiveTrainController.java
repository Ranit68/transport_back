package com.example.delhi.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.delhi.dto.LiveTrainPosition;
import com.example.delhi.service.LiveTrainService;

@RestController
@RequestMapping("/api/live-trains")
public class LiveTrainController {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Kolkata");

    private final LiveTrainService liveTrainService;

    public LiveTrainController(LiveTrainService liveTrainService) {
        this.liveTrainService = liveTrainService;
    }

    @GetMapping
    public List<LiveTrainPosition> getAllLiveTrains() {
        return liveTrainService.getLivePositions();
    }

    @GetMapping("/preview")
    public List<LiveTrainPosition> previewLiveTrains(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String time
    ) {
        LocalDate previewDate = date == null || date.isBlank()
                ? LocalDate.now(ZONE_ID)
                : LocalDate.parse(date);
        LocalTime previewTime = time == null || time.isBlank()
                ? LocalTime.now(ZONE_ID)
                : LocalTime.parse(time);

        return liveTrainService.getLivePositionsAt(previewDate, previewTime);
    }

    @GetMapping("/{tripId}")
    public LiveTrainPosition getLiveTrain(@PathVariable String tripId) {
        LiveTrainPosition position = liveTrainService.getLivePosition(tripId);
        if (position == null) {
            return new LiveTrainPosition();
        }
        return position;
    }
}
