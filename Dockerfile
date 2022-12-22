# Docker multi-stage build
FROM maven:3-jdk-11

WORKDIR /workdir
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ /workdir/src/
RUN ls -l
RUN mvn package

FROM openjdk:11

MAINTAINER Maxim Zheltoukhov

COPY --from=0 "/workdir/target/telequests.jar" app.jar
ENV JAVA_OPTS=""

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]