# 1. 빌드를 위한 베이스 이미지 (Java 21) - eclipse-temurin 사용
FROM eclipse-temurin:21-jdk-alpine as builder

# 작업 디렉토리 설정
WORKDIR /app

COPY mvnw .
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src

# Maven으로 프로젝트 빌드 (의존성 다운로드 후 패키징)
RUN ./mvnw package -DskipTests

# 2. 실행을 위한 경량 이미지 - eclipse-temurin (JRE) 사용
FROM eclipse-temurin:21-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 단계에서 생성된 JAR 파일만 복사
COPY --from=builder /app/target/*.jar app.jar

# 8080 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]