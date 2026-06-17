package com.example.delhi.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.delhi.dto.StopDto;
import com.example.delhi.service.SupabaseService;

@RestController
@RequestMapping("/api")
public class MetroController {

    @Autowired
    private SupabaseService service;

    @GetMapping("/stations")
    public List<StopDto> stations() {
        return service.getStations();
    }
}
