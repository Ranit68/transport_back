package com.example.delhi.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JourneyRequest {
    private String source;

    private String destination;

    private String strategy;
}
