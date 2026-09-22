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
COPY --from=fhir-adapter-builder ./target/fhirAdapter.war /usr/local/tomcat/webapps/fhirAdapter.war
