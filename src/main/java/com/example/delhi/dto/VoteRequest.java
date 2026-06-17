package com.example.delhi.dto;

import lombok.Data;

@Data
public class VoteRequest {

    private String userId;

    private String vote;
}