
import json
from datetime import datetime, timedelta
from db import *
from unicodedata import category

import pycountry

#Cached for coords
cache_coords = {
    "dublin": (53.33306, -6.24889),
    "galway": (53.27245, -9.05095),
    "san francisco": (37.77493, -122.41942),
    "athlone":(53.4239, -7.9403)
}

UK_VARIANTS = ["uk", "england", "wales", "scotland", "northern ireland", "britain"]

def country_code(name: str) -> str: #country name to code for news api

    try:
        if name.lower() in UK_VARIANTS:
            return "gb"
        elif name.lower() == "america":
            return "us"
        else:
            code = pycountry.countries.lookup(name)
            return code.alpha_2.lower()
    except LookupError:
        return "wo"  # worldwide


#function calling schema



def execute_weather_function(city: str, days_ahead: int = 0, hour: int = None, connection=None):

    # if connection.closed: #incase connection between db and backend is severed
    #     print("connection not found, restarting")
    #     connection = get_connection()
    connection = get_connection()
    city = city.lower()


    if city in cache_coords:
        print(f"Using cached coordinates for {city}")
        latitude, longitude = cache_coords[city]
    else:
        coords = get_coordinates(city, connection)
        if coords is None:
            return {"error": f"City '{city}' not found"}
        latitude, longitude = coords
        cache_coords[city] = (latitude, longitude)


    if days_ahead < 1:
        # curent weather
        weather = get_current_weather(latitude, longitude, connection)
    else:
        if hour is None:
            #full day forecast
            target_day = datetime.now() + timedelta(days=days_ahead)
            weather = get_forecast_weather_day(latitude, longitude, target_day)
        else:
            # specific hour forecast
            target_datetime = datetime.now() + timedelta(days=days_ahead)
            target_datetime = target_datetime.replace(hour=hour, minute=0, second=0, microsecond=0)
            weather = get_forecast_weather_specific_time(latitude, longitude, target_datetime)

    # handle list responses
    if isinstance(weather, list):
        weather = weather[0] if len(weather) > 0 else weather

    return {
        "city": city,
        "weather": weather
    }


def execute_news_function(country: str = None,  source: str = None, topic: str = None):


    error_msg = ""
    if source:
        source = source.lower()
        if source == "rte.ie":
            source = "independent.ie"
            country = "Ireland"
            error_msg = " (RTE is not supported. Showing results from Independent.ie instead.)"


    if country and country.lower() != "none":
        country = country_code(country)

    else:
        country = None

    # get news
    headlines = get_news(country, cat=None, source=source, language="en", specific_topic=topic )

    if isinstance(headlines, dict) and "error" in headlines:#incase of error e.g. no articles found,
        print("Headlines not found"+headlines["error"])
        return {
            "headlines": [],
            "error_msg": "\n"+headlines["error"]
        }

    return {
        "headlines": headlines,
        "error_msg": error_msg
    }


def handle_prompt(llmresp,raw_prompt: str,default_city:str, connection=None, current_time = None) -> dict:

    # if connection.closed or connection is None: #incase connection between db and backend is severed
    #     print("connection not found, restarting")
    #     connection = get_connection()
    connection = get_connection()


    if (current_time is None):
        current_time = datetime.now().strftime("%A, %B %d, %Y at %H:%M")



        # call funcs
        if intent == "weather":

            llm_city = function_args.get("city")
            days_ahead = function_args.get("days_ahead",0)
            hour = function_args.get("hour")

            if hour == '':
                hour = None
            if days_ahead == '':
                days_ahead = None
            if (llm_city.lower() not in raw_prompt) or (llm_city is None) or (llm_city == ''):
                llm_city = default_city


            result = execute_weather_function(
                city=llm_city,
                days_ahead=days_ahead,
                hour=hour,
                connection=connection
            )
            print(result)
            return {
                "intent": "weather",
                "prompt": raw_prompt,
                "city": llm_city,
                "result": result.get("weather", result)
            }

        elif intent == "news":

            country = function_args.get("country")
            source = function_args.get("source")
            topic = function_args.get("topic")

            if country == '':
                country = None
            if source == '':
                source = None
            if topic == '':
                topic = None


            result = execute_news_function(
                country=country,
                #category=function_args.get("category"),
                source=source,
                topic=topic
            )
            print(result)
            return {
                "intent": "news",
                "prompt": raw_prompt + result.get("error_msg", ""),
                "result": result["headlines"]
            }

    # regular chat response (no funcs call)
    else:

        return {
            "intent": "chat",
            "prompt": raw_prompt,
            "result": response_message.content
        }

