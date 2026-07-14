package com.crm.backend.search;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/search")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    public GlobalSearchController(
            GlobalSearchService globalSearchService
    ) {
        this.globalSearchService = globalSearchService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GlobalSearchResponse> search(
            @RequestParam(name = "q") String query,
            @RequestParam(
                    name = "limitPerModule",
                    defaultValue = "5"
            ) Integer limitPerModule,
            @RequestParam(
                    name = "modules",
                    required = false
            ) Set<SearchModule> modules
    ) {
        return ResponseEntity.ok(
                globalSearchService.search(
                        query,
                        limitPerModule,
                        modules
                )
        );
    }
}