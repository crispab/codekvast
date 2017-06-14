#!/usr/bin/env bash

declare port=$(jps -m | grep codekvast-warehouse- | head --lines=1 | egrep --only-matching "server.port=[0-9]+" | cut -d= -f2)
if [ -n "$port" ]; then
    echo "http://localhost:$port"
fi
