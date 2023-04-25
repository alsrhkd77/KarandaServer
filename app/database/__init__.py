import firebase_admin
from firebase_admin import credentials
from app.config.settings import settings

app_options = {'projectId': 'karanda-384102'}

if settings.env == "dev":
    cred = credentials.Certificate("./OAuth_id/karanda-384102-firebase-adminsdk-i8vxk-6a4c6730c0.json")
else:
    cred = credentials.ApplicationDefault()
default_app = firebase_admin.initialize_app(options=app_options, credential=cred)
