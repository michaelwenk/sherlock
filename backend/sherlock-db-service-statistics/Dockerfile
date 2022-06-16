FROM adoptopenjdk/openjdk11:alpine
COPY target/sherlock-db-service-statistics-0.0.1-SNAPSHOT.jar /
ENTRYPOINT ["java", "-Xmx6G", "-jar", "sherlock-db-service-statistics-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080