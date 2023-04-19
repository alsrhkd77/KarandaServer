from firebase_admin import firestore


class FirestoreProvider:
    def __init__(self):
        self.db = firestore.client()

    def get_discord_data(self):
        ref = self.db.collection(u'defaultData').document(u'discord').get()
        return ref.to_dict()['exchange_data']
