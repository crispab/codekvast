--
-- Copyright (c) 2015-2016 Crisp AB
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in
-- all copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
-- THE SOFTWARE.
--

${ifMariadbStart}
CREATE OR REPLACE VIEW MethodInvocations1 AS
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
${ifMariadbEnd}
