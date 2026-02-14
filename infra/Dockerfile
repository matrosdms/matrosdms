FROM eclipse-temurin:25-jdk-noble

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

# Configure headless mode
ENV JAVA_OPTS="-Djava.awt.headless=true"

# Create non-root user
RUN addgroup --system matros && adduser --system --ingroup matros matros
USER matros:matros

WORKDIR /app

# Copy the JAR
COPY --chown=matros:matros server/target/server*.jar application.jar

# Expose ports (App, Mail, etc.)
EXPOSE 9090 2525 1143

VOLUME /data
VOLUME /tmp

# Added --enable-preview (Required for Java 25 features enabled in your POM)
ENTRYPOINT ["java", "--enable-preview", "-Djava.awt.headless=true", "-jar", "application.jar"]