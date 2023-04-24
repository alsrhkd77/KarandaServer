from firebase_admin import firestore


class FirestoreProvider:
    def __init__(self):
        self.db = firestore.client()

    def get_discord_data(self):
        ref = self.db.collection(u'defaultData').document(u'discord').get()
        exchange_data = ref.to_dict()['exchange_data']
        return exchange_data

    def get_sql_server_settings(self):
        ref = self.db.collection(u'defaultData').document(u'sql-machine').get()
        properties = ref.to_dict()['properties']
        return properties

    def get_token_settings(self):
        ref = self.db.collection(u'defaultData').document(u'token').get()
        properties = ref.to_dict()['properties']
        return properties


firestore_provider = FirestoreProvider()
