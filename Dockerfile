FROM maven:3.6-jdk-8

EXPOSE 3030

ADD src /sources/src
ADD pom.xml /sources/pom.xml

RUN cd sources && mvn -DskipTests=true package
RUN mkdir /app && cp /sources/target/fuseki-oauth-security-1.0-SNAPSHOT.jar /app/server.jar
RUN mkdir /usr/local/fuseki && cp -r /sources/target/webapp /usr/local/fuseki/webapp

ADD conf/admin_security_data.ttl /sec_data.ttl
ADD conf /usr/local/fuseki

ENV FUSEKI_HOME=/usr/local/fuseki
ENV FUSEKI_BASE=/usr/local/fuseki
ENV JPDA_ADDRESS="15005"
ENV JPDA_TRANSPORT=dt_socket
ENV SERVER=y
ENV JJPDA_SUSPEND=n

CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=15005", "-jar", "/app/server.jar"]



