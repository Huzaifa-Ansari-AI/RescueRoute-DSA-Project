package com.huzaifa.rescueroute.repository;

import com.huzaifa.rescueroute.entity.ExternalFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalFacilityRepository extends JpaRepository<ExternalFacility, UUID>, JpaSpecificationExecutor<ExternalFacility> {

    Optional<ExternalFacility> findBySourceAndOsmElementTypeAndOsmElementId(String source, String osmElementType, Long osmElementId);

    List<ExternalFacility> findByImportStatus(String importStatus);

    List<ExternalFacility> findByFacilityType(String facilityType);

    List<ExternalFacility> findByFacilityTypeAndImportStatus(String facilityType, String importStatus);

    List<ExternalFacility> findByLatitudeBetweenAndLongitudeBetween(Double minLat, Double maxLat, Double minLon, Double maxLon);

    List<ExternalFacility> findByMatchedFacilityId(UUID matchedFacilityId);
}
