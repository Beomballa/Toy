package com.section.front.memberactivity.dto;

import java.util.List;
import java.util.Map;

public record FrontMemberActivityResponse(
        Map<String, List<FrontMemberActivityProductResponse>> activities,
        Map<String, Integer> limits
) {
}
