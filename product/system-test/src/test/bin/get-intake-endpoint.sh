#!/usr/bin/env bash

declare port=$(jps -m | grep "codekvast-intake-" | head -n 1 | egrep --only-matching "\-\-management.server.port=[0-9]+" | cut -d= -f2)
if [[ -n "$port" ]]; then
    echo "http://localhost:$port/management/health"
fi
