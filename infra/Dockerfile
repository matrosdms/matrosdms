# -----------------------------------------------------------------------------
# Single Stage Build - Simplified
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S matros && adduser -S matros -G matros
USER matros:matros

WORKDIR /app

# Copy the Spring Boot JAR directly
COPY --chown=matros:matros server*.jar application.jar

VOLUME /data
VOLUME /tmp

ENTRYPOINT ["java", "-jar", "application.jar"]
