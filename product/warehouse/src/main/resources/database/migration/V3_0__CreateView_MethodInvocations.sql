CREATE OR REPLACE SQL SECURITY INVOKER VIEW MethodInvocations1 AS
  SELECT
    m.signature                                                              AS Method,
    a.name                                                                   AS Application,
    a.version                                                                AS Version,
    IF(i.invokedAtMillis = 0, NULL, FROM_UNIXTIME(i.invokedAtMillis / 1000)) AS InvokedAt,
    i.confidence                                                             AS Confidence
  FROM invocations i INNER JOIN methods m ON i.methodId = m.id
    INNER JOIN applications a ON i.applicationId = a.id;
