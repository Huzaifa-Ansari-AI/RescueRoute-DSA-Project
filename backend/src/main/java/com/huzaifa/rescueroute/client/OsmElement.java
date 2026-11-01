package com.huzaifa.rescueroute.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Normalized representation of an OpenStreetMap element parsed from Overpass API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OsmElement {

    private String elementType; // 'node', 'way', 'relation'
    private Long elementId;
    private String name;
    private String facilityType; // 'Hospital', 'Clinic', 'Pharmacy', 'Blood Bank', 'Ambulance'
    private Double latitude;
    private Double longitude;
    private String address;
    private String city;
    private String phoneNumber;
    private String website;
    private String openingHours;
    private String sourceUrl;
    private String rawTagsJson;
}
