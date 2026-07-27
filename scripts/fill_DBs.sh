#!/bin/bash

# Enable automatic export of variables
set -a
# Source the .env file in root directory to load environment variables
source .env
# Disable automatic export of variables
set +a

echo "-> deleting results..." && \
time curl -s -o /dev/null -X DELETE -i 'http://localhost:8080/result/deleteAll' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
echo "-> results deleted." && \

echo "-> deleting datasets..." && \
time curl -s -o /dev/null -X DELETE -i 'http://localhost:8080/dataset/deleteAll' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
echo "-> datasets deleted." && \

echo "-> filling datasets..." && \
echo "-> setting dataset limits..." && \
time curl -s -o /dev/null -X GET -i 'http://localhost:8080/dataset/updateMultiplicitySectionsSettings?nucleus=13C&minShift=-5&maxShift=230&binSize=2' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
echo "-> dataset limits set." && \

echo "-> filling datasets from nmrshiftdb..." && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDBNameAndFileIndex?nucleus=13C&dbName=nmrshiftdb&fileIndex=0&minShift=-5&maxShift=230' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
echo "-> filling datasets from nmrshiftdb done." && \

echo "-> filling datasets from coconut database..." && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDBNameAndFileIndex?nucleus=13C&dbName=coconut&fileIndex=0&minShift=-5&maxShift=230' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDBNameAndFileIndex?nucleus=13C&dbName=coconut&fileIndex=1&minShift=-5&maxShift=230' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDBNameAndFileIndex?nucleus=13C&dbName=coconut&fileIndex=2&minShift=-5&maxShift=230' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDBNameAndFileIndex?nucleus=13C&dbName=coconut&fileIndex=3&minShift=-5&maxShift=230' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
echo "-> filling datasets from coconut database done." && \

echo "-> updating indices in datasets collection..." && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/updateIndexes' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
echo "-> updated indices in datasets collection." && \

echo "-> datasets filled." && \

echo "-> building statistics and fragments..." && \
curl -s -o /dev/null -X POST -i 'http://localhost:8080/database/fillDatabases?nucleus=13C&maxSphere=6' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD
echo "-> statistics and fragments build process invoked. This may take a while...\nPlease check the logs for progress information."
