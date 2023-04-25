from pydantic import BaseSettings
from functools import lru_cache

'''
실행 환경(개발, 배포)에 따라 변수에 다른 값을 포함
'''

class Settings(BaseSettings):
    web_front_url: str
    env:str

    class Config:
        env_file = "settings.env"


@lru_cache()
def _settings() -> Settings:
    return Settings()


settings: Settings = _settings()
