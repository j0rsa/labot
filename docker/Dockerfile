FROM eclipse-temurin:17-jre-alpine

ARG JAR_FILE
RUN mkdir -p /app
WORKDIR /app
COPY ${JAR_FILE} ./app.jar
CMD ["java","-Djava.security.egd=file:/dev/./urandom -XX:+UseContainerSupport -XX:InitialRAMPercentage=80.0 -XX:MinRAMPercentage=80.0 -XX:MaxRAMPercentage=80.0 -Dconfig.file=/app/config/application.conf","-jar","./app.jar"]
