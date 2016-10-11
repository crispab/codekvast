FROM java:8-jre-alpine
MAINTAINER Olle Hallin "olle.hallin@crisp.se"
RUN mkdir -p /var/log /tmp/codekvast
VOLUME ["/var/log"]
VOLUME ["/tmp/codekvast"]
ENV SPRING_PROFILES_ACTIVE=docker
ADD ["${archivesBaseName}-${codekvastVersion}-all.jar", "${archivesBaseName}-${codekvastVersion}.jar"]
CMD ["java", "-jar", "${archivesBaseName}-${codekvastVersion}.jar"]
