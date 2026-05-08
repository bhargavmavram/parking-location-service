package com.parking.location.repo;

import com.parking.location.domain.ParkingLocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingLocationRepository extends JpaRepository<ParkingLocation, Long> {
    List<ParkingLocation> findByZoneId(Long zoneId);
}
