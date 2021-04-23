-- Relies on base-data.sql

-- Enable automatic garbage collection for customer 1
UPDATE agent_state SET createdAt = '2018-01-01', lastPolledAt = '2018-01-01', garbage = FALSE;

UPDATE jvms
SET publishedAt = '2018-01-01';

DELETE
    FROM price_plan_overrides;

INSERT INTO price_plan_overrides (id, customerId, createdAt, createdBy, updatedAt, note, retentionPeriodDays)
    VALUES (1, 1, NOW(), 'integration test', NOW(), 'Inserted by weedable-data.sql', 1);
