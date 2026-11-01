package com.huzaifa.rescueroute.dsa;

import com.huzaifa.rescueroute.dto.RankedFacilityResponseDto;

import java.util.*;

/**
 * Data Structures & Algorithms (DSA) Engine for Top-K Priority Ranking.
 *
 * <p><strong>Algorithm Explanation & Design:</strong>
 * To efficiently retrieve the top K ranked facilities from N candidates without sorting the
 * entire dataset in O(N log N) time, this engine uses a bounded <strong>Min-Heap</strong>
 * (implemented via Java's {@link PriorityQueue}).
 *
 * <p><strong>Heap Ordering Strategy:</strong>
 * The heap comparator orders elements such that the <em>least desirable</em> candidate among the
 * current top-K candidates sits at the head (root) of the Min-Heap.
 *
 * <p><strong>Time & Space Complexity:</strong>
 * <ul>
 *   <li><strong>Time Complexity:</strong> O(N log K) — Each of the N candidates is evaluated.
 *       Inserting or evicting in a heap of size K takes O(log K) operations.
 *       For large datasets where K &lt;&lt; N (e.g. K=10, N=10,000), O(N log K) is asymptotically
 *       superior to full sorting O(N log N).</li>
 *   <li><strong>Space Complexity:</strong> O(K) auxiliary space — The heap never stores more than
 *       K elements at any point in memory.</li>
 * </ul>
 */
public final class TopKRankingEngine {

    private TopKRankingEngine() {
        // Prevent instantiation of algorithmic engine
    }

    /**
     * Comparator defining "natural order" for the Min-Heap.
     * The item at the head of this heap is the "weakest" candidate currently in the top-K.
     *
     * <p>Comparison logic:
     * 1. Lower score is considered "smaller" (sits at head).
     * 2. If scores are equal, larger distance is considered "smaller" (sits at head).
     * 3. If scores and distances are equal, deterministic tie-breaking by reversed UUID.
     */
    private static final Comparator<RankedFacilityResponseDto> MIN_HEAP_COMPARATOR = (a, b) -> {
        // 1. Primary: Score ascending (lower score = weaker)
        int scoreCompare = Double.compare(a.getScore(), b.getScore());
        if (scoreCompare != 0) {
            return scoreCompare;
        }

        // 2. Secondary: Distance descending (larger distance = weaker)
        int distCompare = Double.compare(b.getDistanceKm(), a.getDistanceKm());
        if (distCompare != 0) {
            return distCompare;
        }

        // 3. Tertiary: Deterministic tie-breaker using Facility ID
        String idA = a.getFacility() != null && a.getFacility().getId() != null
                ? a.getFacility().getId().toString() : "";
        String idB = b.getFacility() != null && b.getFacility().getId() != null
                ? b.getFacility().getId().toString() : "";

        // Reversed string compare so the lexicographically larger ID sits at head as weaker
        return idB.compareTo(idA);
    };

    /**
     * Comparator for final result sorting: Highest score first, lowest distance first.
     */
    public static final Comparator<RankedFacilityResponseDto> BEST_FIRST_COMPARATOR = (a, b) -> {
        // 1. Score descending (higher score is better)
        int scoreCompare = Double.compare(b.getScore(), a.getScore());
        if (scoreCompare != 0) {
            return scoreCompare;
        }

        // 2. Distance ascending (smaller distance is better)
        int distCompare = Double.compare(a.getDistanceKm(), b.getDistanceKm());
        if (distCompare != 0) {
            return distCompare;
        }

        // 3. Facility ID ascending
        String idA = a.getFacility() != null && a.getFacility().getId() != null
                ? a.getFacility().getId().toString() : "";
        String idB = b.getFacility() != null && b.getFacility().getId() != null
                ? b.getFacility().getId().toString() : "";

        return idA.compareTo(idB);
    };

