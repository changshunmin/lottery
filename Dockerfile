FROM openjdk:8-jre-slim

WORKDIR /app

COPY target/lottery-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]