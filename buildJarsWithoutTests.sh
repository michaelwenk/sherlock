
cd backend/webcase-discovery-server && mvn clean package -DskipTests
cd ../..
cd backend/webcase-gateway && mvn clean package -DskipTests
cd ../..
cd backend/webcase-core && mvn clean package -DskipTests
cd ../..
cd backend/webcase-dereplication && mvn clean package -DskipTests
cd ../..
cd backend/webcase-elucidation && mvn clean package -DskipTests
cd ../..
cd backend/webcase-pylsd && mvn clean package -DskipTests
cd ../..
cd backend/webcase-result && mvn clean package -DskipTests
cd ../..
cd backend/webcase-db-service-dataset && mvn clean package -DskipTests
cd ../..
cd backend/webcase-db-service-statistics && mvn clean package -DskipTests
cd ../..
cd backend/webcase-db-service-result && mvn clean package -DskipTests
cd ../..
cd backend/webcase-db-service-hosecode && mvn clean package -DskipTests
cd ../..