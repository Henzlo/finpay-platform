FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m",
            "-jar", "app.jar"]
