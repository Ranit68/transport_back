package com.example.delhi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CalendarDateDto {

    private String service_id;
    private String date;
    private Integer exception_type;

    public String getService_id() {
        return service_id;
    }

    public void setService_id(String service_id) {
        this.service_id = service_id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(Object date) {
        if (date == null) {
            this.date = null;
            return;
        }
        String text = String.valueOf(date).trim();
        this.date = text.isBlank() ? null : text;
    }

    public Integer getException_type() {
        return exception_type;
    }

    public void setException_type(Integer exception_type) {
        this.exception_type = exception_type;
    }
}
