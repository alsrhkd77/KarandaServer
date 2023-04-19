import requests
from app.firestore_provider import FirestoreProvider


class DiscordProvider:
    def __init__(self):
        self.firestoreProvider = FirestoreProvider()

    def exchange_code(self, code: str, redirect_url: str = 'http://localhost:8000/auth/discord/authorize/'):
        source = self.firestoreProvider.get_discord_data()
        data = {
            'client_id': source['CLIENT_ID'],
            'client_secret': source['CLIENT_SECRET'],
            'grant_type': 'authorization_code',
            'code': code,
            'scope': 'identify email',
            'redirect_uri': redirect_url
        }
        headers = {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
        r = requests.post('%s/oauth2/token' % source['API_ENDPOINT'], data=data, headers=headers)
        r.raise_for_status()
        authorization = r.json()
        return {
            'access_token': authorization['access_token'],
            'refresh_token': authorization['refresh_token']
        }
