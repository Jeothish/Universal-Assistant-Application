# backend/main.py
import sys

import whisper
from fastapi import FastAPI, Query, UploadFile, File, HTTPException, Depends, Form
from fastapi.responses import JSONResponse
import tempfile
import os
from whispertest import handle_prompt
from collections import deque
import numpy as np
from pydantic import BaseModel
from typing import List, Optional
import tensorflow as tf
from tensorflow.keras.models import Sequential
from db import add_reminders_db, get_reminders_db, edit_reminders_db, delete_reminders_db
import psycopg2
from InputProcessing import handle_prompt_with_qwen
from datetime import time

def get_connection():
    # connect to database
    try:
        connection = psycopg2.connect(
            os.getenv("DATABASE_URL")
        )
        print("Connection successful!")
        return connection

    except Exception as e:
        print(f"Failed to connect: {e}")
        return None


connection = get_connection()
app = FastAPI(title="App", description="Assistant app")


class SignRequest(BaseModel):
    frames: List[List[float]]  # 30x126 array


class TextRequest(BaseModel):
    text: str
    time: str
    city: str


class ReminderCreate(BaseModel):
    reminder_title: str
    reminder_date: str
    reminder_description: Optional[str] = None
    is_complete: bool = False
    recurrence_type: Optional[str] = None
    reminder_time: time



class ReminderGet(BaseModel):
    reminder_id: int
    reminder_title: Optional[str] = None
    reminder_date: str
    reminder_description: Optional[str] = None
    is_complete: Optional[bool] = None
    recurrence_type: Optional[str] = None
    reminder_time: time

class ReminderEdit(BaseModel):
    reminder_title: Optional[str] = None
    reminder_date: Optional[str] = None
    reminder_description: Optional[str] = None
    is_complete: Optional[bool] = None
    recurrence_type: Optional[str] = None
    reminder_time: time

model = whisper.load_model("small")


@app.post("/voice")
async def voice(audio: UploadFile = File(...), timestamp: str = Form(None), city: str = Form(None)):
    print("Time: "+timestamp)
    print("City: "+city)
    if audio.content_type not in [
        "audio/wav",
        "audio/x-wave",
        "audio/mp4",
        "audio/aac",
        "audio/m4a",
        "application/octet-stream",
    ]:
        raise HTTPException(status_code=400, detail="Invalid audio file")
    with tempfile.NamedTemporaryFile(delete=False, suffix="..m4a") as tmp:
        tmp.write(await audio.read())
        path = tmp.name

    try:
        result = model.transcribe(path)
        raw_prompt = str(result["text"]).lower()

        if not raw_prompt.strip():
            print("no speech detected")
            return
        connection = None
        response = handle_prompt_with_qwen(raw_prompt,city,connection, timestamp)

        print(response)
        return JSONResponse(content=response)
    finally:
        os.remove(path)  # delete audio file


@app.post("/text")
async def text(req: TextRequest):
    inp = req.text.strip().lower()
    time = req.time.strip()
    city = req.city.strip()
    print("City: "+city)
    print("text in:" + inp)
    if not inp:
        return
    else:
        connection = None
        response = handle_prompt_with_qwen(inp,city,connection,time, )
        return JSONResponse(content=response)


class SingleFrameRequest(BaseModel):
    features: List[float]
    hand: str


modelASLL = tf.keras.models.load_model("asl_mediapipe_model.keras")
modelASLR = tf.keras.models.load_model("asl_mediapipe_model_custom_og.keras")


labelsR = np.load("asl_labels.npy", allow_pickle=True)
labelsL = np.load("asl_labels_og_retrain.npy", allow_pickle=True)

pred_queueL = deque(maxlen=10)
pred_queueR = deque(maxlen=10)
pred_queue = deque(maxlen=10)
SEQUENCE_LENGTH = 30
FEATURES_PER_FRAME = 63


def normalize_frame(frame_63):  # need this as differnt hand / cameras sizes have differnet coords
    pts = np.array(frame_63).reshape(21, 3)

    # wrist normalize
    pts -= pts[0]

    # scale normalization
    scale = np.linalg.norm(pts[9])
    if scale > 0:
        pts /= scale

    return pts.flatten()


