FROM eclipse-temurin:21-jdk-jammy

# Install minimal native libs required by PDFBox (AWT, fonts)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libx11-6 \
    libfontconfig1 \
    libfreetype6 \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_OPTS="-Djava.awt.headless=true"

RUN addgroup --system matros && adduser --system --ingroup matros matros
USER matros:matros

WORKDIR /app
# Copy the Spring Boot JAR directly
COPY --chown=matros:matros server/target/server*.jar application.jar

# Expose ports for application and mail services
EXPOSE 9090 2525 1143

VOLUME /data
VOLUME /tmp

#docker run -p 9090:9090 -p 2525:2525 -p 1143:1143 ghcr.io/matrosdms/matrosdms:develop

ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "application.jar"]