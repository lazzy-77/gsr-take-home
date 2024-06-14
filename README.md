TODO
Assumptions made
    Timestamp is in ISO 8601 rather than epoch for human readability

## Prerequisites
1. Docker Desktop installed
2. Java JDK 17 in installed
3. Maven CLI is installed
4. Configure `application.properties` to add or remove crypto pairs

# Start up
1. Build the project with `mvn clean install`
2. Bring up all the needed containers with `docker compose up -d`
3. Start the app with `java -jar target/gsr-take-home-0.0.1-SNAPSHOT.jar`

## Kafdrop
Kafdrop is a UI for kafka
You can view the individual topics and messages on each topic by clicking this [link](http://localhost:9000)