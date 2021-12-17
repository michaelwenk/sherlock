FROM adoptopenjdk/openjdk11:alpine
COPY target/sherlock-pylsd-0.0.1-SNAPSHOT.jar /
ENTRYPOINT ["java", "-jar", "sherlock-pylsd-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080
RUN apk add --update --no-cache python2
COPY data/ data/