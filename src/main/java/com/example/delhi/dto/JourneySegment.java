package com.example.delhi.dto;

import java.util.List;

import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class JourneySegment {
    
    private String mode;

    private String line;

    private String from;

    private String to;

    private List<String> stops;

    private double distanceKm;

    private int durationMin;
}
