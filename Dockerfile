# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -e -DskipTests clean package

FROM tomcat:10.1.29-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/target/session-ja4-sample.war /usr/local/tomcat/webapps/session-ja4-sample.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
