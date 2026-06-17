package com.example.delhi.dto;

import java.util.List;


import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CombinedJourneyResponse {
private String source;

    private String destination;

    private double totalDistanceKm;

    private int totalTimeMin;

    private int interchanges;

    private int totalFare;

    private List<String> interchangePoints;

    private List<JourneySegment> segments;
}
