# Use BellSoft Liberica OpenJDK Alpine image for Java 17
FROM bellsoft/liberica-openjdk-alpine:17

# Set the working directory inside the container
WORKDIR /app

# Copy the .env file into the container at /app
#COPY .env .env

# Set environment variables from .env file
#ENV $(cat .env | xargs)

# Copy the packaged Spring Boot application JAR file into the container at /app
COPY target/hyp-backend-api.jar /app/hyp-backend-api.jar

# Specify the command to run your Spring Boot application when the container starts
ENTRYPOINT ["java", "-jar", "-Dserver.port=9090", "/app/hyp-backend-api.jar"]
