package com.crm.backend.workflow;

import com.crm.backend.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
class WorkflowFoundationMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateWorkflowFoundationSchemaAndAccessRules() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                      'workflow_definitions',
                      'workflow_triggers',
                      'workflow_actions',
                      'workflow_executions',
                      'workflow_action_executions',
                      'workflow_action_attempts'
                  )
                """, Integer.class);
        assertEquals(6, tableCount);

        Integer revisionColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND (
                      (table_name = 'workflow_definitions'
                       AND column_name = 'definition_version')
                      OR (table_name = 'workflow_actions'
                          AND column_name IN (
                              'definition_version',
                              'retired_at'
                          ))
                  )
                """, Integer.class);
        assertEquals(3, revisionColumnCount);

        Integer permissionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM permissions
                WHERE name IN ('WORKFLOW_VIEW', 'WORKFLOW_MANAGE')
                """, Integer.class);
        assertEquals(2, permissionCount);

        Integer totalWorkflowGrants = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM role_permissions role_permission
                JOIN permissions permission
                  ON permission.id = role_permission.permission_id
                WHERE permission.name IN ('WORKFLOW_VIEW', 'WORKFLOW_MANAGE')
                """, Integer.class);
        assertEquals(5, totalWorkflowGrants);

        Integer expectedWorkflowGrants = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM role_permissions role_permission
                JOIN roles role ON role.id = role_permission.role_id
                JOIN permissions permission
                  ON permission.id = role_permission.permission_id
                WHERE (permission.name = 'WORKFLOW_VIEW'
                       AND role.name IN ('OWNER', 'ADMIN', 'MANAGER'))
                   OR (permission.name = 'WORKFLOW_MANAGE'
                       AND role.name IN ('OWNER', 'ADMIN'))
                """, Integer.class);
        assertEquals(5, expectedWorkflowGrants);

        Integer expectedPlanRules = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM subscription_plan_features plan_feature
                JOIN subscription_plans plan
                  ON plan.id = plan_feature.plan_id
                WHERE plan_feature.feature_key = 'WORKFLOW_AUTOMATION'
                  AND (
                      (plan.code = 'STARTER'
                       AND plan_feature.enabled = FALSE
                       AND plan_feature.limit_value IS NULL)
                      OR (plan.code = 'PROFESSIONAL'
                          AND plan_feature.enabled = TRUE
                          AND plan_feature.limit_value = 10)
                      OR (plan.code = 'BUSINESS'
                          AND plan_feature.enabled = TRUE
                          AND plan_feature.limit_value = 100)
                      OR (plan.code = 'ENTERPRISE'
                          AND plan_feature.enabled = TRUE
                          AND plan_feature.limit_value IS NULL)
                  )
                """, Integer.class);
        assertEquals(4, expectedPlanRules);
    }
}
