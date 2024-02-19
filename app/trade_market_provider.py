from datetime import datetime

import requests

from app.database.firestore_provider import firestore_provider
from app.schemas.trade_market_item import MarketWaitItem, TradeMarketItem


class TradeMarketProvider:
    def __init__(self):
        source = firestore_provider.get_trade_market_data()
        self.api = source['api']
        self.headers = source['headers']
        self.keyType = source['keyType']

    def wait_list(self) -> list[MarketWaitItem]:
        url = f'{self.api}/GetWorldMarketWaitList'
        payload = {}
        result = []
        try:
            response = requests.post(url, json=payload, headers=self.headers)
            if response.status_code == 200:
                body = response.json()
                if 'resultMsg' in body:
                    body['resultMsg'] = body['resultMsg'].replace("|", " ").rstrip()
                    for line in body['resultMsg'].split(" "):
                        item = line.split('-')
                        item_num = int(item[0])
                        enhancement_level = int(item[1])
                        price = int(item[2])
                        target_time = datetime.fromtimestamp(int(item[3]))
                        result.append(
                            MarketWaitItem(item_num=item_num, enhancement_level=enhancement_level, price=price,
                                           target_time=target_time))
        except Exception as e:
            print(e)
            print("Official server is not available")
        finally:
            return result

    def search_list(self, item_list: list) -> list[TradeMarketItem]:
        url = f'{self.api}/GetWorldMarketSearchList'
        if item_list is None:
            raise Exception('Item list is empty')
        payload = {
            "keyType": self.keyType,
            "searchResult": ", ".join(str(x) for x in item_list)
        }
        result = []
        try:
            response = requests.post(url, json=payload, headers=self.headers)
            if response.status_code == 200:
                body = response.json()
                if 'resultMsg' in body:
                    if body['resultMsg'] != '0' and body['resultMsg'] != '':
                        body['resultMsg'] = body['resultMsg'].replace("|", " ").rstrip()
                        for line in body['resultMsg'].split(" "):
                            item = line.split('-')
                            item_num = int(item[0])
                            current_stock = int(item[1])
                            price = int(item[2])
                            cumulative_volume = int(item[3])
                            result.append(TradeMarketItem(item_num=item_num, enhancement_level=0, price=price,
                                                          current_stock=current_stock,
                                                          cumulative_volume=cumulative_volume))
        except Exception as e:
            print(e)
            print("Official server is not available")
        return result

    def price_info(self, main_key: int, sub_key: int = 0) -> list[str]:
        url = f'{self.api}/GetMarketPriceInfo'
        payload = {
            "keyType": self.keyType,
            "mainKey": str(main_key),
            "subKey": str(sub_key)
        }
        result = []
        try:
            response = requests.post(url, json=payload, headers=self.headers)
            if response.status_code == 200:
                body = response.json()
                if "resultMsg" in body:
                    result = body["resultMsg"].split('-')
                    result.reverse()    # 첫번째 값이 오늘이 되도록
        except Exception as e:
            print(e)
            print("Official server is not available")
        return result

    def sub_list(self, main_key: int) -> list[TradeMarketItem]:
        result = []
        url = f'{self.api}/GetWorldMarketSubList'
        payload = {
            "keyType": self.keyType,
            "mainKey": str(main_key)
        }
        try:
            response = requests.post(url, json=payload, headers=self.headers)
            if response.status_code == 200:
                body = response.json()
                if 'resultMsg' in body:
                    if body['resultMsg'] != '0' and body['resultMsg'] != '':
                        body['resultMsg'] = body['resultMsg'].replace("|", " ").rstrip()
                        for line in body['resultMsg'].split(" "):
                            item = line.split('-')
                            item_num = int(item[0])
                            enhancement_level = int(item[1])
                            price = int(item[3])
                            current_stock = int(item[4])
                            cumulative_volume = int(item[5])
                            # last_sale_price = int(item[8])
                            # last_sale_time = item[9]    # Timestamp
                            result.append(
                                TradeMarketItem(item_num=item_num, enhancement_level=enhancement_level, price=price,
                                                current_stock=current_stock, cumulative_volume=cumulative_volume))
        except Exception as e:
            print(e)
            print("Official server is not available")
        return result


trade_market_provider = TradeMarketProvider()
