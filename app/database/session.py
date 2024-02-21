from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.config.settings import settings
from app.database.firestore_provider import firestore_provider

properties = firestore_provider.get_sql_server_settings()
SQLALCHEMY_ENGINE_OPTIONS = {
        'pool_recycle': 280,
        'pool_pre_ping': True
    }
if settings.env == 'dev':
    SQLALCHEMY_DATABASE_URL = "sqlite:///./karanda_dev.db"
    engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}, echo=True)
    #engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
else:
    engine = create_engine(
        f'{properties["connector"]}://{properties["user"]}:{properties["pwd"]}@{properties["base_url"]}/{properties["database"]}',
        **SQLALCHEMY_ENGINE_OPTIONS, echo=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
