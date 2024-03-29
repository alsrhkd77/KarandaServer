import copy
import itertools
import json
from datetime import datetime, timedelta, timezone
from typing import Union, List

from fastapi import APIRouter, Request, HTTPException, BackgroundTasks
from fastapi.encoders import jsonable_encoder
from fastapi.params import Query
from starlette import status

from starlette.websockets import WebSocket, WebSocketDisconnect

from app.database.session import SessionLocal
from app.schemas.bdo_item import BdoItem
from app.schemas.market_data import MarketDataCreate, MarketDataUpdate, MarketDataResponse, MarketData
from app.trade_market_provider import trade_market_provider
from app.crud.crud_market_data import crud_market_data
from app.crud.crud_bdo_item import crud_bdo_item
from app.utils.websocket_manager import trade_market_websocket_manager

router = APIRouter(prefix='/trade-market')

wait_list_last_update: datetime = None
wait_item_list = []

'''
강화레벨이 0이 아닌것들은 무조건 한번에 업데이트
'''


async def check_wait_list() -> None:
    global wait_list_last_update, wait_item_list
    if wait_list_last_update is None or wait_list_last_update < datetime.now() - timedelta(seconds=90):
        wait_item_list = trade_market_provider.wait_list()
        wait_list_last_update = datetime.now()
        if wait_item_list is not None:
            await trade_market_websocket_manager.broadcast(json.dumps(jsonable_encoder(wait_item_list)))
    return


@router.websocket('/wait-list')
async def wait_list(websocket: WebSocket):
    global wait_list_last_update
    await trade_market_websocket_manager.accept(websocket)
    if wait_list_last_update is None or wait_list_last_update < datetime.now() - timedelta(seconds=90):
        await check_wait_list()
    else:
        await websocket.send_text(json.dumps(jsonable_encoder(wait_item_list)))
    try:
        while True:
            data = await websocket.receive_text()
            if data == 'update':
                await check_wait_list()
    except WebSocketDisconnect:
        trade_market_websocket_manager.disconnect(websocket)
    return


