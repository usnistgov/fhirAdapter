# ==========================================
# Stage 1: Build JAR
# ==========================================
FROM eclipse-temurin:8-jdk AS fhir-adapter-builder

WORKDIR /app

RUN apt-get update && apt-get install -y \
    git maven jq\
    && rm -rf /var/lib/apt/lists/*

# Copy Source Code
COPY . .
# Install dependencies
RUN chmod +x ./dependencies.sh && chmod +x ./build-with-dependencies.sh && ./build-with-dependencies.sh
# Create Artifact
RUN mvn clean package

# ==========================================
# Stage 2: Build Deploy
# ==========================================
FROM tomcat:9.0.117-jdk8-temurin-noble AS fhir-adapter-deployer
RUN rm -rf /usr/local/tomcat/webapps/*
RUN rm -rf /usr/local/tomcat/webapps.dist
RUN sed -i '/<\/web-app>/i \
    <error-page>\n\
      <exception-type>java.lang.Throwable<\/exception-type>\n\
      <location>/error.html<\/location>\n\
    <\/error-page>\n \
    <error-page>\n\
      <error-code>0<\/error-code>\n\
      <location>/error.html<\/location>\n\
    <\/error-page>\n' /usr/local/tomcat/conf/web.xml
COPY ./error.html /usr/local/tomcat/webapps/ROOT/error.html

# Application logs to stdout. Tomcat sends the webapp's own logger (servlet
# exceptions, Spring startup failures) to a dated file under logs/ that no
# container platform reads. Add the console handler next to the file handler
# so the same lines reach whatever collects stdout.
RUN set -eux; \
    F=/usr/local/tomcat/conf/logging.properties; \
    sed -i 's#^\(org\.apache\.catalina\.core\.ContainerBase\.\[Catalina\]\.\[localhost\]\.handlers = \)2localhost\.org\.apache\.juli\.AsyncFileHandler$#\12localhost.org.apache.juli.AsyncFileHandler, java.util.logging.ConsoleHandler#' "$F"; \
    grep -q '^org\.apache\.catalina\.core\.ContainerBase\.\[Catalina\]\.\[localhost\]\.handlers = 2localhost\.org\.apache\.juli\.AsyncFileHandler, java\.util\.logging\.ConsoleHandler$' "$F"
COPY --from=fhir-adapter-builder ./target/fhirAdapter.war /usr/local/tomcat/webapps/fhirAdapter.war
