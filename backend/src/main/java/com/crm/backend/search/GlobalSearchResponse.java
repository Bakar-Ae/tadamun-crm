package com.crm.backend.search;

import java.util.List;

public record GlobalSearchResponse(
        String query,
        List<SearchResultResponse> results
) {
}