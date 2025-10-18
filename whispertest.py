import whisper
from weather_api import *
import geonamescache
from google import genai
from google.genai import types
from pydantic import BaseModel
import enum
import json
from news_api import *

client = genai.Client(api_key = 'AIzaSyCe-x74VuKCWJ0iPd9t0BVW3LHtoF69T_k')

# Load model (tiny, base, small, medium, large)
model = whisper.load_model("base")

result = model.transcribe("chatwithllm.m4a")

raw_prompt = str(result["text"]).lower()#prompt string

prompt_to_split=""
#remove spaces, question marks, full stops etc from prompt for easier keyword detection
for i in raw_prompt:
    if i.isalpha() or i==" " or i.isdigit():
        prompt_to_split+=i
prompt = prompt_to_split.split()
#break prompt down into words in a list

print(prompt)

weather_keywords = ["weather", "temperature", "rain", "forecast", "sunny", "wind", "humidity", "snow", "climate"]
news_keywords = ["news","events","headlines","breaking","stories","trending"]


def get_intent_llm():

    class IntentType(enum.Enum):
        news = "news"
        weather = "weather"
        chat_with_llm = "chat"


    class IntentSchema(BaseModel):  # for LLM response schema
        intent: IntentType


    #use LLM to detect intent
    response = client.models.generate_content(
        model="gemini-2.5-flash",
        contents=raw_prompt+"get the intent for this prompt",
        config = {"response_mime_type": "application/json","response_schema":IntentSchema,},
    )


    print(response.text)

    return json.loads(response.text)


intent_llm=get_intent_llm()['intent']

print(intent_llm)


#get intent using keyword detection to authenticate llm response
def get_intent_kw():
    if any(word in prompt for word in weather_keywords):
        return "weather"

    elif any(word in prompt for word in news_keywords):
        return "news"

    else:
        return "chat"

intent_kw = get_intent_kw()

#get weather forecast
if intent_kw == "weather" and intent_llm == "weather":

    #library for city names
    gc = geonamescache.GeonamesCache()
    cities = gc.get_cities()
    city_names = [cities[city]['name'].lower() for city in cities]

    multi_word=[]


    #store cities with names with multiple words
    for i in city_names:
        if " " in i:
            multi_word.append(i)
            city_names.remove(i)#remove from og list, to speed up check

    found_city = ""

    #check if prompt contains a multi-word city first
    for city in multi_word:
        if city in raw_prompt:
            found_city = city
            break

    #if not multiword city, check single word cities
    if found_city == "":
        for city in prompt:
            if city in city_names:
                found_city = city
                break

    print(found_city)
    print(raw_prompt)

    class Forecast(BaseModel):#for LLM response schema
        city: str
        days_ahead: int
        hour24: int

    #use LLM to detect city, time and day for forecast
    response = client.models.generate_content(
        model="gemini-2.5-flash",
        contents=raw_prompt,
        config = {"response_mime_type": "application/json","response_schema":Forecast,},
    )

    #convert json response to dict
    forecast: Forecast = response.parsed
    forecast_llm = forecast.model_dump()

    print(forecast_llm)

    forecast_llm["city"] = forecast_llm["city"].lower()

    #to prevent hallucination/LLM error, use keyword detecttion to authenticate llm response
    if found_city != forecast_llm["city"]:
        raise Exception("City detected by LLM and keyword matching algorithm doesn't match")


    #if user doesnt specify what day they want forecast for, get current forecast
    if forecast_llm["days_ahead"] < 1:
        current_city_name = found_city
        latitude, longitude = (get_coordinates(current_city_name))
        print(f"===== Current Weather IN {current_city_name} =====")
        print(get_current_weather(latitude, longitude))

    #if user specifies what day, get forecast for that day, if no time is specified, it will default to 12:00
    else:
        forecast_city_name = forecast_llm['city']
        lat, lon = (get_coordinates(forecast_city_name))
        days_ahead = forecast_llm['days_ahead']
        hour = forecast_llm['hour24']
        target_datetime = datetime.now() + timedelta(days=days_ahead)
        target_datetime = target_datetime.replace(hour=hour, minute=0, second=0, microsecond=0)

        print(f"\n ==== Forcast for {forecast_city_name} on {target_datetime.strftime("%d %B %Y %H:%M")} ====")

        print(get_forecast_weather(lat, lon, target_datetime))

elif intent_kw == "news" and intent_llm == "news":
    headlines = get_news("Dublin")
    for i in headlines:
        print(i)


elif intent_llm == "chat":
    response = client.models.generate_content(
        model="gemini-2.5-flash",contents=raw_prompt + " keep it short and simple.")
    print(response.text)

else:
    print("Your request could not be processed.")

#TODO
# db for cities
# fix news + weather api results
#
