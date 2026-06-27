package com.example.delhi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityAlert {

    private String id;

    private String userId;

    private String message;

    private String category;

    private long createdAt;
    private int trueVotes;
    private int falseVotes;
    private int reportCount;
}
