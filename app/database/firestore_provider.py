from firebase_admin import firestore

from app.schemas.maretta_status_report import MarettaStatusReportResponse


class FirestoreProvider:
    def __init__(self):
        self.db = firestore.client()
        self.watch_docs = []

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

    def get_trade_market_data(self):
        ref = self.db.collection(u'defaultData').document(u'trade-market').get()
        data = ref.to_dict()
        return data

    def update_maretta_status(self, data: MarettaStatusReportResponse):
        data.report_at = data.report_at.replace(tzinfo=None)
        ref = self.db.collection(u'synchronize-data').document(u'maretta-status')
        ref.update(data.dict())

    def watch_maretta_status(self, callback):
        ref = self.db.collection(u'synchronize-data').document(u'maretta-status')
        doc_watch = ref.on_snapshot(callback)
        self.watch_docs.append(doc_watch)

    def unsubscribe(self):
        for doc_watch in self.watch_docs:
            doc_watch.unsubscribe()


firestore_provider = FirestoreProvider()
