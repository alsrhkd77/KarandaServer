from fastapi import APIRouter
from app.api.auth.discord import router as discord_router

router = APIRouter(prefix='/auth')
router.include_router(discord_router)
