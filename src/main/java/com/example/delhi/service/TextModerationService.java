package com.example.delhi.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.delhi.model.ModerationResult;

@Service
public class TextModerationService {

    private final BadWordService badWordService;

    public TextModerationService(
            BadWordService badWordService) {

        this.badWordService = badWordService;
    }

    public ModerationResult analyze(
            String message) {

        if (message == null
                || message.isBlank()) {

            return new ModerationResult(
                    false,
                    "Message cannot be empty",
                    1.0,
                    0
            );
        }

        String normalized
                = normalize(message);

        if (badWordService
                .containsBadWord(normalized)) {

            return new ModerationResult(
                    false,
                    "Inappropriate language detected",
                    1.0,
                    0
            );
        }

        if (isSpam(normalized)) {

            return new ModerationResult(
                    false,
                    "Spam detected",
                    0.8,
                    0
            );
        }

        double relevance
                = calculateTransportRelevance(
                        normalized);

        return new ModerationResult(
                true,
                "OK",
                0,
                relevance
        );
    }

    private String normalize(
            String text) {

        return text.toLowerCase()
                .replace("@", "a")
                .replace("4", "a")
                .replace("3", "e")
                .replace("1", "i")
                .replace("$", "s")
                .replaceAll(
                        "[^a-z0-9 ]",
                        "");
    }

    private boolean isSpam(
            String text) {

        if (text.length() > 250) {
            return true;
        }

        String[] words
                = text.split("\\s+");

        if (words.length < 3) {
            return false;
        }

        int repeats = 0;

        for (int i = 1;
                i < words.length;
                i++) {

            if (words[i]
                    .equals(words[i - 1])) {

                repeats++;
            }
        }

        return repeats > 5;
    }

    private double calculateTransportRelevance(
            String text) {

        Set<String> keywords
                = Set.of(
                        "traffic",
                        "jam",
                        "accident",
                        "bus",
                        "metro",
                        "delay",
                        "road",
                        "station",
                        "route",
                        "train",
                        "roadblock",
                        "congestion"
                );

        int matches = 0;

        for (String word : keywords) {

            if (text.contains(word)) {
                matches++;
            }
        }

        return Math.min(
                1.0,
                matches / 3.0);
    }
}
