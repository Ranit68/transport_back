package com.example.delhi.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.delhi.dto.StopDto;
import com.example.delhi.model.TransferPoint;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TransferBuilderService {

    private final MetroGraphService metroGraphService;
    private final BusGraphService busGraphService;

    public TransferBuilderService(
            MetroGraphService metroGraphService,
            BusGraphService busGraphService) {

        this.metroGraphService = metroGraphService;
        this.busGraphService = busGraphService;
    }

    public List<TransferPoint> buildTransfers() {

        List<StopDto> metroStations = new ArrayList<>(metroGraphService.getAllStops());

        List<StopDto> busStops = busGraphService.getStopIds()
                .stream()
                .map(busGraphService::getStop)
                .filter(stop -> stop != null)
                .filter(stop -> busGraphService.hasEdges(stop.getStop_id()))
                .toList();

        return buildTransfers(metroStations, busStops);
    }
    private static final double MAX_DISTANCE_METERS = 300;

    public List<TransferPoint> buildTransfers(
            List<StopDto> metroStations,
            List<StopDto> busStops) {
        List<TransferPoint> transfers = new ArrayList<>();

        log.info("Metro stations = {}", metroStations.size());
        log.info("Bus stops = {}", busStops.size());

        for (var it = metroStations.iterator(); it.hasNext();) {
            StopDto metro = it.next();
            for (StopDto bus : busStops) {
                double distance
                        = distanceMeters(
                                metro.getStop_lat(),
                                metro.getStop_lon(),
                                bus.getStop_lat(),
                                bus.getStop_lon());
                if (distance <= MAX_DISTANCE_METERS) {
                    transfers.add(
                            new TransferPoint(
                                    metro.getStop_id(),
                                    bus.getStop_id(),
                                    distance,
                                    metro.getStop_name(),
                                    bus.getStop_name(),
                                    4
                            ));
                }
            }

        }
        log.info("Transfers found = {}", transfers.size());
        log.info("Metro stations = {}", metroStations.size());
        log.info("Bus stops = {}", busStops.size());
        return transfers;

    }

    private double distanceMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {
        final double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a
                = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        return 2 * R * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a));
    }

}
