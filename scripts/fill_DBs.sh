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
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDirPathAndDbName?pathToDir=%2Fdata%2Fnmrshiftdb&dbName=nmrshiftdb&nucleus=13C&minShift=-5&maxShift=230' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
echo "-> filling datasets from nmrshiftdb done." && \

# echo "-> filling datasets from acd_labs_predictions database containing spectral predictions..." && \
# time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/insertByDirPathAndDbName?pathToDir=%2Fdata%2Facd_labs_predictions&dbName=acd_labs_predictions&nucleus=13C&minShift=-5&maxShift=230' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
# echo "-> filling datasets from acd_labs_predictions database done." && \

echo "-> updating indices in datasets collection..." && \
time curl -s -o /dev/null -X POST -i 'http://localhost:8080/dataset/updateIndexes' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD && \
echo "-> updated indices in datasets collection." && \

echo "-> datasets filled." && \

echo "-> building statistics and fragments..." && \
curl -s -o /dev/null -X POST -i 'http://localhost:8080/database/buildStatistics?nucleus=13C&maxSphere=6' -u $SPRING_SECURITY_USER_NAME:$SPRING_SECURITY_USER_PASSWORD
echo "-> statistics and fragments build process invoked. This may take a while...\nPlease check the logs and 'http://localhost:8080/database/buildStatistics/status' for progress information."