@router.get('/get/detail/{item_code}', response_model=List[MarketDataResponse])
def detail(request: Request, background_tasks: BackgroundTasks, item_code: int):
    db = request.state.db
    data = list(map(market_data_model_to_schema, crud_market_data.get_all_by_item_num(db=db, item_num=item_code)))
    now = (datetime.now(timezone.utc) + timedelta(hours=9)).replace(tzinfo=None)
    now_date = datetime.combine(now, datetime.min.time())

    create = []
    update = []

    # Initialize if no data exists
    if data is None or len(data) == 0:
        item_data = crud_bdo_item.get_tradeable_item_by_item_num(db=db, item_num=item_code)
        if item_data is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND)
        data = initialize_price_data(item_info=item_data, now=now, now_date=now_date)
        crud_market_data.create_from_list(db=db, data=data)
    else:
        grouped_data = {}
        date_list = []

        # Group by datetime
        for k, v in itertools.groupby(data, lambda x: x.date):
            grouped_data[k] = list(v)
            grouped_data[k].sort(key=lambda x: x.enhancement_level)
            date_list.append(k)
        date_list = date_list[:90] if len(date_list) > 90 else date_list
        date_list.sort(reverse=True)

        if date_list[0].date() != now.date():
            # Create today data
            if len(grouped_data[date_list[0]]) == 1:
                realtime_data = trade_market_provider.search_list(item_list=[item_code])
            else:
                realtime_data = trade_market_provider.sub_list(main_key=item_code)
            for r in realtime_data:
                new_data = MarketDataCreate(item_num=r.item_num, enhancement_level=r.enhancement_level, price=r.price,
                                            cumulative_volume=r.cumulative_volume, current_stock=r.current_stock,
                                            date=now)
                create.append(new_data)

        # Update today data
        elif date_list[0] < now - timedelta(minutes=15):
            if len(grouped_data[date_list[0]]) == 1:
                realtime_data = trade_market_provider.search_list(item_list=[item_code])
            else:
                realtime_data = trade_market_provider.sub_list(main_key=item_code)
            for index in range(len(realtime_data)):
                update_data = MarketDataUpdate(**jsonable_encoder(grouped_data[date_list[0]][index]))
                update_data.update(data=realtime_data[index], date=now)
                update.append(update_data)
                data.remove(grouped_data[date_list[0]][index])
            del date_list[0]

        # Update Price Data
        if date_list[0].date() != (now - timedelta(days=1)).date() or date_list[0] > now_date - timedelta(days=1):
            # Get price data
            price_data = []
            for item in grouped_data[date_list[0]]:
                price_data.append(
                    trade_market_provider.price_info(main_key=item.item_num, sub_key=item.enhancement_level)[1:])

            date_without_time = list(map(lambda x: x.date(), date_list))
            for day in range(len(price_data[0])):
                # Fill non-existent price data
                if (now_date - timedelta(days=day + 1)).date() not in date_without_time:
                    for index in range(len(grouped_data[date_list[0]])):
                        item = grouped_data[date_list[0]][index]
                        create.append(
                            MarketDataCreate(item_num=item.item_num, enhancement_level=item.enhancement_level,
                                             price=price_data[index][day], cumulative_volume=0,
                                             current_stock=0, date=now_date - timedelta(days=day + 1)))
                # Update past days 
                else:
                    index: int = date_without_time.index((now_date - timedelta(days=day + 1)).date())
                    if date_list[index] > now_date - timedelta(days=day + 1):
                        for enhance_level in range(len(grouped_data[date_list[index]])):
                            update_data = MarketDataUpdate(
                                **jsonable_encoder(grouped_data[date_list[index]][enhance_level]))
                            update_data.price = price_data[enhance_level][index]
                            update.append(update_data)
                            data.remove(grouped_data[date_list[index]][enhance_level])

        # Create and update to DB
        if create:
            # crud_market_data.create_from_list(db=db, data=create)
            background_tasks.add_task(create_trade_market_data, data=create)
            create = list(map(market_data_to_market_data_response, create))
        if update:
            # crud_market_data.update_from_list(db=db, data=update)
            background_tasks.add_task(update_trade_market_data, data=update)
            update = list(map(market_data_to_market_data_response, update))

        data = list(map(market_data_to_market_data_response, data)) + create + update
        data.sort(key=lambda x: x.enhancement_level)

    return data


def initialize_price_data(item_info: BdoItem, now: datetime, now_date: datetime):
    result = []
    realtime_data = trade_market_provider.sub_list(main_key=item_info.item_num)
    if not realtime_data:
        realtime_data = trade_market_provider.search_list([item_info.item_num])
    for item in realtime_data:
        price_data = trade_market_provider.price_info(main_key=item.item_num, sub_key=item.enhancement_level)
        for i in range(len(price_data)):
            if i == 0:
                result.append(
                    MarketDataCreate(item_num=item.item_num, enhancement_level=item.enhancement_level, price=item.price,
                                     cumulative_volume=item.cumulative_volume, current_stock=item.current_stock,
                                     date=now))
            else:
                result.append(MarketDataCreate(item_num=item.item_num, enhancement_level=item.enhancement_level,
                                               price=price_data[i], cumulative_volume=0, current_stock=0,
                                               date=now_date - timedelta(days=i)))
    return result


def market_data_to_market_data_response(data: Union[MarketData, MarketDataCreate, MarketDataUpdate]):
    return MarketDataResponse(
        item_num=data.item_num,
        enhancement_level=data.enhancement_level,
        price=data.price,
        cumulative_volume=data.cumulative_volume,
        current_stock=data.current_stock,
        date=data.date,
    )


def market_data_model_to_schema(data):
    return MarketData.from_orm(data)


