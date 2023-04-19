from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.database.firestore_provider import firestore_provider

properties = firestore_provider.get_sql_server_settings()
engine = create_engine(
    f'{properties["connector"]}://{properties["user"]}:{properties["pwd"]}@{properties["base_url"]}/{properties["database"]}')
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
