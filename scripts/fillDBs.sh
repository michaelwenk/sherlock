curl -X POST -i 'http://localhost:8081/sherlock-db-service-dataset/replaceAll?nuclei=13C&setLimits=true'
curl -X POST -i 'http://localhost:8081/sherlock-db-service-statistics/hybridization/replaceAll?nuclei=13C'
curl -X POST -i 'http://localhost:8081/sherlock-db-service-statistics/connectivity/replaceAll?nuclei=13C'
curl -X POST -i 'http://localhost:8081/sherlock-db-service-statistics/hosecode/replaceAll?nuclei=13C&maxSphere=6'

## before publishing
# copy the HOSE code map from shared volume into local data folder
# docker cp sherlock-core:/data/hosecode/hosecodes.json backend/sherlock-core/data/hosecode/
# delete all result entries in DB via CMD or button frontend
# curl -X DELETE -i 'http://localhost:8081/sherlock-db-service-result/deleteAll'