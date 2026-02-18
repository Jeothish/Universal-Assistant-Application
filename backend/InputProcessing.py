
import json
from datetime import datetime, timedelta
from openai import OpenAI
import pycountry
from unicodedata import category

from db import get_connection


client = OpenAI(
    base_url="http://localhost:1234/v1",
    api_key="lm-studio"
)


from app import get_current_weather, get_forecast_weather_day, get_forecast_weather_specific_time, get_coordinates, \
    get_news
from emedding import get_intent

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
FUNCTION_DEFINITIONS = [
    {
        "type": "function",
        "function": {
            "name": "get_weather_forecast",
            "description": "Get weather forecast for a specific city. Can get current weather or forecast for future days/hours.",
            "parameters": {
                "type": "object",
                "properties": {
                    "city": {
                        "type": "string",
                        "description": "The city name ONLY if explicitly mentioned by user. Leave empty otherwise (e.g., 'Dublin', 'San Francisco', 'London')"
                    },
                    "days_ahead": {
                        "type": "integer",
                        "description": "Number of days ahead to forecast. 0 for current weather, 1 for tomorrow, etc.",
                        "default": 0
                    },
                    "hour": {
                        "type": "integer",
                        "description": "Specific hour (0-23) for the forecast. If not specified, returns full day forecast.",
                        "minimum": 0,
                        "maximum": 23
                    }
                },
                "required": []
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "get_news_headlines",
            "description": "Get top news headlines for a country and/or a topic.",
            "parameters": {
                "type": "object",
                "properties": {
                    "country": {
                        "type": "string",
                        "description": "Country name ONLY if explicitly mentioned by user. Leave empty otherwise. (e.g., 'Ireland', 'USA', 'UK')."
                    },
                    # "category": {
                    #     "type": "string",
                    #     "enum": [
                    #         "business", "crime", "domestic", "education", "entertainment",
                    #         "environment", "food", "health", "lifestyle", "politics",
                    #         "science", "sports", "technology", "top", "tourism", "world", "other"
                    #     ],
                    #     "description": "News category"
                    #},
                    # "source": {
                    #     "type": "string",
                    #     "enum": [
                    #             "bbc", "cnn", "reuters", "apnews", "theguardian", "independent",
                    #             "irishtimes", "irishexaminer", "rte", "thejournal", "breakingnews",
                    #             "skynews", "foxnews", "nbcnews", "abcnews", "cbsnews", "msnbc",
                    #             "washingtonpost", "nytimes", "thesun", "dailymail", "telegraph",
                    #             "irishmirror", "express", "economist", "bloomberg", "forbes",
                    #             "techcrunch", "theverge", "wired", "arstechnica", "engadget",
                    #             "eurosport", "sportinglife", "talksport"
                    #         ],
                    #     "description": "ONLY insert if user explicitly names a news source. "
                    #},#Leave empty otherwise. Specific news source domain (lowercase, no spaces, no .com/.ie extensions). E.g., 'bbc', 'cnn', 'independent'
                    "topic":{
                        "type": "string",
                        "description": "news topic such as 'Olympics', 'World Cup', 'Donald Trump' etc. Leave empty otherwise."
                    }
                },
                "required": []
            }
        }
    }
]


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
    if source and source.lower() == "rte":
        source = "independent"
        country = "Ireland"
        error_msg = " (RTE is not supported. Showing results from Independent.ie instead.)"


    if country and country.lower() != "none":
        country = country_code(country)

    else:
        country = None

    # get news
    headlines = get_news(country, cat=None, source=None, language="en", specific_topic=topic )

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


def handle_prompt_with_qwen(raw_prompt: str,default_city:str, connection=None, current_time = None) -> dict:

    # if connection.closed or connection is None: #incase connection between db and backend is severed
    #     print("connection not found, restarting")
    #     connection = get_connection()
    connection = get_connection()


    if (current_time is None):
        current_time = datetime.now().strftime("%A, %B %d, %Y at %H:%M")

    intent = get_intent(raw_prompt)
    print("intent: "+intent)
    # Decide tool choice based on intent
    if intent == "weather":
        tools = [FUNCTION_DEFINITIONS[0]]  # Only weather function
        tool_choice = "required"
    elif intent == "news":
        tools = [FUNCTION_DEFINITIONS[1]]  # Only news function
        tool_choice = "required"
    else:
        tools = FUNCTION_DEFINITIONS  # All functions available
        tool_choice = "auto"

    messages = [
        {
            "role": "system",
            "content": (
                "You are a helpful assistant that can provide weather forecasts and news headlines. "
                f"Current date and time: {current_time}. "
                "Use the available functions to help answer user queries. "
                #f"for this prompt use the {intent} function."
                # "If the user's request is not about weather or news, respond conversationally without using functions."
            )
        },
        {
            "role": "user",
            "content": raw_prompt
        }
    ]


    response = client.chat.completions.create(
        model="functiongemma-270m-it",
        messages=messages,
        tools=tools,
        tool_choice=tool_choice,  # let model decide whe to use functions
        temperature=0.1
    )

    response_message = response.choices[0].message

    # check if model calls a function
    if response_message.tool_calls:

        tool_call = response_message.tool_calls[0]
        function_name = tool_call.function.name
        function_args = json.loads(tool_call.function.arguments)


        print(f"Arguments: {function_args}")

        # call funcs
        if function_name == "get_weather_forecast":
            llm_city = function_args.get("city")
            if (llm_city.lower() not in raw_prompt) or (llm_city is None):
                llm_city = default_city


            result = execute_weather_function(
                city=llm_city,
                days_ahead=function_args.get("days_ahead", 0),
                hour=function_args.get("hour"),
                connection=connection
            )
            print(result)
            return {
                "intent": "weather",
                "prompt": raw_prompt,
                "city": llm_city,
                "result": result.get("weather", result)
            }

        elif function_name == "get_news_headlines":
            result = execute_news_function(
                country=function_args.get("country"),
                #category=function_args.get("category"),
                #source=function_args.get("source"),
                topic=function_args.get("topic")
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

