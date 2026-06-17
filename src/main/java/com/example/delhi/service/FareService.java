package com.example.delhi.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class FareService {

    public int calculateFare(double distanceKm, LocalDate date) {
        boolean sunday = date.getDayOfWeek() == DayOfWeek.SUNDAY;

        if (sunday) {
            if (distanceKm <= 5) {
                return 11; 
            }else if (distanceKm <= 12) {
                return 21; 
            }else if (distanceKm <= 21) {
                return 32; 
            }else if (distanceKm <= 32) {
                return 43; 
            }else {
                return 54;
            }
        } else {
            if (distanceKm <= 2) {
                return 11; 
            }else if (distanceKm <= 5) {
                return 21; 
            }else if (distanceKm <= 12) {
                return 32; 
            }else if (distanceKm <= 21) {
                return 43; 
            }else if (distanceKm <= 32) {
                return 54; 
            }else {
                return 64;
            }
        }
    }
}
