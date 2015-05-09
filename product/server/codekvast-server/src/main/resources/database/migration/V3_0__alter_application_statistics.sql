ALTER TABLE application_statistics ALTER COLUMN num_startup_signatures
RENAME TO num_bootstrap_signatures;

ALTER TABLE application_statistics ADD column max_started_at_millis BIGINT AFTER first_started_at_millis;

UPDATE application_statistics stats
SET max_started_at_millis = (SELECT MAX(jvm.started_at_millis)
                             FROM jvm_info jvm
                             WHERE jvm.application_id = stats.application_id
                             GROUP BY jvm.application_id);

ALTER TABLE application_statistics ALTER COLUMN max_started_at_millis SET NOT NULL;

COMMENT ON COLUMN application_statistics.max_started_at_millis IS 'When was the last time this application was started?';
