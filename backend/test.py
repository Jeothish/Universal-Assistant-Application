import requests
#
# API_URL = "http://127.0.0.1:8000/text"
# # AUDIO_FILE = "Recording.wav"  # path to a real wav file
#
# # with open(AUDIO_FILE, "rb") as f:
# #     files = {
# #         "audio": ("Recording.wav", f, "audio/wav")
# #     }
# payload = {
#     "text": "what's the weather in galway"
# }
#
# response = requests.post(API_URL, json=payload)
#
# print("Status Code:", response.status_code)
#
# try:
#     print("Response JSON:")
#     print(response.json())
# except Exception:
#     print("Raw Response:")
#     print(response.text)
# from openai import OpenAI
#
# client = OpenAI(
#     base_url="http://localhost:1234/v1",
#     api_key="lm-studio"  # required but ignored
# )
#
# response = client.chat.completions.create(
#     model="local-model",
#     messages=[
#         {
#             "role": "system",
#             "content": "You are a weather JSON API. Output only valid JSON. make up the weather forecast, it does not have to be true"
#         },
#         {
#             "role": "user",
#             "content": "What's the weather in Dublin tomorrow?"
#         }
#     ],
#     response_format={
#         "type": "json_schema",
#         "json_schema": {
#             "name": "intent_schema",
#             "schema": {
#                 "type": "object",
#                 "properties": {
#                     "intent": {"type": "string"},
#                     "city": {"type": ["string", "null"]},
#                     "date": {"type": ["string", "null"]},
#                     "forecast": {"type": ["string", "null"]}
#                 },
#                 "required": ["intent", "city"]
#             }
#         }
#     },
#     temperature=0.2
# )
#
# # print(response.choices[0].message.content)
# #
# import pycountry
# def country_code(name: str):
#     try:
#
#
#         code = pycountry.countries.lookup(name)
#         return code.alpha_2.lower()
#     except LookupError:
#         return None
#
# print(country_code("united kingdom"))

from datetime import datetime
print()