FROM adoptopenjdk/openjdk11:alpine
COPY target/sherlock-db-service-result-0.0.1-SNAPSHOT.jar /
ENTRYPOINT ["java", "-jar", "sherlock-db-service-result-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080