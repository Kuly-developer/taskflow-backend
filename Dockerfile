# ደረጃ 1፦ ኮዱን በMaven ማጠናቀር
FROM maven:3.8.1-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# ደረጃ 2፦ የተጠናቀረውን የጃቫ Fat JAR ፋይል ማስነሳት
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/taskflow-backend-1.0.0-SNAPSHOT-fat.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
