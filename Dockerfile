# Use the official OpenJDK 11 base image
FROM adoptopenjdk:11-jre-hotspot

# Set the working directory inside the container
WORKDIR /app

# Copy the packaged Spring Boot application JAR file into the container at /app
COPY target/hyp-backend-api.jar /app/hyp-backend-api.jar

# Specify the command to run your Spring Boot application when the container starts
ENTRYPOINT ["java", "-jar", "-Dserver.port=9090", "/app/hyp-backend-api.jar"]
