FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle :server:shadowJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/server/build/libs/fichacorte-server.jar /app/fichacorte.jar
WORKDIR /app
CMD ["java", "-jar", "fichacorte.jar"]