@app.post("/predict")
async def predict(data: SingleFrameRequest):
    try:

        if len(data.features) != FEATURES_PER_FRAME:
            raise HTTPException(
                status_code=400,
                detail=f"Expected {FEATURES_PER_FRAME} features, got {len(data.features)}"
            )

        normalized = normalize_frame(data.features)

        features = normalized.reshape(1, -1)  # (1,63) model inpt shape

        if data.hand == "Right":
            pred = modelASLL.predict(features, verbose=0) #media pipe inverts hands so mp right = actual left
            pred_queue = pred_queueL
            labels = labelsL
            print("left hand")
        else:
            pred = modelASLR.predict(features, verbose=0)
            pred_queue = pred_queueR
            labels = labelsR
            print("right hand")

        idx = int(np.argmax(pred))
        conf = float(np.max(pred))

        letter = ""
        if conf > 0.7:
            pred_queue.append(idx)

        # input smooting to prevent glitching
        if len(pred_queue) > 0:
            final_idx = max(set(pred_queue), key=pred_queue.count)
            letter = str(labels[final_idx])

        print(f"Pred: {letter} (confidence: {conf:.2f})")


        return JSONResponse(content={
            "letter": letter,
            "confidence": conf
        })

    except Exception as e:
        print(f"Prediction error: {e}")
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/echo_asl")
async def echo_asl(req: TextRequest):
    text = req.text.lower()
    tokens = ["REST"]

    for char in text:
        if char.isalpha():
            tokens.append(char.upper())

    tokens.append("REST")

    return {"tokens": tokens}


@app.get("/reminders/get", response_model=list[ReminderGet])
async def get_reminder(
    reminder_title: str | None = Query(None),
    reminder_date: str | None = Query(None),
    reminder_description: str | None = Query(None),
    is_complete: bool | None = Query(None),
    recurrence_type: str | None = Query(None),
    reminder_time: str | None = Query(None),
):
    rows = get_reminders_db(
        connection = get_connection(),
        reminder_title=reminder_title,
        reminder_date=reminder_date,
        reminder_description=reminder_description,
        is_complete=is_complete,
        recurrence_type=recurrence_type,
        reminder_time=reminder_time
    )
    
    return [
        ReminderGet(
            reminder_id = r[0],
            reminder_title=r[1],
            reminder_date=str(r[2]),
            reminder_description=r[3],
            is_complete=r[4],
            recurrence_type=r[5],
            reminder_time=r[6]
        )
        for r in rows
    ]

@app.post("/reminders/add")
async def add_reminder(reminder: ReminderCreate):
    try:
        connection = get_connection()
        add_reminders_db(connection,
                         reminder.reminder_title,
                         reminder.reminder_date,
                         reminder.reminder_description,
                         reminder.is_complete,
                         reminder.recurrence_type,
                         reminder.reminder_time)
        return JSONResponse({"message": "Reminder added successfully!"})
    except Exception as e:
        return JSONResponse({"error": str(e)}, status_code=500)


@app.delete("/reminders/delete/{reminder_id}")
def delete_reminder(reminder_id: int):
    try:
        connection = get_connection()
        delete_reminders_db(reminder_id, connection)
        return JSONResponse({"message": "Reminder deleted successfully!"})
    except Exception as e:
        return JSONResponse({"error": str(e)}, status_code=500)


@app.patch("/reminders/edit/{reminder_id}")
def edit_reminder(reminder_id: int, reminder: ReminderEdit):
    try:
        connection = get_connection()
        edit_reminders_db(connection,
                          reminder_id,
                          reminder.reminder_title,
                          reminder.reminder_date,
                          reminder.reminder_description,
                          reminder.is_complete,
                          reminder.recurrence_type,
                          reminder.reminder_time)
        return JSONResponse({"message": "Reminder edited successfully!"})
    except Exception as e:
        return JSONResponse({"error": str(e)}, status_code=500)

# uvicorn main:app --host 0.0.0.0 --port 8000
# Ensure in backend directory
# source venv/bin/activate
# python -m uvicorn main:app --host 0.0.0.0 --port 8000
# uvicorn main:app --host 0.0.0.0 --port 8000


#TODO                                       highest priority
# Azure / Pi
# caching
# rate limiting
# Streaming output (chat)
# TTS
# llm humanize weather info
# fix news sources functiongemma
# pass in user location in front end prompt
# Universal ASL
# bigger model for chat (api??)
# double pressing vc crash (double pressing buttons in genreal)
# news handle unsupported domains (done for rte)
#                                           lowest priority