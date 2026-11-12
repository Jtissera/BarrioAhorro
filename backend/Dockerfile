# Ubicación: backend/Dockerfile  (el contexto de build es la carpeta backend/)

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew \
 && ./gradlew --no-daemon bootJar -x test \
 && cp "$(ls build/libs/*.jar | grep -v plain | head -n 1)" /app/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app \
 && mkdir -p /data/uploads \
 && chown -R app:app /data
COPY --from=build /app/app.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
