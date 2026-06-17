package com.example.delhi.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.delhi.dto.ActiveTrip;
import com.example.delhi.dto.LiveTrainPosition;
import com.example.delhi.dto.StopDto;
import com.example.delhi.dto.StopTimeDto;

@Service
public class LiveTrainService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm:ss");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Kolkata");
    private static final long DAY_SECONDS = 86400L;
    private static final long EARLY_MORNING_THRESHOLD = 6 * 3600L;

    private final ActiveServiceResolver activeServiceResolver;
    private final ActiveTripLoader activeTripLoader;
    private final MetroGraphService metroGraphService;

    private final Map<String, LiveTrainPosition> livePositions = new ConcurrentHashMap<>();

    public LiveTrainService(
            ActiveServiceResolver activeServiceResolver,
            ActiveTripLoader activeTripLoader,
            MetroGraphService metroGraphService
    ) {
        this.activeServiceResolver = activeServiceResolver;
        this.activeTripLoader = activeTripLoader;
        this.metroGraphService = metroGraphService;
    }

    @Scheduled(initialDelay = 0, fixedRate = 5000)
    public void refreshLivePositions() {
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalTime now = LocalTime.now(ZONE_ID);
        List<LiveTrainPosition> positions = calculateLivePositions(today, now);

        Map<String, LiveTrainPosition> updatedPositions = new ConcurrentHashMap<>();
        for (LiveTrainPosition position : positions) {
            updatedPositions.put(position.getTripId(), position);
        }

        livePositions.clear();
        livePositions.putAll(updatedPositions);
    }

    public List<LiveTrainPosition> getLivePositionsAt(LocalDate today, LocalTime now) {
        return calculateLivePositions(today, now);
    }

    private List<LiveTrainPosition> calculateLivePositions(LocalDate today, LocalTime now) {
        Set<String> activeServiceIds = resolveLiveServiceIds(today, now);

        List<ActiveTrip> activeTrips = activeTripLoader.loadActiveTrips(activeServiceIds);
        List<LiveTrainPosition> positions = new ArrayList<>();

        for (ActiveTrip trip : activeTrips) {
            if (trip == null || !trip.isValid()) {
                continue;
            }
            LiveTrainPosition position = buildPosition(trip, today, now);
            if (position != null) {
                positions.add(position);
            }
        }

        return positions;
    }

    public List<LiveTrainPosition> getLivePositions() {
        return new ArrayList<>(livePositions.values());
    }

    public LiveTrainPosition getLivePosition(String tripId) {
        return livePositions.get(tripId);
    }

    private Set<String> resolveLiveServiceIds(LocalDate today, LocalTime now) {
        Set<String> serviceIds = ConcurrentHashMap.newKeySet();
        serviceIds.addAll(activeServiceResolver.resolveActiveServiceIds(today));
        if (now.toSecondOfDay() < EARLY_MORNING_THRESHOLD) {
            serviceIds.addAll(activeServiceResolver.resolveActiveServiceIds(today.minusDays(1)));
        }
        return serviceIds;
    }

    private LiveTrainPosition buildPosition(ActiveTrip trip, LocalDate today, LocalTime now) {
        List<StopTimeDto> stopTimes = trip.getStopTimes();
        long nowSecondsOfDay = now.toSecondOfDay();

        for (int index = 0; index < stopTimes.size() - 1; index++) {
            StopTimeDto currentStopTime = stopTimes.get(index);
            StopTimeDto nextStopTime = stopTimes.get(index + 1);

            long currentArrival = parseTime(currentStopTime.getArrival_time());
            long currentDeparture = parseTime(currentStopTime.getDeparture_time());
            long nextArrival = parseTime(nextStopTime.getArrival_time());

            if (currentDeparture < 0 || nextArrival < 0) {
                continue;
            }

            nextArrival = normalizeFollowingTime(currentDeparture, nextArrival);
            currentArrival = normalizeCurrentArrival(currentArrival, currentDeparture);

            long normalizedNow = normalizeNowSeconds(nowSecondsOfDay, currentDeparture, nextArrival);
            boolean atStation = currentArrival >= 0
                    && normalizedNow >= currentArrival
                    && normalizedNow < currentDeparture;
            boolean enRoute = normalizedNow >= currentDeparture
                    && normalizedNow < nextArrival;

            if (!atStation && !enRoute) {
                continue;
            }

            return createPosition(
                    trip,
                    currentStopTime,
                    nextStopTime,
                    today,
                    currentArrival,
                    currentDeparture,
                    nextArrival,
                    normalizedNow,
                    atStation
            );
        }

        return null;
    }

    private LiveTrainPosition createPosition(
            ActiveTrip trip,
            StopTimeDto currentStopTime,
            StopTimeDto nextStopTime,
            LocalDate today,
            long currentArrival,
            long currentDeparture,
            long nextArrival,
            long normalizedNow,
            boolean atStation
    ) {
        String currentStopId = currentStopTime.getStop_id();
        String nextStopId = nextStopTime.getStop_id();

        StopDto currentStop = metroGraphService.getStop(currentStopId);
        StopDto nextStop = metroGraphService.getStop(nextStopId);

        if (currentStop == null || nextStop == null) {
            return null;
        }

        if (currentStop.getStop_lat() == null || currentStop.getStop_lon() == null
                || nextStop.getStop_lat() == null || nextStop.getStop_lon() == null) {
            return null;
        }

        double progress = atStation
                ? 0.0
                : Math.min(1.0, Math.max(0.0, (normalizedNow - currentDeparture) / (double) (nextArrival - currentDeparture)));

        double currentLat = currentStop.getStop_lat() + (nextStop.getStop_lat() - currentStop.getStop_lat()) * progress;
        double currentLon = currentStop.getStop_lon() + (nextStop.getStop_lon() - currentStop.getStop_lon()) * progress;
        double bearing = calculateBearing(currentStop.getStop_lat(), currentStop.getStop_lon(), nextStop.getStop_lat(), nextStop.getStop_lon());
        LocalDate serviceDate = normalizedNow >= DAY_SECONDS ? today.minusDays(1) : today;

        LiveTrainPosition position = new LiveTrainPosition();
        position.setTripId(trip.getTripId());
        position.setRouteId(trip.getRouteId());
        position.setRouteName(resolveRouteName(trip.getRouteId()));
        position.setCurrentStopId(currentStopId);
        position.setCurrentStopName(currentStop.getStop_name());
        position.setCurrentStopSequence(currentStopTime.getStop_sequence());
        position.setNextStopId(nextStopId);
        position.setNextStopName(nextStop.getStop_name());
        position.setNextStopSequence(nextStopTime.getStop_sequence());
        position.setCurrentStopArrivalTimestamp(convertSecondsToEpochMs(serviceDate, currentArrival));
        position.setCurrentStopDepartureTimestamp(convertSecondsToEpochMs(serviceDate, currentDeparture));
        position.setNextStopArrivalTimestamp(convertSecondsToEpochMs(serviceDate, nextArrival));
        position.setNextStopDepartureTimestamp(convertSecondsToEpochMs(serviceDate, normalizeFollowingTime(nextArrival, parseTime(nextStopTime.getDeparture_time()))));
        position.setStatus(atStation ? "At station" : "En route");
        position.setProgress(progress);
        position.setCurrentLat(currentLat);
        position.setCurrentLon(currentLon);
        position.setBearing(bearing);
        position.setLastUpdatedEpochMs(System.currentTimeMillis());

        return position;
    }

    private long normalizeFollowingTime(long baseSeconds, long followingSeconds) {
        if (baseSeconds < 0 || followingSeconds < 0) {
            return followingSeconds;
        }
        return followingSeconds < baseSeconds ? followingSeconds + DAY_SECONDS : followingSeconds;
    }

    private long normalizeCurrentArrival(long arrivalSeconds, long departureSeconds) {
        if (arrivalSeconds < 0 || departureSeconds < 0) {
            return arrivalSeconds;
        }
        return arrivalSeconds > departureSeconds ? arrivalSeconds - DAY_SECONDS : arrivalSeconds;
    }

    private long normalizeNowSeconds(long nowSeconds, long departureSeconds, long arrivalSeconds) {
        if (nowSeconds < EARLY_MORNING_THRESHOLD && (departureSeconds >= DAY_SECONDS || arrivalSeconds >= DAY_SECONDS)) {
            return nowSeconds + DAY_SECONDS;
        }
        return nowSeconds;
    }

    private long parseTime(String timeText) {
        if (timeText == null || timeText.isBlank()) {
            return -1;
        }
        try {
            if (timeText.startsWith("24:") || timeText.startsWith("25:") || timeText.startsWith("26:") || timeText.startsWith("27:") || timeText.startsWith("28:") || timeText.startsWith("29:")) {
                String[] parts = timeText.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds).toSeconds();
            }
            LocalTime time = LocalTime.parse(timeText, TIME_FORMATTER);
            return time.toSecondOfDay();
        } catch (Exception e) {
            return -1;
        }
    }

    private long convertSecondsToEpochMs(LocalDate serviceDate, long secondsOfDay) {
        if (secondsOfDay < 0) {
            return -1;
        }
        long days = secondsOfDay / DAY_SECONDS;
        int secondOfDay = (int) (secondsOfDay % DAY_SECONDS);
        return serviceDate
                .plusDays(days)
                .atTime(LocalTime.ofSecondOfDay(secondOfDay))
                .atZone(ZONE_ID)
                .toInstant()
                .toEpochMilli();
    }

    private String resolveRouteName(String routeId) {
        if (routeId == null) {
            return "Unknown Route";
        }

        var route = metroGraphService.getRoute(routeId);
        if (route != null && route.getRoute_long_name() != null && !route.getRoute_long_name().isBlank()) {
            return route.getRoute_long_name();
        }
        return routeId;
    }

    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);
        double theta = Math.atan2(y, x);
        return (Math.toDegrees(theta) + 360) % 360;
    }
}
