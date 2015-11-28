CREATE OR REPLACE SQL SECURITY INVOKER VIEW MethodInvocations1 AS
  SELECT
    DISTINCT
    m.signature                                                                        AS Method,
    GROUP_CONCAT(DISTINCT a.name SEPARATOR ', ')                                       AS Applications,
    GROUP_CONCAT(DISTINCT a.version SEPARATOR ', ')                                    AS Versions,
    GROUP_CONCAT(DISTINCT j.environment SEPARATOR ', ')                                AS Environments,
    IF(max(i.invokedAtMillis) = 0, NULL, FROM_UNIXTIME(MAX(i.invokedAtMillis / 1000))) AS LastInvokedAt,
    MIN(j.startedAt)                                                                   AS CollectedSince
  FROM invocations i
    INNER JOIN applications a ON i.applicationId = a.id
    INNER JOIN methods m ON i.methodId = m.id
    INNER JOIN jvms j ON i.jvmId = j.id
  GROUP BY m.signature;
