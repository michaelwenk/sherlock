FROM openjdk:11
COPY target/sherlock-db-service-dataset-0.0.1-SNAPSHOT.jar /
ENTRYPOINT ["java", "-jar", "sherlock-db-service-dataset-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080
COPY data/nmrshiftdb/ data/nmrshiftdb/
COPY data/coconut/ data/coconut/