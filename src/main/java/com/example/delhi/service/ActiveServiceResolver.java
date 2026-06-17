package com.example.delhi.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.delhi.dto.CalendarDateDto;
import com.example.delhi.dto.CalendarDto;

@Service
public class ActiveServiceResolver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final SupabaseService supabaseService;

    public ActiveServiceResolver(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    public Set<String> resolveActiveServiceIds(LocalDate date) {
        Set<String> activeServiceIds = new LinkedHashSet<>();
        List<CalendarDto> calendars = supabaseService.fetchCalendar();

        for (CalendarDto calendar : calendars) {
            if (isServiceActiveOnDate(calendar, date)) {
                if (calendar.getService_id() != null) {
                    activeServiceIds.add(calendar.getService_id());
                }
            }
        }

        List<CalendarDateDto> exceptions = supabaseService.fetchCalendarDates(date);
        for (CalendarDateDto exception : exceptions) {
            if (exception.getService_id() == null || exception.getException_type() == null) {
                continue;
            }
            if (exception.getException_type() == 1) {
                activeServiceIds.add(exception.getService_id());
            } else if (exception.getException_type() == 2) {
                activeServiceIds.remove(exception.getService_id());
            }
        }

        return activeServiceIds;
    }

    private boolean isServiceActiveOnDate(CalendarDto calendar, LocalDate date) {
        if (calendar == null || calendar.getService_id() == null) {
            return false;
        }

        boolean dayActive = switch (date.getDayOfWeek()) {
            case MONDAY ->
                isFlagEnabled(calendar.getMonday());
            case TUESDAY ->
                isFlagEnabled(calendar.getTuesday());
            case WEDNESDAY ->
                isFlagEnabled(calendar.getWednesday());
            case THURSDAY ->
                isFlagEnabled(calendar.getThursday());
            case FRIDAY ->
                isFlagEnabled(calendar.getFriday());
            case SATURDAY ->
                isFlagEnabled(calendar.getSaturday());
            case SUNDAY ->
                isFlagEnabled(calendar.getSunday());
        };

        if (!dayActive) {
            return false;
        }

        LocalDate startDate = parseDate(calendar.getStart_date());
        LocalDate endDate = parseDate(calendar.getEnd_date());
        if (startDate == null || endDate == null) {
            return false;
        }

        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private boolean isFlagEnabled(Integer flag) {
        return flag != null && flag == 1;
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}
