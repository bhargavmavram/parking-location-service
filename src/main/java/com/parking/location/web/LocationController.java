package com.parking.location.web;

import com.parking.location.domain.*;
import com.parking.location.repo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
public class LocationController {
    private final ParkingZoneRepository zones;
    private final ParkingLocationRepository locations;
    private final ParkingSpaceRepository spaces;
    private final CameraDeviceRepository cameras;
    private final SecureRandom random = new SecureRandom();

    public LocationController(ParkingZoneRepository zones, ParkingLocationRepository locations, ParkingSpaceRepository spaces, CameraDeviceRepository cameras) {
        this.zones = zones;
        this.locations = locations;
        this.spaces = spaces;
        this.cameras = cameras;
    }

    @GetMapping("/status")
    public StatusResponse status() {
        return new StatusResponse("parking-location-service", "UP");
    }

    @GetMapping("/zones")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','PARKING_OWNER','SUPPORT')")
    public List<ParkingZone> zones() { return zones.findAll(); }

    @PostMapping("/zones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PARKING_OWNER')")
    public ParkingZone createZone(@Valid @RequestBody ZoneRequest request) {
        ParkingZone zone = new ParkingZone();
        zone.setCode(request.code());
        zone.setName(request.name());
        zone.setCity(request.city());
        zone.setDescription(request.description());
        zone.setActive(request.active());
        return zones.save(zone);
    }

    @GetMapping("/zones/{zoneId}/availability")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MANAGER','EMPLOYEE','PARKING_OWNER','SUPPORT')")
    public ZoneAvailabilityResponse zoneAvailability(@PathVariable Long zoneId) {
        zones.findById(zoneId).orElseThrow(() -> notFound("Zone not found"));
        List<ParkingLocation> zoneLocations = locations.findByZoneId(zoneId);
        long total = 0;
        long available = 0;
        for (ParkingLocation location : zoneLocations) {
            List<ParkingSpace> locationSpaces = spaces.findByLocationId(location.getId());
            total += locationSpaces.size();
            available += locationSpaces.stream().filter(space -> space.getStatus() == SpaceStatus.AVAILABLE).count();
        }
        return new ZoneAvailabilityResponse(zoneId, zoneLocations.size(), total, available);
    }

    @PostMapping("/parking-locations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PARKING_OWNER')")
    public ParkingLocation createLocation(@Valid @RequestBody LocationRequest request) {
        zones.findById(request.zoneId()).orElseThrow(() -> notFound("Zone not found"));
        ParkingLocation location = new ParkingLocation();
        location.setZoneId(request.zoneId());
        location.setName(request.name());
        location.setAddress(request.address());
        location.setType(request.type());
        location.setActive(request.active());
        return locations.save(location);
    }

    @GetMapping("/parking-locations")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MANAGER','EMPLOYEE','PARKING_OWNER','SUPPORT')")
    public List<ParkingLocation> parkingLocations(@RequestParam(required = false) Long zoneId) {
        return zoneId == null ? locations.findAll() : locations.findByZoneId(zoneId);
    }

    @PostMapping("/parking-locations/{locationId}/spaces")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PARKING_OWNER','EMPLOYEE')")
    public ParkingSpace createSpace(@PathVariable Long locationId, @Valid @RequestBody SpaceRequest request) {
        locations.findById(locationId).orElseThrow(() -> notFound("Location not found"));
        ParkingSpace space = new ParkingSpace();
        space.setLocationId(locationId);
        space.setCode(request.code());
        space.setStatus(request.status());
        space.setReservable(request.reservable());
        return spaces.save(space);
    }

    @GetMapping("/parking-locations/{locationId}/spaces")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MANAGER','EMPLOYEE','PARKING_OWNER','SUPPORT')")
    public List<ParkingSpace> spaces(@PathVariable Long locationId) {
        locations.findById(locationId).orElseThrow(() -> notFound("Location not found"));
        return spaces.findByLocationId(locationId);
    }

    @PatchMapping("/spaces/{spaceId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PARKING_OWNER','EMPLOYEE')")
    public ParkingSpace updateSpaceStatus(@PathVariable Long spaceId, @Valid @RequestBody SpaceStatusRequest request) {
        ParkingSpace space = spaces.findById(spaceId).orElseThrow(() -> notFound("Space not found"));
        space.setStatus(request.status());
        return spaces.save(space);
    }

    @GetMapping("/parking-locations/{locationId}/availability")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MANAGER','EMPLOYEE','PARKING_OWNER','SUPPORT')")
    public LocationAvailabilityResponse availability(@PathVariable Long locationId) {
        locations.findById(locationId).orElseThrow(() -> notFound("Location not found"));
        List<ParkingSpace> locationSpaces = spaces.findByLocationId(locationId);
        return new LocationAvailabilityResponse(locationId, locationSpaces.size(), spaces.countByLocationIdAndStatus(locationId, SpaceStatus.AVAILABLE));
    }

    @PostMapping("/parking-locations/{locationId}/cameras")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PARKING_OWNER')")
    public CameraDevice createCamera(@PathVariable Long locationId, @Valid @RequestBody CameraRequest request) {
        locations.findById(locationId).orElseThrow(() -> notFound("Location not found"));
        CameraDevice camera = new CameraDevice();
        camera.setLocationId(locationId);
        camera.setName(request.name());
        camera.setActive(request.active());
        return cameras.save(camera);
    }

    @PostMapping(value = "/cameras/{cameraId}/plate-events", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','PARKING_OWNER')")
    public PlateScanResponse scanPlate(@PathVariable Long cameraId, @RequestPart("image") MultipartFile image) {
        CameraDevice camera = cameras.findById(cameraId).orElseThrow(() -> notFound("Camera not found"));
        return new PlateScanResponse(camera.getId(), image.getOriginalFilename(), randomPlate(), "MOCK_CAMERA_SCAN");
    }

    private String randomPlate() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        return "" + letters.charAt(random.nextInt(26)) + letters.charAt(random.nextInt(26))
                + (10 + random.nextInt(90)) + letters.charAt(random.nextInt(26))
                + letters.charAt(random.nextInt(26)) + letters.charAt(random.nextInt(26));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    public record StatusResponse(String service, String status) {}
    public record ZoneRequest(@NotBlank String code, @NotBlank String name, @NotBlank String city, String description, boolean active) {}
    public record LocationRequest(@NotNull Long zoneId, @NotBlank String name, @NotBlank String address, @NotNull LocationType type, boolean active) {}
    public record SpaceRequest(@NotBlank String code, @NotNull SpaceStatus status, boolean reservable) {}
    public record SpaceStatusRequest(@NotNull SpaceStatus status) {}
    public record CameraRequest(@NotBlank String name, boolean active) {}
    public record LocationAvailabilityResponse(Long locationId, long totalSpaces, long availableSpaces) {}
    public record ZoneAvailabilityResponse(Long zoneId, long totalLocations, long totalSpaces, long availableSpaces) {}
    public record PlateScanResponse(Long cameraId, String uploadedFileName, String vehicleRegistrationNumber, String source) {}
}
