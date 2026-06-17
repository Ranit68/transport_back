package com.example.delhi.dto;

import lombok.Data;

@Data
public class AlertRequest {

    private String userId;

    private String message;
}