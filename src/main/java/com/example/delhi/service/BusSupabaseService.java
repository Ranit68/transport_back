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
import org.springframework.beans.factory.annotation.Value;
import com.example.delhi.dto.StopDto;
import com.example.delhi.dto.StopTimeDto;
import com.example.delhi.dto.TripDto;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class BusSupabaseService {

   @Value("${supabase.url}")
private String SUPABASE_URL;

@Value("${supabase.key}")
private String API_KEY;
    
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
        List<RouteDto> allRoutes = new ArrayList<>();
        int offset = 0;
        int limit = 1000;

        while (true) {
            String url = SUPABASE_URL
                    + "/rest/v1/bus_routes?select=route_id,route_long_name,route_short_name"
                    + "&limit=" + limit
                    + "&offset=" + offset;

            List<RouteDto> batch = fetchList(url, new TypeReference<>() {
            });

            if (batch == null || batch.isEmpty()) {
                break;
            }

            allRoutes.addAll(batch);

            if (batch.size() < limit) {
                break;
            }

            offset += limit;
        }

        return allRoutes;
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
                + "&service_id=" + java.net.URLEncoder.encode(filterValue, java.nio.charset.StandardCharsets.UTF_8);
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
                + "&trip_id=" + java.net.URLEncoder.encode(filterValue, java.nio.charset.StandardCharsets.UTF_8);
        return fetchList(url, new TypeReference<>() {
        });
    }

    public List<StopDto> getStations() {
        return fetchStops();
    }

}
