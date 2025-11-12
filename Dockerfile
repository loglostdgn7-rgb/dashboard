# ---------------------------------------------------
# 빌드 스테이지 (의존성 캐시 최적화 적용)
# ---------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /app

# 빌드에 필요한 파일만 먼저 복사
COPY mvnw .
COPY .mvn/ .mvn
COPY pom.xml ./

# 실행 권한 부여
RUN chmod +x ./mvnw

#  pom.xml을 기반으로 의존성만 먼저 다운로드 (이 레이어가 캐시됩니다)
RUN ./mvnw dependency:go-offline

# 소스 코드 복사
# (소스 코드가 바뀌면 여기부터 빌드를 다시 시작합니다)
COPY src ./src

# 애플리케이션 빌드
RUN ./mvnw package -DskipTests

# ---------------------------------------------------
#  실행 스테이지
# ---------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일만 복사
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]