package com.huzaifa.rescueroute.dsa;

import com.huzaifa.rescueroute.dto.FacilityResponseDto;
import com.huzaifa.rescueroute.dto.RankedFacilityResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TopKRankingEngineTest {

    private RankedFacilityResponseDto createCandidate(String name, double score, double distanceKm, UUID id) {
        return RankedFacilityResponseDto.builder()
                .facility(FacilityResponseDto.builder()
                        .id(id != null ? id : UUID.randomUUID())
                        .name(name)
                        .facilityType("Hospital")
                        .build())
                .score(score)
                .distanceKm(distanceKm)
                .build();
    }

    @Test
    @DisplayName("Empty or null candidate list returns empty list")
    void testEmptyAndNullCandidates() {
        assertTrue(TopKRankingEngine.getTopK(null, 5).isEmpty());
        assertTrue(TopKRankingEngine.getTopK(new ArrayList<>(), 5).isEmpty());
        assertTrue(TopKRankingEngine.getTopK(List.of(createCandidate("A", 0.8, 5.0, null)), 0).isEmpty());
        assertTrue(TopKRankingEngine.getTopK(List.of(createCandidate("A", 0.8, 5.0, null)), -1).isEmpty());
    }

    @Test
    @DisplayName("Top-K selects highest scores with O(N log K) Min-Heap")
    void testTopKSelection() {
        List<RankedFacilityResponseDto> candidates = List.of(
                createCandidate("Low 1", 0.20, 15.0, null),
                createCandidate("High 1", 0.95, 2.0, null),
                createCandidate("Medium 1", 0.60, 8.0, null),
                createCandidate("High 2", 0.88, 3.5, null),
                createCandidate("Low 2", 0.15, 20.0, null),
                createCandidate("High 3", 0.91, 1.2, null)
        );

        // Request top 3
        List<RankedFacilityResponseDto> top3 = TopKRankingEngine.getTopK(candidates, 3);

        assertEquals(3, top3.size());
        assertEquals("High 1", top3.get(0).getFacility().getName());
        assertEquals(1, top3.get(0).getRank());
        assertEquals("High 3", top3.get(1).getFacility().getName());
        assertEquals(2, top3.get(1).getRank());
        assertEquals("High 2", top3.get(2).getFacility().getName());
        assertEquals(3, top3.get(2).getRank());
    }

    @Test
    @DisplayName("Distance tie-breaking when scores are equal")
    void testDistanceTieBreaking() {
        List<RankedFacilityResponseDto> candidates = List.of(
                createCandidate("Far", 0.85, 12.0, null),
                createCandidate("Near", 0.85, 3.0, null),
                createCandidate("Mid", 0.85, 7.0, null)
        );

        List<RankedFacilityResponseDto> top2 = TopKRankingEngine.getTopK(candidates, 2);

        assertEquals(2, top2.size());
        assertEquals("Near", top2.get(0).getFacility().getName());
        assertEquals("Mid", top2.get(1).getFacility().getName());
    }

    @Test
    @DisplayName("Deterministic tie-breaking when both score and distance are identical")
    void testDeterministicIdTieBreaking() {
        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        List<RankedFacilityResponseDto> candidates = List.of(
                createCandidate("Second", 0.90, 5.0, id2),
                createCandidate("First", 0.90, 5.0, id1)
        );

        List<RankedFacilityResponseDto> result = TopKRankingEngine.getTopK(candidates, 2);

        assertEquals(2, result.size());
        assertEquals(id1, result.get(0).getFacility().getId());
        assertEquals(id2, result.get(1).getFacility().getId());
    }

    @Test
    @DisplayName("When K is greater than N, returns all N sorted")
    void testKGreaterThanN() {
        List<RankedFacilityResponseDto> candidates = List.of(
                createCandidate("B", 0.50, 10.0, null),
                createCandidate("A", 0.80, 5.0, null)
        );

        List<RankedFacilityResponseDto> result = TopKRankingEngine.getTopK(candidates, 10);

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getFacility().getName());
        assertEquals("B", result.get(1).getFacility().getName());
    }
}
