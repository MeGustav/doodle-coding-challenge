# syntax=docker/dockerfile:1

# ---- build the jar ----
FROM eclipse-temurin:26-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ---- explode the jar into Spring Boot layers ----
FROM eclipse-temurin:26-jre AS extract
WORKDIR /application
COPY --from=build /workspace/build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- runtime image, one Docker layer per Spring Boot layer ----
FROM eclipse-temurin:26-jre
WORKDIR /application

COPY --from=extract /application/extracted/dependencies/ ./
COPY --from=extract /application/extracted/spring-boot-loader/ ./
COPY --from=extract /application/extracted/snapshot-dependencies/ ./
COPY --from=extract /application/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]