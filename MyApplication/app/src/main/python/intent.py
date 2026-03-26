import difflib
import geonamescache
import re
import json
from datetime import datetime, timedelta
from weather_api import *
from news_api import *
WEATHER_KEYWORDS = [

    "weather", "forecast", "temperature", "temp", "rain", "raining",
    "snow", "snowing", "wind", "sunny", "cloudy", "fog", "foggy",
    "humid", "humidity", "storm", "thunderstorm", "hail", "ice",
    "cold", "hot", "warm", "freezing", "chilly", "heat", "heatwave",
    "drizzle", "shower", "overcast", "clear sky", "uv index", "pouring", "lashing",

    "umbrella", "jacket", "coat",

    "outside today", "outside tomorrow", "outside this week",
    "going out", "dress for", "what to wear",
]

NEWS_KEYWORDS = [

    "news", "headline", "headlines", "article", "articles", "report",
    "latest", "breaking", "update", "updates", "story", "stories",
    "press", "media", "coverage", "journalist",


    "what's happening", "whats happening", "what is happening",
    "going on in", "situation in", "current events",
    "top stories", "in the news", "making news",


    "bbc", "cnn", "fox news", "guardian", "irish times", "reuters",
    "al jazeera", "bloomberg", "nytimes", "new york times",
    "independent", "rte", "sky news", "politico",


    "election", "elections", "parliament", "government", "minister",
    "president", "prime minister", "war", "conflict", "crisis",
    "protest", "strike", "scandal", "lawsuit", "verdict",
    "economy", "inflation", "recession", "budget", "policy",
    "stock market", "stocks", "shares", "crypto", "bitcoin",
]

#
WEATHER_OVERRIDE = [
    "hurricane", "tornado", "typhoon", "cyclone", "blizzard",
    "heatwave", "heat wave", "flood", "flooding", "drought",
    "weather warning", "weather alert", "storm warning"
]
FUZZY_THRESHOLD = 0.8

def fuzz_match(word, l):#for misspellings
    matches = difflib.get_close_matches(word, l, n=1, cutoff=FUZZY_THRESHOLD)
    return len(matches)>0#ret true if any matches ffound

def get_intent(text):
    text=text.lower().strip()
    text = f" {text} "
    words = text.split()


    for i in WEATHER_OVERRIDE:
        if f" {i} " in text:
            return "weather"

    for i in WEATHER_KEYWORDS:
        if f" {i} " in text:
            return "weather"

    for i in NEWS_KEYWORDS:
        if f" {i} " in text:
            return "news"

    for i in words:
        if len(i) < 5:#higher chance of false pos w smnaller words
            continue
        if fuzz_match(i,WEATHER_KEYWORDS):
            return "weather"
        if fuzz_match(i,NEWS_KEYWORDS):
            return "news"


    return "chat"

gc = geonamescache.GeonamesCache()
cities = gc.get_cities()
city_names = [cities[city]['name'].lower() for city in cities]

multi_word = set()
single_word = set()

for city in city_names:
    if " " in city:
        multi_word.add(city)
    else:
        single_word.add(city)

def get_city(text, default):
    for city in multi_word:
        if city in text:
            return city.title()

    # single word exact match
    words = text.split()
    for word in words:

        if word in single_word:
            return word.title()

    # fuzzy for misspelling
    # for word in words:
    #     if len(word) < 5:
    #         continue
    #     matches = difflib.get_close_matches(word, single_word, n=1, cutoff=FUZZY_THRESHOLD)
    #     if matches:
    #         return matches[0].title()

    return default


def days(text):

    today = datetime.now().weekday()

    DAY_NAMES = {
        "monday": 0, "tuesday": 1, "wednesday": 2, "thursday": 3,
        "friday": 4, "saturday": 5, "sunday": 6,
        "mon": 0, "tue": 1, "wed": 2, "thu": 3, "fri": 4, "sat": 5, "sun": 6
    }

    for day_name, day_num in DAY_NAMES.items():
        if day_name in text:
            diff = (day_num - today) % 7
            return diff if diff > 0 else 7  # if same day next week


    if "day after tomorrow" in text:
        return 2
    if "tomorrow" in text or "tmrw" in text or "tmr" in text:
        return 1
    if "next week" in text:
        return 7
    if "this week" in text or "coming days" in text:
        return 3
    if "weekend" in text:
        days_to_saturday = (5 - today) % 7
        return days_to_saturday if days_to_saturday > 0 else 7

    # "in x days"
    match = re.search(r"in (\d+) days?", text)
    if match:
        return int(match.group(1))

    # "x day forecast" // "x-day forecast"
    match = re.search(r"(\d+)[\s-]day", text)
    if match:
        return int(match.group(1))

    return 0

