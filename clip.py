# main.py  ─ FastAPI + MQTT + SBERT Emotion Server
# -----------------------------------------------
import json
import numpy as np
import paho.mqtt.publish as publish
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sentence_transformers import SentenceTransformer

# ───────────────────────────────
# MQTT設定
# ───────────────────────────────
MQTT_BROKER = "broker.hivemq.com"
MQTT_PORT   = 1883

TOPIC_CHAT_BROADCAST           = "chat/broadcast"
TOPIC_EMOTION_BROADCAST        = "emotion/broadcast"
TOPIC_TRIGGER_STT_BROADCAST    = "trigger_stt/broadcast"
TOPIC_TRIGGER_PONPON_BROADCAST = "trigger_ponpon/broadcast"
TOPIC_TRIGGER_NADENADE_BROADCAST = "trigger_nadenade/broadcast"

# ───────────────────────────────
# FastAPI設定
# ───────────────────────────────
app = FastAPI(title="Emotion Server")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ───────────────────────────────
# Sentence-BERTモデル準備
# ───────────────────────────────
model = SentenceTransformer("sonoisa/sentence-bert-base-ja-mean-tokens-v2")

# 感情ラベル（日本語→英語）
emotion_labels = [
    ("うれしい", "happy"),
    ("悲しい", "sad"),
    ("楽しい", "fun"),
    ("嫌だ",   "bad"),
]
# 事前にベクトル化
emotion_embeddings = model.encode([jp for jp, _ in emotion_labels])

def predict_emotion_json(text: str) -> dict:
    """
    入力文をSBERTでベクトル化し、4感情とのコサイン類似度を計算。
    もっとも近い感情を {emotion:<en>, level:<0-10>} で返す。
    """
    vec  = model.encode([text])[0]
    sims = [float(np.dot(vec, e) / (np.linalg.norm(vec) * np.linalg.norm(e))) for e in emotion_embeddings]

    idx          = int(np.argmax(sims))
    best_emotion = emotion_labels[idx][1]     # 英語ラベル
    level        = int(max(min(sims[idx] * 10, 10), 0))  # 0-10 整数化

    return {"emotion": best_emotion, "level": level}

# ───────────────────────────────
# エンドポイント
# ───────────────────────────────
@app.get("/")
def root():
    return {"status": "running"}

@app.post("/send_chat")
def send_chat(message: dict):
    """
    スマホ → HTTP
             └→ MQTT (chat/broadcast) へチャット本文
             └→ MQTT (emotion/broadcast) へ感情JSON
    """
    # ① チャットをそのままブロードキャスト
    publish.single(
        TOPIC_CHAT_BROADCAST,
        json.dumps(message, ensure_ascii=False),
        hostname=MQTT_BROKER,
        port=MQTT_PORT,
    )

    # ② 感情を推定してブロードキャスト
    text         = message.get("text", "")
    emotion_json = predict_emotion_json(text)

    publish.single(
        TOPIC_EMOTION_BROADCAST,
        json.dumps(emotion_json, ensure_ascii=False),
        hostname=MQTT_BROKER,
        port=MQTT_PORT,
    )

    return {"status": "sent", **emotion_json}

@app.post("/send_trigger")
def send_trigger(trigger: dict):
    """
    スマホ → HTTP で任意トリガーを送信
    trigger: {"type": "nadenade" | "ponpon" | "stt", ...}
    """
    ttype = trigger.get("type")

    if ttype == "nadenade":
        topic, msg = TOPIC_TRIGGER_NADENADE_BROADCAST, {"command": "tts"}

    elif ttype == "ponpon":
        topic, msg = TOPIC_TRIGGER_PONPON_BROADCAST, {"emotion": "happy", "level": 10}

    elif ttype == "stt":
        topic, msg = TOPIC_TRIGGER_STT_BROADCAST, {"emotion": "happy", "level": 10}

    else:
        return {"status": "error", "message": "unknown trigger type"}

    publish.single(topic, json.dumps(msg), hostname=MQTT_BROKER, port=MQTT_PORT)
    return {"status": "trigger sent"}
