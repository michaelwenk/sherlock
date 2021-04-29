FROM openjdk:11
COPY target/webcase-casekit-0.0.1-SNAPSHOT.jar /
ENTRYPOINT ["java", "-jar", "webcase-casekit-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080
COPY data/nmrshiftdb/nmrshiftdb2withsignals.sd data/nmrshiftdb/nmrshiftdb2withsignals.sd