FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY gradlew gradle ./
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre-alpine

RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D appuser
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown -R 1000:1000 /app
USER 1000

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
