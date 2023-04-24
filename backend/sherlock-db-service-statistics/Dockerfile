FROM adoptopenjdk/openjdk11:alpine
COPY target/sherlock-db-service-statistics-0.0.1-SNAPSHOT.jar /
ENTRYPOINT ["java", "-Xmx4G", "-jar", "sherlock-db-service-statistics-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080
COPY data/ data/
COPY scripts/ scripts/
RUN apk add --update --no-cache nodejs npm

#RUN apk add --update --no-cache python3 bash
#RUN mkdir /root/.conda \
#    && sh /data/miniconda/Miniconda3-latest-Linux-x86_64.sh -b
#ENV PATH="/root/miniconda3/bin:${PATH}"
#ARG PATH="/root/miniconda3/bin:${PATH}"
#RUN conda create -c conda-forge -n my-rdkit-env rdkit

#RUN ["chmod", "+x", "/scripts/entrypoint.sh"]

#ENTRYPOINT ["./scripts/entrypoint.sh"]


RUN mkdir /data/temp

RUN cd scripts && npm install