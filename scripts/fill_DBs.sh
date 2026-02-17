#!/bin/bash

echo "-> deleting results..." && \
time curl -s -o /dev/null -X DELETE -i 'http://localhost:8080/result/deleteAll' && \
echo "-> results deleted." && \

echo "-> deleting datasets..." && \
time curl -s -o /dev/null -X DELETE -i 'http://localhost:8080/dataset/deleteAll' && \
echo "-> datasets deleted." && \

echo "-> filling datasets..." && \
echo "-> setting dataset limits..." && \
time curl -s -o /dev/null -X GET -i 'http://localhost:8080/dataset/updateMultiplicitySectionsSettings?nucleus=13C&minShift=-5&maxShift=230&binSize=2' && \
echo "-> dataset limits set." && \

echo "-> filling datasets from nmrshiftdb..." && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDBNameAndFileIndex?nucleus=13C&dbName=nmrshiftdb&fileIndex=0&minShift=-5&maxShift=230' && \
echo "-> filling datasets from nmrshiftdb done." && \

# echo "-> filling datasets from coconut database..." && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDBNameAndFileIndex?nucleus=13C&dbName=coconut&fileIndex=0&minShift=-5&maxShift=230'  && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDBNameAndFileIndex?nucleus=13C&dbName=coconut&fileIndex=1&minShift=-5&maxShift=230'  && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDBNameAndFileIndex?nucleus=13C&dbName=coconut&fileIndex=2&minShift=-5&maxShift=230'  && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDBNameAndFileIndex?nucleus=13C&dbName=coconut&fileIndex=3&minShift=-5&maxShift=230'  && \
# echo "-> filling datasets from coconut database done." && \

echo "-> datasets filled." && \

echo "-> building statistics and fragments..." && \
curl -s -o /dev/null -X POST -i 'http://localhost:8080/database/fillDatabases?nucleus=13C&maxSphere=6'
echo "-> statistics and fragments build process invoked."
