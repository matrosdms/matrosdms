# -----------------------------------------------------------------------------
# STAGE 1: Build - Extract Layers
# -----------------------------------------------------------------------------
# FIX: Capitalized 'AS' to silence linter warning
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /builder

# CLEVER CHANGE: Path is relative to the PROJECT ROOT now
# We specifically target the module's target folder
COPY server/target/*-exec.jar application.jar

# Extract layers
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
