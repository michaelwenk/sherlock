#!/bin/bash

sh scripts/fill_datasets.sh && \
# delete all result entries
curl -X DELETE -i 'http://localhost:8080/result/deleteAll' && \
# build statistics
curl -X POST -i 'http://localhost:8080/statistics/hybridization/replaceAll?nuclei=13C'
curl -X POST -i 'http://localhost:8080/statistics/connectivity/replaceAll?nuclei=13C'
curl -X POST -i 'http://localhost:8080/statistics/heavyAtomStatistics/replaceAll'
curl -X POST -i 'http://localhost:8080/statistics/hosecode/replaceAll?nuclei=13C&maxSphere=6' && curl -X POST -i 'http://localhost:8080/statistics/hosecode/buildStatistics'
# fragments...