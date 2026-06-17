package com.example.delhi.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.delhi.dto.AlertRequest;
import com.example.delhi.dto.AlertResponse;
import com.example.delhi.dto.ReportRequest;
import com.example.delhi.dto.VoteRequest;
import com.example.delhi.model.CommunityAlert;
import com.example.delhi.model.ModerationResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TextModerationService textModerationService;

    private static final String ALERT_LIST_KEY
            = "community:alerts";

    public AlertResponse postAlert(
            AlertRequest request) {

        if (request.getMessage() == null
                || request.getMessage().trim().isEmpty()) {

            throw new RuntimeException(
                    "Message cannot be empty");
        }

        ModerationResult result
                = textModerationService.analyze(
                        request.getMessage());

        if (!result.isAllowed()) {

            throw new RuntimeException(
                    result.getReason());
        }

        CommunityAlert alert
                = new CommunityAlert(
                        UUID.randomUUID().toString(),
                        request.getUserId(),
                        request.getMessage().trim(),
                        System.currentTimeMillis(),
                        0,
                        0,
                        0
                );

        String key
                = "alert:" + alert.getId();

        redisTemplate.opsForValue().set(
                key,
                alert,
                Duration.ofHours(1)
        );

        redisTemplate.opsForList().leftPush(
                ALERT_LIST_KEY,
                alert.getId()
        );

        return new AlertResponse(
                alert.getId(),
                alert.getMessage(),
                alert.getCreatedAt(),
                alert.getTrueVotes(),
                alert.getFalseVotes(),
                alert.getReportCount()
        );
    }

    public List<AlertResponse> getAlerts() {

        List<AlertResponse> response
                = new ArrayList<>();

        List<Object> ids
                = redisTemplate.opsForList()
                        .range(
                                ALERT_LIST_KEY,
                                0,
                                -1
                        );

        if (ids == null) {
            return response;
        }

        for (Object idObj : ids) {

            String id
                    = String.valueOf(idObj);

            CommunityAlert alert
                    = (CommunityAlert) redisTemplate.opsForValue()
                            .get("alert:" + id);

            if (alert == null) {
                continue;
            }

            response.add(
                    new AlertResponse(
                            alert.getId(),
                            alert.getMessage(),
                            alert.getCreatedAt(),
                            alert.getTrueVotes(),
                            alert.getFalseVotes(),
                            alert.getReportCount()
                    )
            );
        }

        return response;
    }

    public void voteAlert(
            String alertId,
            VoteRequest request) {

        String voteKey
                = "vote:"
                + alertId
                + ":"
                + request.getUserId();

        if (Boolean.TRUE.equals(
                redisTemplate.hasKey(voteKey))) {

            throw new RuntimeException(
                    "Already voted");
        }

        CommunityAlert alert
                = (CommunityAlert) redisTemplate.opsForValue()
                        .get("alert:" + alertId);

        if (alert == null) {

            throw new RuntimeException(
                    "Alert not found");
        }

        if ("TRUE".equalsIgnoreCase(
                request.getVote())) {

            alert.setTrueVotes(
                    alert.getTrueVotes() + 1);

        } else {

            alert.setFalseVotes(
                    alert.getFalseVotes() + 1);
        }

        redisTemplate.opsForValue().set(
                "alert:" + alertId,
                alert,
                Duration.ofHours(1));

        redisTemplate.opsForValue().set(
                voteKey,
                "voted",
                Duration.ofHours(24));
    }

    public void reportAlert(
            String alertId,
            ReportRequest request) {

        String reportKey
                = "report:"
                + alertId
                + ":"
                + request.getUserId();

        if (Boolean.TRUE.equals(
                redisTemplate.hasKey(reportKey))) {

            throw new RuntimeException(
                    "Already reported");
        }

        CommunityAlert alert
                = (CommunityAlert) redisTemplate.opsForValue()
                        .get("alert:" + alertId);

        if (alert == null) {

            throw new RuntimeException(
                    "Alert not found");
        }

        alert.setReportCount(
                alert.getReportCount() + 1);

        redisTemplate.opsForValue().set(
                "alert:" + alertId,
                alert,
                Duration.ofHours(1));

        redisTemplate.opsForValue().set(
                reportKey,
                "reported",
                Duration.ofHours(24));

        if (alert.getReportCount() >= 5) {

            redisTemplate.delete(
                    "alert:" + alertId);
        }
    }
}
