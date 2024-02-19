from starlette.websockets import WebSocket


class WebSocketManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)

    async def send_message(self, message: str, websocket: WebSocket):
        await websocket.send_text(message)

    async def send_json(self, json_data: str, websocket: WebSocket):
        await websocket.send_json(json_data)

    async def broadcast(self, message: str):
        for connection in self.active_connections:
            await connection.send_text(message)

    async def broadcast_json(self, message: str):
        for connection in self.active_connections:
            await connection.send_json(message)


trade_market_websocket_manager = WebSocketManager()
