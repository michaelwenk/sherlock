FROM adoptopenjdk/openjdk11:alpine
COPY target/sherlock-db-service-hosecode-0.0.1-SNAPSHOT.jar /
ENTRYPOINT ["java", "-Xms8g", "-jar", "sherlock-db-service-hosecode-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080