FROM openjdk:11
COPY target/webcase-db-service-hosecode-0.0.1-SNAPSHOT.jar /
ENTRYPOINT ["java", "-jar", "webcase-db-service-hosecode-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080
COPY data/nmrshiftdb/nmrshiftdb2withsignals.sd data/nmrshiftdb/nmrshiftdb2withsignals.sd