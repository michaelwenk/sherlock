FROM adoptopenjdk/openjdk11:alpine
COPY target/sherlock-gateway-0.0.1-SNAPSHOT.jar /
ENTRYPOINT ["java", "-jar", "sherlock-gateway-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080