from fastapi import APIRouter
import requests

router = APIRouter(prefix='/chzzk')


@router.get('/get/live-status')
async def get_live_status():
    result = False
    headers = {
        'User-Agent': 'Mozilla/5.0'
    }
    url = "https://api.chzzk.naver.com/service/v1/channels/e28fd3efe38595427f8e51142c91b247"
    try:
        chzzk_response = requests.get(url, headers=headers)
        if chzzk_response.status_code == 200:
            data = chzzk_response.json()
            if data["content"]["openLive"]:
                result = True
    except Exception as e:
        print(e)
    finally:
        return {"live-status": result}
