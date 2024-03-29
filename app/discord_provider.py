import requests
from app.database.firestore_provider import firestore_provider


class DiscordProvider:
    def __init__(self):
        self.source = firestore_provider.get_discord_data()
        self.headers = {
            'Content-Type': 'application/x-www-form-urlencoded'
        }

    def exchange_code(self, code: str, redirect_url: str):
        data = {
            'client_id': self.source['CLIENT_ID'],
            'client_secret': self.source['CLIENT_SECRET'],
            'grant_type': 'authorization_code',
            'code': code,
            # 'scope': 'identify email',
            'scope': 'identify',
            'redirect_uri': redirect_url
        }
        r = requests.post('%s/oauth2/token' % self.source['API_ENDPOINT'], data=data, headers=self.headers)
        if r.status_code != 200:
            return r.json()
        r.raise_for_status()
        body = r.json()
        return {
            'access_token': body['access_token'],
            'refresh_token': body['refresh_token']
        }

    '''
    def refresh_token(self, token):
        data = {
            'client_id': self.source['CLIENT_ID'],
            'client_secret': self.source['CLIENT_SECRET'],
            'grant_type': 'refresh_token',
            'scope': 'identify',
            'refresh_token': token,
        }
        headers = {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
        r = requests.post('%s/oauth2/token' % self.source['API_ENDPOINT'], data=data, headers=self.headers)
        if r.status_code != 200:
            return r.json()
        r.raise_for_status()
        body = r.json()
        return {
            'access_token': body['access_token'],
            'refresh_token': body['refresh_token']
        }
    '''
    def get_user_data(self, token: str):
        headers = {
            'Authorization': f'Bearer {token}',
        }
        r = requests.get(f'https://discord.com/api/users/@me', headers=headers)
        r.raise_for_status()
        data = r.json()
        return data

    def get_user_data_with_id(self, discord_id: str):
        headers = {
            'Authorization': f'Bot {self.source["TOKEN"]}',
        }
        r = requests.get(f'https://discord.com/api/users/{discord_id}', headers=headers)
        r.raise_for_status()
        data = r.json()
        return data


discord_provider = DiscordProvider()
