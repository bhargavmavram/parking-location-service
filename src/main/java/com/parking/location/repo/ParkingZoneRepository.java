package com.parking.location.repo;

import com.parking.location.domain.ParkingZone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingZoneRepository extends JpaRepository<ParkingZone, Long> {
}
