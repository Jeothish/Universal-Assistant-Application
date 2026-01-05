# # test_api.py
# import requests
#
# API_URL = "http://127.0.0.1:8000/voice"
# AUDIO_FILE = "Recording.wav"  # path to a real wav file
#
# with open(AUDIO_FILE, "rb") as f:
#     files = {
#         "audio": ("Recording.wav", f, "audio/wav")
#     }
#
#     response = requests.post(API_URL, files=files)
#
# print("Status Code:", response.status_code)
#
# try:
#     print("Response JSON:")
#     print(response.json())
# except Exception:
#     print("Raw Response:")
#     print(response.text)
from SignLanguageRecognition import signLanguageRecognizer
signLanguageRecognizer.signLanguageRecognizerMethod()