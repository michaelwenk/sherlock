
cd backend/sherlock-discovery-server && mvn clean package
cd ../..
cd backend/sherlock-gateway && mvn clean package
cd ../..
cd backend/sherlock-core && mvn clean package
cd ../..
cd backend/sherlock-db-service-dataset && mvn clean package
cd ../..
cd backend/sherlock-db-service-statistics && mvn clean package
cd ../..
cd backend/sherlock-db-service-result && mvn clean package
cd ../..