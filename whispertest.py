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

result = model.transcribe("news sample.m4a")

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

    class NewsSchema(BaseModel):
        topic: str
        city: str

    class Forecast(BaseModel):  # for LLM response schema
        city: str
        days_ahead: int
        hour24: int


    class IntentSchema(BaseModel):  # for LLM response schema
        intent: IntentType
        forecast_info: Forecast
        news_info: NewsSchema


    #use LLM to detect intent
    response = client.models.generate_content(
        model="gemini-2.5-flash",
        contents=raw_prompt+"get the intent for this prompt, if no hour specified for weather, set hour24 as 25",
        config = {"response_mime_type": "application/json","response_schema":IntentSchema,},
    )


    print(response.text)

    return json.loads(response.text)


response_llm=get_intent_llm()
intent_llm=response_llm["intent"]

print("LLM detected intent: "+intent_llm)


#get intent using keyword detection to authenticate llm response
def get_intent_kw():
    if any(word in prompt for word in weather_keywords):
        return "weather"

    elif any(word in prompt for word in news_keywords):
        return "news"

    else:
        return "chat"

intent_kw = get_intent_kw()

#create lists/sets for cities
gc = geonamescache.GeonamesCache()
# library for city names
cities = gc.get_cities()
city_names = [cities[city]['name'].lower() for city in cities]

multi_word = []

# store cities with names with multiple words
for i in city_names:
    if " " in i:
        multi_word.append(i)
        city_names.remove(i)  # remove from og list, to speed up check

city_names = set(city_names)
multi_word = set(multi_word)

#city detection using KW
def get_city(string,split_string):

    detected_city = ""

    # check if prompt contains a multi-word city first
    for city in multi_word:
        if city in string:
            detected_city= city
            break

    # if not multiword city, check single word cities
    if detected_city== "":
        for city in split_string:
            if city in city_names:
                detected_city = city
                break

    print("KWD city: "+detected_city)
    return detected_city


#get weather forecast
if intent_kw == "weather" and intent_llm == "weather":

    found_city = get_city(raw_prompt,prompt)

    forecast_llm = response_llm["forecast_info"]

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

        #TODO
        # change to whole day forecast if hour = 25

        if hour >25:
            hour = 12

        target_datetime = datetime.now() + timedelta(days=days_ahead)
        target_datetime = target_datetime.replace(hour=hour, minute=0, second=0, microsecond=0)

        print(f"\n ==== Forcast for {forecast_city_name} on {target_datetime.strftime("%d %B %Y %H:%M")} ====")

        print(get_forecast_weather(lat, lon, target_datetime))

#news api
elif intent_kw == "news" and intent_llm == "news":

    #get relevent params for news
    news_llm = response_llm["news_info"]
    news_city = news_llm["city"]
    news_topic = news_llm["topic"]

    #if no region specified get news for Dublin
    if news_city == "":
        news_city = "Dublin"
    if news_topic == "":
        headlines = get_news(news_city)
    else:
        headlines = get_news(news_city,news_topic)

    for i in headlines:
        print(i)

#chat bot
elif intent_llm == "chat":
    response = client.models.generate_content(
        model="gemini-2.5-flash",contents=raw_prompt + " keep it short and simple.")
    print(response.text)

else:
    print("Your request could not be processed.")

#TODO
# db for cities
# fix news + weather api results
# recode flask code
