#!/usr/bin/env bash
#-----------------------------------------------------------------------
# Wrapper for docker-compose that sets the project name, so that the
# containers get the same name no matter what directory the file lives in.
#-----------------------------------------------------------------------

# Which version of Codekvast Warehouse?
declare WAREHOUSE_VERSION=${WAREHOUSE_VERSION:-latest}

# On what port should Codekvast Warehouse expose the REST API (0=random)?
# Edit to suit your needs.
declare WAREHOUSE_PORT=${WAREHOUSE_PORT:-0}

# Where to queue data files uploaded by Codekvast Agent?
declare WAREHOUSE_QUEUE_DIR=${WAREHOUSE_QUEUE_DIR:-/var/codekvast}

# How should Docker handle restarts of warehouse containers?
declare WAREHOUSE_RESTART_POLICY=${WAREHOUSE_RESTART_POLICY:-unless-stopped}

# Where to put log files?
declare WAREHOUSE_LOG_DIR=${WAREHOUSE_LOG_DIR:-/var/log}

# Where should the MariaDB database live?
declare WAREHOUSE_DATABASE_DIR=${WAREHOUSE_DATABASE_DIR:-/var/lib/codekvast-database}

#--- No changes below this line! ---------------------------------------

cat << EOF | docker-compose -p ${WAREHOUSE_CONTAINER_PREFIX:-codekvast} -f- $@
version: '2'

services:

  app:
    image: crisp/codekvast-warehouse:${WAREHOUSE_VERSION}

    volumes:
    - ${WAREHOUSE_QUEUE_DIR}:/var/codekvast
    - ${WAREHOUSE_LOG_DIR}:/var/log

    links:
    - db:database

    restart: ${WAREHOUSE_RESTART_POLICY}

    ports:
    - "${WAREHOUSE_PORT}:8080"

  db:
    image: mariadb:10

    environment:
    - MYSQL_ROOT_PASSWORD=root
    - MYSQL_DATABASE=codekvast
    - MYSQL_USER=codekvast
    - MYSQL_PASSWORD=codekvast
    - TERM=xterm-256color

    volumes:
    - ${WAREHOUSE_DATABASE_DIR}:/var/lib/mysql

    expose:
    - "3306"

    command: --character-set-server=utf8 --collation-server=utf8_general_ci --default-storage-engine=innodb
EOF
