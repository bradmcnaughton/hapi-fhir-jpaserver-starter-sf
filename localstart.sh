#cd /c/workspace/hapi-fhir-jpaserver-starter-sf && mvn clean package -DskipTests && java -Xmx2048m -Djavax.net.debug=ssl,handshake,data,trustmanager -jar target/ROOT.war 
cd /c/workspace/hapi-fhir-jpaserver-starter-sf && mvn clean package -DskipTests && java -Xmx2048m -jar target/ROOT.war 
