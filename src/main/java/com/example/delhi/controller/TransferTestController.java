package com.example.delhi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.delhi.model.TransferPoint;
import com.example.delhi.service.TransferBuilderService;

@RestController
public class TransferTestController {

    private final TransferBuilderService transferBuilderService;

    public TransferTestController(
            TransferBuilderService transferBuilderService) {

        this.transferBuilderService = transferBuilderService;
    }

    @GetMapping("/api/transfers")
    public List<TransferPoint> getTransfers() {

        return transferBuilderService.buildTransfers();
    }
}
