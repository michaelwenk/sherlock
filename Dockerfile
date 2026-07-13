FROM eclipse-temurin:25-jdk-alpine-3.23

COPY pom.xml pom.xml
COPY src src
COPY scripts scripts
COPY lib lib
COPY data/lsd data/lsd
# COPY data/nmrshiftdb data/nmrshiftdb
# COPY data/coconut data/coconut

RUN apk update && apk upgrade && \
    apk add --no-cache maven make g++ python3
RUN sh scripts/install_predictorc_jar.sh && sh scripts/install_casekit.sh && mvn clean package -DskipTests
RUN cd data/lsd/PyLSD/LSD && make clean && sh install.sh

ENTRYPOINT ["java", "-jar", "target/sherlock-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080