    /**
     * Comparator for Road-network best-first sorting:
     * 1. Higher score is better (descending).
     * 1. Lower driving travel time (estimatedTravelMinutes) is better (ascending, nulls last).
     * 2. Lower driving distance (drivingDistanceKm) is better (ascending, nulls last).
     * 3. Higher score is better (descending).
     * 4. Lower straight-line distance (distanceKm) is better (ascending).
     * 5. Deterministic tie-breaker using Facility ID ascending.
     */
    public static final Comparator<RankedFacilityResponseDto> ROAD_BEST_FIRST_COMPARATOR = (a, b) -> {
        // 1. Driving travel time ascending (nulls last) - lowest ETA gets top rank
        Integer durA = a.getEstimatedTravelMinutes();
        Integer durB = b.getEstimatedTravelMinutes();
        if (durA != null || durB != null) {
            if (durA == null) return 1;
            if (durB == null) return -1;
            int durCompare = Integer.compare(durA, durB);
            if (durCompare != 0) {
                return durCompare;
            }
        }

        // 2. Driving distance ascending (nulls last)
        Double driveA = a.getDrivingDistanceKm();
        Double driveB = b.getDrivingDistanceKm();
        if (driveA != null || driveB != null) {
            if (driveA == null) return 1;
            if (driveB == null) return -1;
            int driveCompare = Double.compare(driveA, driveB);
            if (driveCompare != 0) {
                return driveCompare;
            }
        }

        // 3. Score descending (if travel times and driving distances are identical)
        int scoreCompare = Double.compare(b.getScore(), a.getScore());
        if (scoreCompare != 0) {
            return scoreCompare;
        }

        // 4. Straight-line distance ascending
        int distCompare = Double.compare(a.getDistanceKm(), b.getDistanceKm());
        if (distCompare != 0) {
            return distCompare;
        }

        // 5. Facility ID ascending
        String idA = a.getFacility() != null && a.getFacility().getId() != null
                ? a.getFacility().getId().toString() : "";
        String idB = b.getFacility() != null && b.getFacility().getId() != null
                ? b.getFacility().getId().toString() : "";

        return idA.compareTo(idB);
    };

    /**
     * Orders road-network evaluated candidates best-first, truncates to top K,
     * and assigns sequential 1-based ranks.
     *
     * @param candidates Candidates with road metrics evaluated
     * @param k          Maximum number of results to return
     * @return Ordered and ranked list of top K facilities
     */
    public static List<RankedFacilityResponseDto> rankRoadCandidates(List<RankedFacilityResponseDto> candidates, int k) {
        if (candidates == null || candidates.isEmpty() || k <= 0) {
            return Collections.emptyList();
        }

        List<RankedFacilityResponseDto> sorted = new ArrayList<>(candidates);
        sorted.sort(ROAD_BEST_FIRST_COMPARATOR);

        int targetK = Math.min(k, sorted.size());
        List<RankedFacilityResponseDto> topK = new ArrayList<>(sorted.subList(0, targetK));

        for (int i = 0; i < topK.size(); i++) {
            topK.get(i).setRank(i + 1);
        }

        return topK;
    }

    /**
     * Selects and orders the top K facilities from a candidate list using a bounded Min-Heap.
     *
     * @param candidates Full list of scored facility candidates (size N)
     * @param k          Maximum number of top results desired (K &gt; 0)
     * @return Ordered list of top K facilities, with rank assigned from 1 to K
     */
    public static List<RankedFacilityResponseDto> getTopK(List<RankedFacilityResponseDto> candidates, int k) {
        if (candidates == null || candidates.isEmpty() || k <= 0) {
            return Collections.emptyList();
        }

        int targetK = Math.min(k, candidates.size());

        // Min-heap initialized with capacity targetK + 1
        PriorityQueue<RankedFacilityResponseDto> minHeap =
                new PriorityQueue<>(targetK + 1, MIN_HEAP_COMPARATOR);

        // Process all N candidates: O(N log K)
        for (RankedFacilityResponseDto candidate : candidates) {
            if (minHeap.size() < targetK) {
                minHeap.offer(candidate);
            } else {
                // If the candidate is strictly better than the worst in our top-K heap
                if (MIN_HEAP_COMPARATOR.compare(candidate, minHeap.peek()) > 0) {
                    minHeap.poll(); // Evict weakest
                    minHeap.offer(candidate); // Insert better candidate
                }
            }
        }

        // Drain the min-heap and sort best-first: O(K log K)
        List<RankedFacilityResponseDto> topKList = new ArrayList<>(minHeap.size());
        while (!minHeap.isEmpty()) {
            topKList.add(minHeap.poll());
        }

        topKList.sort(BEST_FIRST_COMPARATOR);

        // Assign 1-based sequential ranks (1, 2, ... K)
        for (int i = 0; i < topKList.size(); i++) {
            topKList.get(i).setRank(i + 1);
        }

        return topKList;
    }
}
