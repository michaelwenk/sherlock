curl -X POST -i 'http://localhost:8081/webcase-db-service-dataset/replaceAll?nuclei=13C,1H&setLimits=true'
curl -X POST -i 'http://localhost:8081/webcase-db-service-statistics/hybridization/replaceAll?nuclei=13C'
curl -X POST -i 'http://localhost:8081/webcase-db-service-statistics/connectivity/replaceAll?nuclei=13C'
curl -X POST -i 'http://localhost:8081/webcase-db-service-hosecode/replaceAll?nuclei=13C&maxSphere=6'
