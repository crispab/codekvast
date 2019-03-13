#!/usr/bin/env bash
#---------------------------------------------------------------------------------
# Daily cleanup tasks
#---------------------------------------------------------------------------------

# Remove rotated access logs older than 7 days
find /var/log/codekvast/* -name '*access.*.log' -ctime +7 -delete

# Remove rotated application logs older than 7 days
find /var/log/codekvast/* -name 'application.log.*.*' -ctime +7 -delete
