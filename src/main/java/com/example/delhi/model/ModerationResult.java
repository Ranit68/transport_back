package com.example.delhi.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ModerationResult {

    private boolean allowed;

    private String reason;

    private double toxicityScore;

    private double relevanceScore;
}