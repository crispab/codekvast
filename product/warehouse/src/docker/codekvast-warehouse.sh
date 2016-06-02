#!/usr/bin/env bash
#-----------------------------------------------------------------------
# Wrapper for docker-compose that sets the project name, so that the
# containers get the same name no matter what directory the file lives in.
#-----------------------------------------------------------------------

# Where to look for data files from Codekvast Daemons?
declare WAREHOUSE_INPUT_DIR=/tmp/codekvast/warehouse

# Where to put log files?
declare WAREHOUSE_LOG_DIR=/var/log

# Where should the MariaDB database live?
declare DATABASE_DIR=/var/lib/codekvast-database

#--- No changes below this line! ---------------------------------------
declare DOCKER_COMPOSE_FILE=/tmp/codekvast-warehouse.docker-compose.yml

cat << EOF > ${DOCKER_COMPOSE_FILE}
version: '2'

services:

  app:
    image: crisp/codekvast-warehouse:latest

    volumes:
    - ${WAREHOUSE_INPUT_DIR}:/tmp/codekvast/.warehouse
    - ${WAREHOUSE_LOG_DIR}:/var/log

    links:
    - db:database

    restart: on-failure:10

    environment:
    - SPRING_PROFILES_ACTIVE=docker

    ports:
    - "8080"

  db:
    image: mariadb:10

    environment:
    - MYSQL_ROOT_PASSWORD=root
    - MYSQL_DATABASE=codekvast_warehouse
    - MYSQL_USER=codekvast
    - MYSQL_PASSWORD=codekvast
    - TERM=xterm-256color

    volumes:
    - ${DATABASE_DIR}:/var/lib/mysql

    expose:
    - "3306"

    command: --character-set-server=utf8 --collation-server=utf8_general_ci --default-storage-engine=innodb
EOF

docker-compose -p codekvast -f ${DOCKER_COMPOSE_FILE} $@
