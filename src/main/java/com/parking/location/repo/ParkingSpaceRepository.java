package com.parking.location.repo;

import com.parking.location.domain.ParkingSpace;
import com.parking.location.domain.SpaceStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {
    List<ParkingSpace> findByLocationId(Long locationId);
    long countByLocationIdAndStatus(Long locationId, SpaceStatus status);
}
