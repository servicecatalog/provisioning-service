FROM openjdk:8u131-jdk-alpine

COPY oscm-provisioning-main/target/oscm-provisioning-0.0.1-SNAPSHOT.jar /opt/jar/

ENTRYPOINT ["java", "-jar", "/opt/jar/oscm-provisioning-0.0.1-SNAPSHOT.jar"]
CMD ["-c", "env", "-l", "stdout", "ALL"]
