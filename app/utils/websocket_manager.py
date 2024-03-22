from starlette.websockets import WebSocket


class WebsocketManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []

    async def accept(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)

    async def send_message(self, message: str, websocket: WebSocket):
        try:
            await websocket.send_text(message)
        except RuntimeError:
            print("Error: sending to websocket")

    async def send_json(self, json_data: str, websocket: WebSocket):
        try:
            await websocket.send_json(json_data)
        except RuntimeError:
            print(f"Error: send json to websocket\n")

    async def broadcast(self, message: str):
        if self.active_connections:
            for connection in self.active_connections:
                await connection.send_text(message)


trade_market_websocket_manager = WebsocketManager()
maretta_websocket_manager = WebsocketManager()
