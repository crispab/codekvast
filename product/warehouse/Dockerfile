FROM mariadb:10
MAINTAINER Olle Hallin "olle.hallin@crisp.se"

ENV MYSQL_ROOT_PASSWORD root
ENV MYSQL_DATABASE codekvast_warehouse
ENV MYSQL_USER codekvast
ENV MYSQL_PASSWORD codekvast

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0x219BD9C9
RUN echo "deb http://repos.azulsystems.com/ubuntu stable main" >> /etc/apt/sources.list.d/zulu.list
RUN apt-get update && apt-get install -y ntpdate zulu-8

EXPOSE 3306
VOLUME /tmp/codekvast
