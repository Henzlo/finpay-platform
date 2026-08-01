FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN mkdir -p /tmp/finpay-reports
COPY target/*.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m",
            "-jar", "app.jar"]
