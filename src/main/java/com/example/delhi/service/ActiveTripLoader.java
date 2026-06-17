package com.example.delhi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.delhi.dto.ActiveTrip;
import com.example.delhi.dto.StopTimeDto;
import com.example.delhi.dto.TripDto;

@Service
public class ActiveTripLoader {

    private final MetroGraphService metroGraphService;

    public ActiveTripLoader(MetroGraphService metroGraphService) {
        this.metroGraphService = metroGraphService;
    }

    public List<ActiveTrip> loadActiveTrips(Set<String> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ActiveTrip> activeTrips = new ArrayList<>();
        for (TripDto trip : metroGraphService.getAllTrips()) {
            if (trip == null || trip.getTrip_id() == null || trip.getService_id() == null) {
                continue;
            }
            if (!serviceIds.contains(trip.getService_id())) {
                continue;
            }

            List<StopTimeDto> tripStopTimes = new ArrayList<>(metroGraphService.getTripStopTimes(trip.getTrip_id()));
            if (tripStopTimes.size() < 2) {
                continue;
            }
            tripStopTimes.sort(Comparator.comparingInt(StopTimeDto::getStop_sequence));
            ActiveTrip activeTrip = new ActiveTrip(
                    trip.getTrip_id(),
                    trip.getRoute_id(),
                    trip.getService_id(),
                    tripStopTimes
            );
            activeTrips.add(activeTrip);
        }

        return activeTrips;
    }
}
