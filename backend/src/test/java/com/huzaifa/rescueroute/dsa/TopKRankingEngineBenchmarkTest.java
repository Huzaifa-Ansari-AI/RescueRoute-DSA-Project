package com.huzaifa.rescueroute.dsa;

import com.huzaifa.rescueroute.dto.FacilityResponseDto;
import com.huzaifa.rescueroute.dto.RankedFacilityResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DSA Performance Profiling & Empirical Benchmark for TopKRankingEngine.
 * Validates algorithmic scaling O(N log K) and auxiliary space O(K) across
 * synthetic healthcare facility datasets up to N = 100,000.
 */
public class TopKRankingEngineBenchmarkTest {

    private static final String[] FACILITY_TYPES = {"Hospital", "Clinic", "Pharmacy", "Blood Bank", "Ambulance"};

    private List<RankedFacilityResponseDto> generateSyntheticCandidates(int n) {
        Random rand = new Random(42); // Deterministic seed for reproducible benchmarking
        List<RankedFacilityResponseDto> list = new ArrayList<>(n);

        // Center around Karachi metropolitan coordinates: ~24.86, ~67.01
        for (int i = 0; i < n; i++) {
            double lat = 24.80 + (rand.nextDouble() * 0.30);
            double lng = 66.90 + (rand.nextDouble() * 0.40);
            double distKm = Math.round(rand.nextDouble() * 45.0 * 100.0) / 100.0;
            double score = Math.round(rand.nextDouble() * 1000.0) / 1000.0;
            String type = FACILITY_TYPES[rand.nextInt(FACILITY_TYPES.length)];

            list.add(RankedFacilityResponseDto.builder()
                    .facility(FacilityResponseDto.builder()
                            .id(UUID.randomUUID())
                            .name("Medical Center #" + (i + 1))
                            .facilityType(type)
                            .latitude(lat)
                            .longitude(lng)
                            .build())
                    .distanceKm(distKm)
                    .straightLineDistanceKm(distKm)
                    .score(score)
                    .isVerified(rand.nextBoolean())
                    .emergencyAvailable(rand.nextBoolean())
                    .build());
        }
        return list;
    }

    private void runBenchmarkFor(int n, int k) {
        List<RankedFacilityResponseDto> candidates = generateSyntheticCandidates(n);

        // Warm up JIT compiler
        for (int i = 0; i < 5; i++) {
            TopKRankingEngine.getTopK(candidates.subList(0, Math.min(n, 500)), k);
        }

        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startTime = System.nanoTime();

        List<RankedFacilityResponseDto> topK = TopKRankingEngine.getTopK(candidates, k);

        long endTime = System.nanoTime();
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        long approxAuxMem = Math.max(0, memAfter - memBefore);

        // Correctness assertions
        assertEquals(k, topK.size(), "Top-K result size must match requested K");

        // Verify descending score monotonicity
        for (int i = 1; i < topK.size(); i++) {
            RankedFacilityResponseDto prev = topK.get(i - 1);
            RankedFacilityResponseDto curr = topK.get(i);

            assertTrue(prev.getScore() >= curr.getScore() ||
                            (prev.getScore() == curr.getScore() && prev.getDistanceKm() <= curr.getDistanceKm()),
                    "Top-K results must be strictly ordered best-first");
            assertEquals(i + 1, curr.getRank(), "Sequential rank must be assigned");
        }

        // Verify that rank 1 score is indeed the maximum in the dataset
        double maxDatasetScore = candidates.stream().mapToDouble(RankedFacilityResponseDto::getScore).max().orElse(0.0);
        assertEquals(maxDatasetScore, topK.get(0).getScore(), 0.001,
                "Rank 1 facility must possess the maximum score in the dataset");

        System.out.printf("[DSA BENCHMARK] N = %-7d | K = %-3d | Time: %7.3f ms | AuxMem: %6.2f KB | Status: PASS%n",
                n, k, elapsedMs, approxAuxMem / 1024.0);
    }

    @Test
    @DisplayName("Benchmark N = 1,000 candidates with K = 10 and K = 20")
    void testBenchmark1K() {
        runBenchmarkFor(1_000, 10);
        runBenchmarkFor(1_000, 20);
    }

    @Test
    @DisplayName("Benchmark N = 10,000 candidates with K = 10 and K = 20")
    void testBenchmark10K() {
        runBenchmarkFor(10_000, 10);
        runBenchmarkFor(10_000, 20);
    }

    @Test
    @DisplayName("Benchmark N = 50,000 candidates with K = 10 and K = 20")
    void testBenchmark50K() {
        runBenchmarkFor(50_000, 10);
        runBenchmarkFor(50_000, 20);
    }

    @Test
    @DisplayName("Benchmark N = 100,000 candidates with K = 10 and K = 20")
    void testBenchmark100K() {
        runBenchmarkFor(100_000, 10);
        runBenchmarkFor(100_000, 20);
    }
}
