# KarandaServer

[Karanda](https://www.karanda.kr) Back-end\
Spring Boot를 사용하였으며, Kotlin으로 작성

## 환경

### 공통
 - OpenJDK Temurin 21.0.4+7-LTS
 - Spring boot 3.3.2
 - Firestore

### 개발
 - Windows 11
 - h2DB
 - IntelliJ IDEA Ultimate

### 배포
 - Cloud Build (GCP)
 - Cloud Run (GCP)
 - MariaDB (on GCP Compute Engine)

## 참고
개발 환경 실행 시 필요 요소
 - `develop` 프로필 사용
 - `src`폴더와 동일한 레벨에 `localDB` 폴더 필요
 - 프로젝트 `./credentials/`에 `.json`형식 파이어베이스 토큰 필요
 - 테스트 시 아래 환경 변수 필요
```
--spring.profiles.active=develop
```