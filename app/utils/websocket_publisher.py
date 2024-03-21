import asyncio
import json
import threading

from fastapi.encoders import jsonable_encoder

from app.api.maretta import latest_report
from app.utils.websocket_manager import maretta_websocket_manager

lock = threading.Lock()


def maretta_publisher():
    while True:
        report = latest_report.get()
        reports = [report]
        lock.acquire()
        asyncio.run(maretta_websocket_manager.broadcast(json.dumps(jsonable_encoder(reports))))
        lock.release()
