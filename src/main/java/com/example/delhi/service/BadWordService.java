package com.example.delhi.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class BadWordService {

    private final Set<String> badWords
            = new HashSet<>();

    @PostConstruct
    public void loadWords() {

        try {

            ClassPathResource resource
                    = new ClassPathResource(
                            "badwords.txt");

            BufferedReader reader
                    = new BufferedReader(
                            new InputStreamReader(
                                    resource.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim()
                        .toLowerCase();

                if (!line.isEmpty()) {
                    badWords.add(line);
                }
            }

            reader.close();

            System.out.println(
                    "Loaded Bad Words = "
                    + badWords.size());

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public boolean containsBadWord(
            String text) {

        if (text == null) {
            return false;
        }

        String lower
                = text.toLowerCase();

        for (String word : badWords) {

            if (lower.contains(word)) {
                return true;
            }
        }

        return false;
    }
}
