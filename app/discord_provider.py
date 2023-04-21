import logging

import requests
from app.database.firestore_provider import firestore_provider


def exchange_code(code: str, redirect_url: str = 'https://karanda-server-6hf3d25tnq-an.a.run.app/auth/discord/authenticate/web'):
    source = firestore_provider.get_discord_data()
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
    if r.status_code != 200:
        return r.json()
    r.raise_for_status()
    authorization = r.json()
    return {
        'access_token': authorization['access_token'],
        'refresh_token': authorization['refresh_token']
    }


def get_user_data(token: str):
    headers = {
        'Authorization': f'Bearer {token}',
    }
    r = requests.get(f'https://discord.com/api/users/@me', headers=headers)
    print(r.json())
    r.raise_for_status()
    data = r.json()
    return data
