FROM adoptopenjdk/openjdk11:alpine
COPY target/sherlock-core-0.0.1-SNAPSHOT.jar /
#ENTRYPOINT ["java", "-Xmx6G", "-jar", "sherlock-core-0.0.1-SNAPSHOT.jar" ]
ENTRYPOINT ["java", "-jar", "sherlock-core-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080
COPY data/ data/
RUN apk add --update --no-cache python2 make g++
RUN cd data/lsd/PyLSD/LSD && make clean && sh install.sh