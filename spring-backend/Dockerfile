# ビルドの context はリポジトリ直下（pom.xml が ../schema を取り込むため）
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

COPY spring-backend/.mvn/ .mvn/
COPY spring-backend/mvnw spring-backend/pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY schema /schema
COPY spring-backend/src ./src
RUN ./mvnw clean package -Dmaven.test.skip=true

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
