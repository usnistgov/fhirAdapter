FROM tomcat:9.0.98-jdk8-temurin-noble
RUN rm -rf /usr/local/tomcat/webapps/*
RUN rm -rf /usr/local/tomcat/webapps.dist
RUN sed -i '/<\/web-app>/i \
    <error-page>\n\
      <exception-type>java.lang.Throwable<\/exception-type>\n\
      <location>/error.jsp<\/location>\n\
    <\/error-page>\n' /usr/local/tomcat/conf/web.xml
COPY ./target/fhirAdapter.war /usr/local/tomcat/webapps/fhirAdapter.war
