# Use an official OpenJDK runtime as a parent image
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy the application's jar file into the container
COPY target/simple-crawler-1.0-SNAPSHOT-jar-with-dependencies.jar /app/app.jar

# Copy the .env file into the container
COPY .env /app/.env

# Run the jar file
ENTRYPOINT ["java","-jar","/app/app.jar"]