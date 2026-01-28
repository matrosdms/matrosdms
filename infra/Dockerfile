# -----------------------------------------------------------------------------
# STAGE 1: Build - Extract Layers
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /builder

# Copy the repackaged Spring Boot jar (excludes .original)
COPY server/target/server*.jar application.jar

# Extract layers for optimized Docker caching
RUN java -Djarmode=layertools -jar application.jar extract

# -----------------------------------------------------------------------------
# STAGE 2: Run
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S matros && adduser -S matros -G matros
USER matros:matros

WORKDIR /app

# Copy layers from builder (no changes needed here)
COPY --from=builder /builder/dependencies/ ./
COPY --from=builder /builder/spring-boot-loader/ ./
COPY --from=builder /builder/snapshot-dependencies/ ./
COPY --from=builder /builder/application/ ./

VOLUME /data
VOLUME /tmp

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
