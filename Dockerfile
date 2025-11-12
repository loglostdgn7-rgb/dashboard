
FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /app

COPY .mvnw .
COPY .mvn/ .mvn
RUN chmod +x ./mvnw

COPY pom.xml ./
RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw package -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]