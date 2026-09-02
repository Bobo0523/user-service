FROM maven:3.8.5-openjdk-17 AS builder


WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline


COPY src ./src
RUN mvn clean package -DskipTests


FROM eclipse-temurin:17-jdk


WORKDIR /app

COPY --from=builder /build/target/User-1.0-SNAPSHOT.jar app.jar


EXPOSE 8080


ENTRYPOINT ["java", "-jar", "app.jar"]