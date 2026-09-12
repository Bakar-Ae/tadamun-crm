SELECT
    LEFT(
        REPLACE(REPLACE(DIGEST_TEXT, CHAR(10), ' '), CHAR(13), ' '),
        140
    ) AS statement_pattern,
    COUNT_STAR AS executions,
    ROUND(SUM_TIMER_WAIT / 1000000000000, 3) AS total_seconds,
    ROUND(AVG_TIMER_WAIT / 1000000000, 3) AS average_ms,
    SUM_ROWS_EXAMINED AS rows_examined,
    SUM_ROWS_SENT AS rows_sent
FROM performance_schema.events_statements_summary_by_digest
WHERE SCHEMA_NAME = DATABASE()
  AND DIGEST_TEXT IS NOT NULL
  AND DIGEST_TEXT REGEXP '^(SELECT|INSERT|UPDATE|DELETE)'
  AND COUNT_STAR >= 5
ORDER BY AVG_TIMER_WAIT DESC
LIMIT 10;
