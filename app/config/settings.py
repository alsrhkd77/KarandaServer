from pydantic import BaseSettings

'''
실행 환경(개발, 배포)에 따라 변수에 다른 값을 포함
'''


class Settings(BaseSettings):
    web_front_url: str = "https://www.karanda.kr"
    env: str = "deploy"


settings = Settings()
