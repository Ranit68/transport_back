package com.example.delhi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AlertResponse {
    private String id;
    private String message;
    private String category;
    private long createdAt;
    private int trueVotes;
    private int falseVotes;
    private int reportCount;
}