FROM eclipse-temurin:21-jdk-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper 및 필요한 파일들을 복사
COPY gradle /app/gradle
COPY gradlew /app/
COPY build.gradle.kts /app/
COPY settings.gradle.kts /app/

# Gradle Wrapper 실행 권한 부여
RUN chmod +x ./gradlew

# Gradle 의존성 다운로드
RUN ./gradlew build --no-daemon

# 전체 소스 코드 복사
COPY src /app/src

# 애플리케이션 빌드
RUN ./gradlew bootJar --no-daemon  --exclude-task test

# Temurin JRE 이미지를 사용하여 더 작은 이미지를 기반으로 Production 환경 설정
FROM eclipse-temurin:21-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar /app/app.jar

# Spring Boot 애플리케이션 실행
CMD ["java", "-jar", "/app/app.jar"]