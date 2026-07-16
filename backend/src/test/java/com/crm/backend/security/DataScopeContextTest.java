package com.crm.backend.security;

import com.crm.backend.role.DataScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataScopeContextTest {

    @Test
    void allScopeShouldAllowEveryRecord() {
        DataScopeContext context = new DataScopeContext(1L, null, DataScope.ALL);

        assertTrue(context.canAccess(99L, 88L));
        assertTrue(context.canAccess(null, null));
    }

    @Test
    void ownScopeShouldOnlyAllowRecordsOwnedByCurrentUser() {
        DataScopeContext context = new DataScopeContext(1L, 10L, DataScope.OWN);

        assertTrue(context.canAccess(1L, 20L));
        assertFalse(context.canAccess(2L, 10L));
    }

    @Test
    void teamScopeShouldAllowRecordsOwnedBySameTeam() {
        DataScopeContext context = new DataScopeContext(1L, 10L, DataScope.TEAM);

        assertTrue(context.canAccess(2L, 10L));
        assertFalse(context.canAccess(2L, 20L));
    }

    @Test
    void teamScopeWithoutTeamShouldFallBackToOwnRecords() {
        DataScopeContext context = new DataScopeContext(1L, null, DataScope.TEAM);

        assertTrue(context.canAccess(1L, null));
        assertFalse(context.canAccess(2L, null));
    }

    @Test
    void nonAllScopeShouldRejectUnownedRecords() {
        DataScopeContext context = new DataScopeContext(1L, 10L, DataScope.TEAM);

        assertFalse(context.canAccess(null, 10L));
    }
}