def extract_hour(text):

    if "midnight" in text:
        return 0
    if "early morning" in text or "dawn" in text:
        return 6
    if "morning" in text:
        return 9
    if "noon" in text or "midday" in text or "lunchtime" in text or "lunch time" in text:
        return 12
    if "afternoon" in text:
        return 14
    if "evening" in text or "tonight" in text:
        return 19
    if "night" in text:
        return 21

    #  3pm,15:00,9 am, 3pm,9am diff ways to word times
    match = re.search(r"\bat?\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b", text)
    if match:
        hour   = int(match.group(1))
        period = match.group(3)# pm/am

        if period == "pm" and hour != 12:#convert to 24hour
            hour += 12
        elif period == "am" and hour == 12:
            hour = 0
        elif period is None and hour <= 6:#assume day if pm/am not specified
            hour += 12

        return hour if 0 <= hour <= 23 else None

    return None  # no hour specified call full day forecast

def getWeather(prompt, default):
    prompt = prompt.lower().strip()
    city=get_city(prompt,default)
    days_ahead=days(prompt)
    hour=extract_hour(prompt)

    result = execute_weather_function(
        city=city,
        days_ahead=days_ahead,
        hour=hour

    )

    return json.dumps({
        "intent": "weather",
        "prompt": prompt,
        "city": city,
        "result": result.get("weather", result)
    })


cache_coords = {
    "dublin": (53.33306, -6.24889),
    "galway": (53.27245, -9.05095),
    "san francisco": (37.77493, -122.41942),
    "athlone":(53.4239, -7.9403)
}



def execute_weather_function(city: str, days_ahead: int = 0, hour: int = None):

    # if connection.closed: #incase connection between db and backend is severed
    #     print("connection not found, restarting")
    #     connection = get_connection()
    # connection = get_connection()
    city = city.lower()


    if city in cache_coords:
        print(f"Using cached coordinates for {city}")
        latitude, longitude = cache_coords[city]
    else:
        coords = get_coordinates(city)
        if coords is None:
            return {"error": f"City '{city}' not found"}
        latitude, longitude = coords
        cache_coords[city] = (latitude, longitude)


    if days_ahead < 1:
        # curent weather
        weather = get_current_weather(latitude, longitude)
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

NEWS_SOURCES = {
    # Ireland
    "irish times": "irishtimes.com",
    "independent": "independent.ie",
    "journal": "thejournal.ie",
    "breaking news": "breakingnews.ie",

    # United Kingdom
    "bbc": "bbc.com",
    "guardian": "theguardian.com",
    "telegraph": "telegraph.co.uk",
    "times": "thetimes.co.uk",
    "sky news": "sky.com",
    "daily mail": "dailymail.co.uk",

    # United States
    "cnn": "edition.cnn.com",
    "new york times": "nytimes.com",
    "nytimes": "nytimes.com",
    "washington post": "washingtonpost.com",
    "fox news": "foxnews.com",
    "nbc": "nbcnews.com",
    "abc news": "abcnews.go.com",
    "cbs news": "cbsnews.com",
    "bloomberg": "bloomberg.com",

    # Europe
    "euronews": "euronews.com",
    "politico": "politico.eu",
    "le monde": "lemonde.fr",
    "france 24": "france24.com",
    "reuters": "reuters.com",

    # Asia
    "al jazeera": "aljazeera.com",
    "japan times": "japantimes.co.jp",
    "the hindu": "thehindu.com",
}

def execute_news_function(source: str = None, topic: str = None):


    error_msg = ""


    if source:
        source = source.lower()
        if source == "rte.ie" or source=="rte":
            source = "independent.ie"
            country = "Ireland"
            error_msg = " (RTE is not supported. Showing results from Independent.ie instead.)"


    # get news
    headlines = get_news(country=None, cat=None, source=source, language="en", specific_topic=topic )

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
def getNews(prompt):
    prompt = prompt.lower()
    source=None
    for name,dom in NEWS_SOURCES.items():
        if name in prompt:
            source = dom

    result = execute_news_function(source=source, topic=prompt)

    return {
            "intent": "news",
            "prompt": prompt + result.get("error_msg", ""),
            "result": result["headlines"]
        }






# def handle_prompt(raw_prompt: str,default_city:str, current_time = None) -> dict:
#
#     # if connection.closed or connection is None: #incase connection between db and backend is severed
#     #     print("connection not found, restarting")
#     #     connection = get_connection()
#
#
#
#     if (current_time is None):
#         current_time = datetime.now().strftime("%A, %B %d, %Y at %H:%M")
#
#
#     # add intent extraction
#
#     # call funcs
#     if intent == "weather":
#
#         #add parameter extraction
#
#
#
#
#     elif intent == "news":
#
#         #add parameter extraction
#
#
#         result = execute_news_function(
#             country=country,
#             #category=function_args.get("category"),
#             source=source,
#             topic=topic
#         )
#         print(result)
#         return {
#             "intent": "news",
#             "prompt": raw_prompt + result.get("error_msg", ""),
#             "result": result["headlines"]
#         }
#
#     # regular chat response (no funcs call)
#     else:
#
#         return {
#             "intent": "chat",
#             "prompt": raw_prompt,
#             "result": response_message.content
#         }