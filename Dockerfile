# Etapa 1: compila el JAR dentro de la imagen. Railway solo recibe el código
# fuente (build/ está en .gitignore), así que el artefacto no puede venir del CI.
# Los tests no se repiten aquí: el CI ya los ejecutó antes de invocar el CD.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar

# Etapa 2: imagen de ejecución mínima, sin JDK ni código fuente
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY --from=build --chown=spring:spring /workspace/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
