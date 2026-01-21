# backend/main.py
import sys

import whisper
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
import tempfile
import os
from whispertest import handle_prompt

import numpy as np
from pydantic import BaseModel
from typing import List
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense
import psycopg2

def get_connection():

    USER = os.getenv("user")
    PASSWORD = os.getenv("password")
    HOST = os.getenv("host")
    PORT = os.getenv("port")
    DBNAME = os.getenv("dbname")
    print(f"Connecting with USER={USER}, PASSWORD={'*' * len(PASSWORD)}, HOST={HOST}, PORT={PORT}, DBNAME={DBNAME}")

    # Connect to the database
    try:
        connection = psycopg2.connect(
            user=USER,
            password=PASSWORD,
            host=HOST,
            port=PORT,
            dbname=DBNAME,
            sslmode = "require"
        )
        print("Connection successful!")
        return connection

    except Exception as e:
        print(f"Failed to connect: {e}")
        return None
connection = get_connection()
app = FastAPI(title="App",description="Assistant app")

class SignRequest(BaseModel):
    frames: List[List[float]]  # 30x126 array

class TextRequest(BaseModel):
    text: str

model = whisper.load_model("small")

@app.post("/voice")
async def voice (audio: UploadFile = File(...)):
    if audio.content_type not in [
        "audio/wav",
        "audio/x-wave",
        "audio/mp4",
        "audio/aac",
        "audio/m4a",
        "application/octet-stream",
    ]:
        raise HTTPException(status_code=400, detail="Invalid audio file")
    with tempfile.NamedTemporaryFile(delete = False, suffix = "..m4a") as tmp:
        tmp.write(await audio.read())
        path = tmp.name

    try:
        result = model.transcribe(path)
        raw_prompt = str(result["text"]).lower()

        if not raw_prompt.strip() :
            print("no speech detected")
            return
        response = handle_prompt(connection,raw_prompt)
        print(response)
        return JSONResponse(content=response)
    finally:
        os.remove(path) #delete audio file

@app.post("/text")
async def text(req: TextRequest):
    inp = req.text.strip().lower()
    print("text in:"+inp)
    if not inp:
        return
    else:
        response = handle_prompt(connection,inp)
        return JSONResponse(content=response)

actions = np.array(['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'r', 's', 't', 'u', 'w', 'y', 'z'])

def load_model():
    model = Sequential()
    model.add(LSTM(64, return_sequences=True, activation='relu', input_shape=(30, 126)))
    model.add(LSTM(128, return_sequences=True, activation='relu'))
    model.add(LSTM(64, return_sequences=False, activation='relu'))
    model.add(Dense(64, activation='relu'))
    model.add(Dense(32, activation='relu'))
    model.add(Dense(actions.shape[0], activation='softmax'))
    model.load_weights(r"C:\Users\HP\PycharmProjects\3rd-Year-Project\backend\action.h5")

    return model

modelASl = load_model()


def predict(seq, model):
    sequence = np.array(seq)


    sentence = []
    predictions = []
    threshold = 0.2
    if sequence.shape != (30, 126):
        raise ValueError(f"Expected shape (30, 126), got {sequence.shape}")
    res = model.predict(tf.expand_dims(sequence, axis=0))[0]

    predictions.append(np.argmax(res))

    if np.unique(predictions[-10:])[0] == np.argmax(res):
        if (max(res) >= threshold):
            if len(sentence) > 0:
                if actions[np.argmax(res)] != sentence[-1]:
                    sentence.append(actions[np.argmax(res)])
            else:
                sentence.append(actions[np.argmax(res)])
            letter=actions[np.argmax(res)]
    print(f"Predicted letter: {letter} ")
    return {
        "letter": letter
        # "confidence": confidence
    }


@app.post("/sign")
async def recognize_sign(data: SignRequest):
    try:
        result = predict(data.frames,modelASl)
        return JSONResponse(content=result)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/echo_asl")
async def echo_asl(req: TextRequest):
    text = req.text.lower()
    tokens = ["REST"]
    
    for char in text:
        if char.isalpha():
            tokens.append(char.upper())
    
    tokens.append("REST")
    
    return {"tokens":tokens}

#uvicorn main:app --host 0.0.0.0 --port 8000


#TODO                                       highest priority
# ASL input
# reminders
# pass in user time/ location in front end prompt
# bigger chat model
# double pressing vc crash
# news unsupported domains (done for rte)
#                                           lowest priority
