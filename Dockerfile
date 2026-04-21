# Estágio 1: Build com Gradle 8
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
# Executa o build do JAR
RUN gradle :server:shadowJar --no-daemon

# Estágio 2: Execução
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copia o JAR gerado (ajustado para o caminho padrão do shadow)
COPY --from=build /app/server/build/libs/fichacorte-server.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
