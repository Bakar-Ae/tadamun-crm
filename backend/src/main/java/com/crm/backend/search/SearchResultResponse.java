package com.crm.backend.search;

public record SearchResultResponse(
        SearchModule module,
        Long id,
        String title,
        String description,
        String status,
        SearchModule parentModule,
        Long parentId
) {
}