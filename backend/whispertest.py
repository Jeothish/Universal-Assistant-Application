import geonamescache
from google import genai

from pydantic import BaseModel
import enum
import json

from app import *
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:1234/v1",
    api_key="lm-studio"
)
# client = genai.Client(api_key = '')
#
# llm = "gemini-2.5-flash-lite"

weather_keywords = ["weather", "temperature", "rain", "forecast", "sunny", "wind", "humidity", "snow", "climate"]
news_keywords = ["news","events","headlines","breaking","stories","trending"]


def get_intent_llm(raw_prompt: str ) -> dict:

    #pydantic classes to ensure data returned from llm is in right format and of right type
    class IntentType(enum.Enum):
        news = "news"
        weather = "weather"
        chat_with_llm = "chat"

    class CatType(enum.Enum):
        business = "business"
        crime = "crime"
        domestic = "domestic"
        education = "education"
        entertainment = "entertainment"
        environment = "environment"
        food = "food"
        health = "health"
        lifestyle = "lifestyle"
        politics = "politics"
        science = "science"
        sports = "sports"
        technology = "technology"
        top = "top"
        tourism = "tourism"
        world = "world"
        other = "other"

    class NewsSchema(BaseModel):
        category: CatType
        country: str
        source: str


    class Forecast(BaseModel):  # for LLM response schema
        city: str
        days_ahead: int
        hour24: int


    class IntentSchema(BaseModel):  # for LLM response schema
        intent: IntentType
        forecast_info: Forecast
        news_info: NewsSchema


    #use LLM to detect intent
    response = client.chat.completions.create(
        model="local-model",
        messages=[
            {
                "role": "system",
                "content": (
                    "You are an intent extraction API. "
                    "Respond only with valid JSON matching the schema."
                )
            },
            {
                "role": "user",
                "content": (
                        raw_prompt +
                        " Get the intent for this prompt. "
                        "If no hour is specified for weather, set hour24 to 25. "
                        "For news source, keep it lowercase and avoid spaces. "
                        "If no source is specified, keep it null. "
                        "No domains (e.g., nytimes). "
                        "For country codes: ie = ireland, us = us."
                )
            }
        ],
        response_format={
            "type": "json_schema",
            "json_schema": {
                "name": "intent_schema",
                "schema": IntentSchema.schema()
            }
        },
        temperature=0.2
    )

    result = response.choices[0].message.content
    print(result)

    return json.loads(result)







#get intent using keyword detection to authenticate llm response
def get_intent_kw(prompt):
    if any(word in prompt for word in weather_keywords):
        return "weather"

    elif any(word in prompt for word in news_keywords):
        return "news"

    else:
        return "chat"



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



def handle_prompt(raw_prompt: str) -> dict:
    response_llm = get_intent_llm(raw_prompt)
    intent_llm = response_llm["intent"]


    #get weather forecast
    prompt_to_split=""
    #remove spaces, question marks, full stops etc from prompt for easier keyword detection
    for i in raw_prompt:
        if i.isalpha() or i==" " or i.isdigit():
            prompt_to_split+=i
    prompt = prompt_to_split.split()
    #break prompt down into words in a list
    intent_kw = get_intent_kw(prompt)



    if intent_kw == "weather" and intent_llm == "weather":

        found_city = get_city(raw_prompt,prompt)

        forecast_llm = response_llm["forecast_info"]

        print(forecast_llm)

        forecast_llm["city"] = forecast_llm["city"].lower()

        #to prevent hallucination/LLM error, use keyword detecttion to authenticate llm response
        if found_city != forecast_llm["city"]:
            #raise Exception("City detected by LLM and keyword matching algorithm doesn't match")
            found_city = forecast_llm["city"]


        #if user doesnt specify what day they want forecast for, get current forecast
        if forecast_llm["days_ahead"] < 1:
            current_city_name = found_city
            latitude, longitude = (get_coordinates(current_city_name))
            #print(f"===== Current Weather IN {current_city_name} =====")
            weather = get_current_weather(latitude, longitude)


        #if user specifies what day, get forecast for that day, if no time is specified, it will default to 12:00
        else:
            if forecast_llm["hour24"] == 25:
                latitude,longitude = get_coordinates(found_city)
                target_day = datetime.now() + timedelta(days=forecast_llm["days_ahead"])

                weather = (get_forecast_weather_day(latitude,longitude,target_day))

            else:
                latitude, longitude = get_coordinates(found_city)
                target_datetime = datetime.now() + timedelta(days=forecast_llm["days_ahead"])
                target_datetime = target_datetime.replace(hour=forecast_llm["hour24"], minute=0, second=0, microsecond=0)

                weather = (get_forecast_weather_specific_time(latitude, longitude, target_datetime))
        if isinstance(weather, list):
            weather = weather[0]
        return {"intent": "weather","city": found_city,"result": weather}

    #news api
    elif intent_kw == "news" and intent_llm == "news":

        #get relevent params for news
        news_llm = response_llm["news_info"]
        news_country = news_llm["country"]
        news_topic = news_llm["category"]
        news_source = news_llm["source"]



        #if no region specified get news for worldwide
        if news_country == "" or news_country == "null":
            news_country = "wo"

        if news_source == "" or news_source == "null":
            headlines = get_news(news_country, news_topic)
        else:
            headlines = get_news(news_country, news_topic, news_source)


        return {
            "intent": "news",
            "result": headlines["result"]
        }

    #chat bot
    elif intent_llm == "chat":
        response = client.chat.completions.create(
            model="local-model",
            messages=[
                {
                    "role": "system",
                    "content": "You are a helpful assistant. Keep responses short and simple."
                },
                {
                    "role": "user",
                    "content": raw_prompt
                }
            ],
            temperature=0.4
        )

        return {
            "intent": "chat",
            "result": response.choices[0].message.content
        }


    else:
        raise Exception("unknown intent. kw: "+intent_kw+" llm: "+intent_llm)




