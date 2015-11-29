CREATE OR REPLACE SQL SECURITY INVOKER VIEW MethodInvocations1 AS
  SELECT
    DISTINCT
    m.packageName                                                                      AS Package,
    m.declaringType                                                                    AS DeclaringType,
    m.signature                                                                        AS Method,
    GROUP_CONCAT(DISTINCT CONCAT(a.name, ' ', a.version) SEPARATOR ', ')               AS OccursInApplications,
    GROUP_CONCAT(DISTINCT j.environment SEPARATOR ', ')                                AS CollectedInEnvironments,
    IF(max(i.invokedAtMillis) = 0, NULL, FROM_UNIXTIME(MAX(i.invokedAtMillis / 1000))) AS LastInvokedAt,
    MIN(j.startedAt)                                                                   AS CollectedSince,
    DATEDIFF(MAX(j.dumpedAt), MIN(j.startedAt))                                        AS CollectedDays,
    SUM(i.invocationCount)                                                             AS OccursInCollectionIntervals
  FROM invocations i
    INNER JOIN applications a ON a.id = i.applicationId
    INNER JOIN methods m ON m.id = i.methodId
    INNER JOIN jvms j ON j.id = i.jvmId
  GROUP BY m.packageName, m.declaringType, m.signature;
