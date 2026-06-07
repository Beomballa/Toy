package com.section.admin.product.res;

import java.util.List;

public record ProductFrontDisplayRankGuideResponse(
        int guideLimit,
        Integer recommendedRank,
        List<Integer> occupiedRanks,
        List<Integer> availableRanks
) {
}
