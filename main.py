import json
import numpy as np
import paho.mqtt.publish as publish
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sentence_transformers import SentenceTransformer


MQTT_BROKER = "broker.hivemq.com"
MQTT_PORT   = 1883

TOPIC_CHAT_BROADCAST           = "chat/broadcast"
TOPIC_EMOTION_BROADCAST        = "emotion/broadcast"
TOPIC_TRIGGER_STT_BROADCAST    = "trigger_stt/broadcast"
TOPIC_TRIGGER_PONPON_BROADCAST = "trigger_ponpon/broadcast"
TOPIC_TRIGGER_NADENADE_BROADCAST = "trigger_nadenade/broadcast"


app = FastAPI(title="Emotion Server")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


model = SentenceTransformer("sonoisa/sentence-bert-base-ja-mean-tokens-v2")

emotion_labels = [
    ("うれしい", "happy"),
    ("悲しい", "sad"),
    ("楽しい", "fun"),
    ("嫌だ",   "bad"),
]

emotion_embeddings = model.encode([jp for jp, _ in emotion_labels])

def predict_emotion_json(text: str) -> dict:

    vec  = model.encode([text])[0]
    sims = [float(np.dot(vec, e) / (np.linalg.norm(vec) * np.linalg.norm(e))) for e in emotion_embeddings]

    idx          = int(np.argmax(sims))
    best_emotion = emotion_labels[idx][1]    
    level        = int(max(min(sims[idx] * 10, 10), 0))  

    return {"emotion": best_emotion, "level": level}


@app.get("/")
def root():
    return {"status": "running"}

@app.post("/send_chat")
def send_chat(message: dict):


    publish.single(
        TOPIC_CHAT_BROADCAST,
        json.dumps(message, ensure_ascii=False),
        hostname=MQTT_BROKER,
        port=MQTT_PORT,
    )

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

    ttype = trigger.get("type")

    if ttype == "nadenade":
        topic, msg = TOPIC_TRIGGER_NADENADE_BROADCAST, {"command": "tts"}

    elif ttype == "ponpon":
        topic, msg = TOPIC_TRIGGER_PONPON_BROADCAST, {"emotion": "happy", "level": 10}

        publish.single(TOPIC_EMOTION_BROADCAST,
                    json.dumps(msg, ensure_ascii=False),
                    hostname=MQTT_BROKER, 
                    port=MQTT_PORT)




    elif ttype == "stt":
        # 音声認識テキストを感情推定
        text = trigger.get("text", "")
        msg  = predict_emotion_json(text)         
        topic = TOPIC_TRIGGER_STT_BROADCAST

        publish.single(
            TOPIC_EMOTION_BROADCAST,
            json.dumps(msg, ensure_ascii=False),
            hostname=MQTT_BROKER,
            port=MQTT_PORT,
        )


    else:
        return {"status": "error", "message": "unknown trigger type"}

    publish.single(topic, json.dumps(msg, ensure_ascii=False),
                   hostname=MQTT_BROKER, port=MQTT_PORT)
    return {"status": "trigger sent", **msg}