@router.get('/get/latest', response_model=list[MarketDataResponse])
def get_latest(request: Request, background_tasks: BackgroundTasks, target_list: list[str] = Query(None)):
    """
    ## Get latest data
    최신(15분 이내) 거래소 데이터를 반환\n
    DB에 목표 아이템의 데이터가 없거나 오래된(15분 이상) 데이터일 경우 api에서 새 데이터를 가져와서 업데이트 후 반환\n
    (날짜가 바뀌었을 경우에도 새 데이터를 가져옴)

    - **target_list**: 리스트 내 아이템은 "item code_enhancement level" 형식으로 구성해야 함
    """
    # Check empty list and max length
    if target_list is None or target_list == [] or len(target_list) > 100:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST)
    db = request.state.db
    target = []
    item_num_list = []
    # Parameter formatting (str -> dict)
    for t in target_list:
        item_num, enhancement_level = t.split("_")
        # Check negative number
        if int(item_num) < 0 or int(enhancement_level) < 0:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST)
        target.append((int(item_num), int(enhancement_level)))
        item_num_list.append(int(item_num))
    # Deduplication
    item_num_list = list(set(item_num_list))
    bdo_item = crud_bdo_item.get_all_tradeable_item_by_item_num(db=db, item_num_list=item_num_list)
    # Check tradeable items
    if len(bdo_item) != len(item_num_list):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST)
    target_item_info = {b.item_num: b for b in bdo_item}
    # Check enhancement level is within the maximum
    for item in target:
        if target_item_info[item[0]].max_enhancement_level < item[1]:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST)
    db_data = crud_market_data.get_today_by_item_nums(db=db, item_nums=item_num_list)
    now = (datetime.now(timezone.utc) + timedelta(hours=9)).replace(tzinfo=None)  # KR
    need_update = []
    # Categorize items that need to be updated
    for key, v in itertools.groupby(db_data, key=lambda x: x.item_num):
        item_num_list.remove(key)
        value = list(v)
        if value[0].date < now - timedelta(minutes=15):
            need_update = need_update + value
    for item in need_update:
        db_data.remove(item)
    if item_num_list or need_update:
        # Get latest data from BDO trade market
        create, update = get_latest_from_trade_market(need_create=item_num_list, need_update=need_update,
                                                      item_info=target_item_info, now=now)
        update = list(update)
        # Insert and update to DB
        if create:
            background_tasks.add_task(create_trade_market_data, data=create)
            # crud_market_data.create_from_list(db=db, data=create)
            for item in create:
                db_data.append(MarketDataResponse(**item.dict()))
        if update:
            background_tasks.add_task(update_trade_market_data, data=update)
            # crud_market_data.update_from_list(db=db, data=update)
            db_data = db_data + update
    result = []
    for (item_num, enhancement_level), value in itertools.groupby(db_data, lambda x: (x.item_num, x.enhancement_level)):
        if (int(item_num), int(enhancement_level)) in target:
            result = result + list(value)
    result = list(map(market_data_to_market_data_response, result))
    return result


def get_latest_from_trade_market(need_create: list, need_update: list[MarketData], item_info: dict, now: datetime):
    need_update_item = {}
    need_create_item = []
    for item in need_update:
        need_update_item[(int(item.item_num), int(item.enhancement_level))] = MarketDataUpdate(**jsonable_encoder(item))
    search_list = [i.item_num for i in need_update]
    search_list = search_list + need_create
    search_list = list(set(search_list))

    for search in copy.deepcopy(search_list):
        if item_info[search].max_enhancement_level == 0:
            continue
        item_data = trade_market_provider.sub_list(main_key=search)
        if item_data:
            if search in need_create:
                for data in item_data:
                    need_create_item.append(MarketDataCreate(**data.dict(), date=now))
            else:
                for data in item_data:
                    need_update_item[(data.item_num, data.enhancement_level)].update(data=data, date=now)
            search_list.remove(search)
    if not search_list:
        return need_create_item, need_update_item.values()
    item_data = trade_market_provider.search_list(item_list=search_list)
    if item_data:
        for data in item_data:
            if data.item_num in need_create:
                need_create_item.append(MarketDataCreate(**data.dict(), date=now))
            else:
                need_update_item[(data.item_num, data.enhancement_level)].update(data=data, date=now)
    return need_create_item, need_update_item.values()


async def update_trade_market_data(data: list[MarketDataUpdate]):
    with SessionLocal() as db:
        crud_market_data.update_from_list(db=db, data=data)


async def create_trade_market_data(data: list[MarketDataCreate]):
    with SessionLocal() as db:
        crud_market_data.create_from_list(db=db, data=data)
