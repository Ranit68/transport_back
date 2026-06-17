package com.example.delhi.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.delhi.dto.CalendarDateDto;
import com.example.delhi.dto.CalendarDto;
import com.example.delhi.dto.RouteDto;
import com.example.delhi.dto.StopDto;
import com.example.delhi.dto.StopTimeDto;
import com.example.delhi.dto.TripDto;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class BusSupabaseService {

    private static final String SUPABASE_URL
            = "https://pxjxzbbhrnozxmkxiyht.supabase.co";

    private static final String API_KEY
            = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB4anh6YmJocm5venhta3hpeWh0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODAyMDI5NTUsImV4cCI6MjA5NTc3ODk1NX0.gUX5enAnMKtvgU2HuunfoEooJFlGS9SP61_Klq0NGss";

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.setBearerAuth(API_KEY);
        return headers;
    }

    private <T> List<T> fetchList(String url, TypeReference<List<T>> type) {
        HttpEntity<String> entity = new HttpEntity<>(buildHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );
        try {
            return objectMapper.readValue(response.getBody(), type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Supabase response", e);
        }
    }

    public List<StopDto> fetchStops() {

        List<StopDto> allStops
                = new ArrayList<>();

        int offset = 0;
        int limit = 1000;

        while (true) {

            String url
                    = SUPABASE_URL
                    + "/rest/v1/bus_stops"
                    + "?select=stop_id,stop_name,stop_lat,stop_lon"
                    + "&limit=" + limit
                    + "&offset=" + offset;

            List<StopDto> batch
                    = fetchList(
                            url,
                            new TypeReference<List<StopDto>>() {
                    });

            if (batch == null || batch.isEmpty()) {
                break;
            }

            allStops.addAll(batch);

            System.out.println(
                    "Loaded Stops = "
                    + allStops.size());

            if (batch.size() < limit) {
                break;
            }

            offset += limit;
        }

        return allStops;
    }

    public List<RouteDto> fetchRoutes() {
        String url = SUPABASE_URL + "/rest/v1/bus_routes?select=route_id,route_long_name,route_short_name";
        return fetchList(url, new TypeReference<>() {
        });
    }

    public List<TripDto> fetchTrips(int offset, int limit) {
        String url = SUPABASE_URL + "/rest/v1/bus_trips?select=trip_id,route_id,service_id"
                + "&limit=" + limit
                + "&offset=" + offset;
        return fetchList(url, new TypeReference<>() {
        });
    }

    public List<StopTimeDto> fetchStopTimes(int offset, int limit) {
        String url = SUPABASE_URL + "/rest/v1/bus_stop_times?select=trip_id,stop_id,stop_sequence,arrival_time,departure_time"
                + "&limit=" + limit
                + "&offset=" + offset;
        return fetchList(url, new TypeReference<>() {
        });
    }

    public List<CalendarDto> fetchCalendar() {
        String url = SUPABASE_URL + "/rest/v1/bus_calendar?select=service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date";
        return fetchList(url, new TypeReference<>() {
        });
    }

    public List<CalendarDateDto> fetchCalendarDates(LocalDate date) {
        String formattedDate = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        String url = SUPABASE_URL + "/rest/v1/bus_calendar_dates?select=service_id,date,exception_type"
                + "&date=eq." + formattedDate;
        try {
            return fetchList(url, new TypeReference<>() {
            });
        } catch (HttpClientErrorException.NotFound e) {
            return Collections.emptyList();
        }
    }

    public List<TripDto> fetchTripsByServiceIds(List<String> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        String inClause = serviceIds.stream()
                .map(id -> "\"" + id.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(","));
        String filterValue = "in.(" + inClause + ")";
        String url = SUPABASE_URL + "/rest/v1/bus_trips?select=trip_id,route_id,service_id"
                + "&service_id=" + filterValue;
        return fetchList(url, new TypeReference<>() {
        });
    }

    public List<StopTimeDto> fetchStopTimesByTripIds(List<String> tripIds) {
        if (tripIds == null || tripIds.isEmpty()) {
            return Collections.emptyList();
        }
        String inClause = tripIds.stream()
                .map(id -> "\"" + id.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(","));
        String filterValue = "in.(" + inClause + ")";
        String url = SUPABASE_URL + "/rest/v1/bus_stop_times?select=trip_id,stop_id,stop_sequence,arrival_time,departure_time"
                + "&trip_id=" + filterValue;
        return fetchList(url, new TypeReference<>() {
        });
    }

    public List<StopDto> getStations() {
        return fetchStops();
    }

}
