# KarandaServer

## 환경

### 공통
 - Python 3.10.7
 - FastAPI
 - Firestore
 - Front: [Flutter (web & windows)](https://github.com/alsrhkd77/Karanda)

### 개발
 - Windows 11
 - SQLite
 - PyCharm (2023.1)

### 배포
 - Cloud Build (GCP)
 - Cloud Run (GCP)
 - MariaDB (on GCP Compute Engine)

## 참고

로컬 개발 환경 실행 시 Python 환경 변수로 아래 코드 넣어주면서 실행
```
env=dev;web_front_url=http://localhost:2345
```
> 프로젝트 `./OAuth_id/`에 `.json`형식 파이어베이스 토큰 필요
> 
> 관련 코드 `app/databse/__init__.py`
