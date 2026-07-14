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

# Create non-root user. /app is chowned before we drop privileges, because the AOT training step
# below runs as this user and has to write the cache into it.
RUN addgroup --system matros && adduser --system --ingroup matros matros
WORKDIR /app
RUN chown -R matros:matros /app
USER matros:matros

# Copy the JAR
COPY --chown=matros:matros server/target/server*.jar application.jar

# Train a JDK AOT cache (JEP 483/514, Java 24+): boot once, exit at context refresh, and keep the
# loaded and linked class image. Cuts cold start ~7.0s -> ~4.7s.
#
# It must be generated inside this image: an AOT cache is pinned to the exact JDK that wrote it, so
# one baked on a build host would simply be rejected here. A mismatched or missing cache is only a
# warning to the JVM, never fatal — the container still starts, just slower.
#
# The training boot uses ephemeral ports and a throwaway data dir so it cannot collide with anything
# or leave a database behind in the image.
RUN java -XX:AOTCacheOutput=app.aot \
        -Dspring.context.exit=onRefresh \
        -Djava.awt.headless=true \
        -jar application.jar \
        --server.port=0 \
        --app.base-path=/tmp/aot-train \
        --app.start-browser=false \
        --app.system-tray=false \
        --app.mail.smtp.port=0 \
        --app.mail.imap.port=0 \
    && rm -rf /tmp/aot-train

# Expose ports (App, Mail, etc.)
EXPOSE 9090 2525 1143

VOLUME /data
VOLUME /tmp

# headless=true keeps AWT off: no tray, no splash, no browser auto-open in a container.
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-Djava.awt.headless=true", "-jar", "application.jar"]
