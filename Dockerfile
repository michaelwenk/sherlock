FROM alpine:3.22.2

COPY target/sherlock-0.0.1-SNAPSHOT.jar /
COPY data/ data/

RUN apk update && apk upgrade && \
    apk add --no-cache openjdk21 make g++ python3
RUN cd data/lsd/PyLSD/LSD && make clean && sh install.sh

ENTRYPOINT ["java", "-jar", "sherlock-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080