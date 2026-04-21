# Estágio 1: Construção (Build)
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copia os arquivos do Gradle e o código fonte
COPY . .

# Dá permissão de execução para o gradlew e constrói o ShadowJar
RUN chmod +x ./gradlew
RUN ./gradlew :server:shadowJar --no-daemon

# Estágio 2: Execução (Runtime)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copia apenas o arquivo JAR gerado no estágio anterior
COPY --from=build /app/server/build/libs/fichacorte-server.jar app.jar

# Define a porta que o Ktor vai usar (o Render fornece a variável PORT)
EXPOSE 8080

# Comando para iniciar o site
CMD ["java", "-jar", "app.jar"]
