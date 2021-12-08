FROM openjdk:11
COPY target/sherlock-pylsd-0.0.1-SNAPSHOT.jar /
ENTRYPOINT ["java", "-jar", "sherlock-pylsd-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080
RUN apt-get update && apt-get install python2.7 --yes
COPY data/ data/