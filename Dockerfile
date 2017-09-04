FROM openjdk:8u131-jdk-alpine

COPY oscm-provisioning-build/oscm-provisioning-main/target/oscm-provisioning.jar /opt/jar/

ENTRYPOINT ["java", "-jar", "/opt/jar/oscm-provisioning.jar"]
CMD ["-c", "env", "-l", "stdout", "ALL"]