package com.parking.location.repo;

import com.parking.location.domain.CameraDevice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CameraDeviceRepository extends JpaRepository<CameraDevice, Long> {
    List<CameraDevice> findByLocationId(Long locationId);
}
