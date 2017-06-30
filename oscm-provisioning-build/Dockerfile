FROM openjdk:8u131-jdk-alpine

COPY oscm-provisioning-main/target/oscm-provisioning.jar /opt/jar
COPY oscm-provisioning-main/src/test/resources/keystore.jks /opt/keystore

ENTRYPOINT ["java", "-jar", "/opt/jar/oscm-provisioning.jar"]
CMD ["-c", "env", "-l", "stdout", "ALL"]