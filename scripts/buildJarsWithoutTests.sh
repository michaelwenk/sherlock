
cd backend/sherlock-discovery-server && mvn clean package -DskipTests
cd ../..
cd backend/sherlock-gateway && mvn clean package -DskipTests
cd ../..
cd backend/sherlock-core && mvn clean package -DskipTests
cd ../..
cd backend/sherlock-pylsd && mvn clean package -DskipTests
cd ../..
cd backend/sherlock-db-service-dataset && mvn clean package -DskipTests
cd ../..
cd backend/sherlock-db-service-statistics && mvn clean package -DskipTests
cd ../..
cd backend/sherlock-db-service-result && mvn clean package -DskipTests
cd ../..
cd backend/sherlock-db-service-hosecode && mvn clean package -DskipTests
cd ../..