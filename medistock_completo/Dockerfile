# Dockerfile with java
FROM openjdk:17-jdk-slim
ARG JAR_FILE=target/demo-0.0.1-Gestion-Medicamentos.jar
COPY ${JAR_FILE} app_Farmacias.JAR_FILE
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app_Farmacias.JAR_FILE"]


