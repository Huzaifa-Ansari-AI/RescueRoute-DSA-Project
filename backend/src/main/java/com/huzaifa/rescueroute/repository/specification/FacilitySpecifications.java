package com.huzaifa.rescueroute.repository.specification;

import com.huzaifa.rescueroute.entity.Facility;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specifications for dynamic filtering of Facility entities.
 */
public class FacilitySpecifications {

    private FacilitySpecifications() {
        // Prevent instantiation of utility class
    }

    /**
     * Builds dynamic JPA specification based on optional filter parameters.
     *
     * @param type   Optional facility type filter (case-insensitive)
     * @param city   Optional city filter (case-insensitive)
     * @param active Optional active status filter
     * @return Specification matching the combined criteria
     */
    public static Specification<Facility> withFilters(String type, String city, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null && !type.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("facilityType")), type.trim().toLowerCase()));
            }

            if (city != null && !city.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase()));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("isActive"), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Builds dynamic JPA specification for administrator queries supporting both active/inactive
     * and verified/unverified filtering.
     *
     * @param type     Optional facility type filter (case-insensitive, ignores 'ALL')
     * @param city     Optional city filter (case-insensitive)
     * @param active   Optional active status filter (null matches both active and inactive)
     * @param verified Optional verification status filter (null matches both verified and unverified)
     * @return Specification matching the combined criteria
     */
    public static Specification<Facility> withAdminFilters(String type, String city, Boolean active, Boolean verified) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type.trim())) {
                predicates.add(cb.equal(cb.lower(root.get("facilityType")), type.trim().toLowerCase()));
            }

            if (city != null && !city.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase()));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("isActive"), active));
            }

            if (verified != null) {
                predicates.add(cb.equal(root.get("isVerified"), verified));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
