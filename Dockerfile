# Teamsheet: one image. Angular is built, then served as static files by Spring Boot.

# ---- 1) Frontend (Angular 22 needs Node >= 24.15) ----
FROM node:24-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npx ng build --configuration production

# ---- 2) Backend jar with the frontend inside ----
FROM eclipse-temurin:21-jdk AS api
WORKDIR /api
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN sh ./mvnw -q -B dependency:go-offline
COPY backend/src src
COPY --from=web /web/dist/frontend/browser/ src/main/resources/static/
# Tests need Docker (Testcontainers); they run locally / in CI, not in the image build.
RUN sh ./mvnw -q -B -DskipTests package

# ---- 3) Runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=api /api/target/teamsheet-backend-*.jar app.jar
COPY deploy/docker-entrypoint.sh entrypoint.sh
RUN sed -i 's/\r$//' entrypoint.sh && chmod +x entrypoint.sh
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
