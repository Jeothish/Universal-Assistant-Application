import whisper
from weather_api import *
import geonamescache
from google import genai
from pydantic import BaseModel


class Forecast(BaseModel):
    city: str
    days_ahead: int
    hour24: int

client = genai.Client(api_key = 'AIzaSyCe-x74VuKCWJ0iPd9t0BVW3LHtoF69T_k')



# Load model (tiny, base, small, medium, large)
model = whisper.load_model("base")

result = model.transcribe("recording (6).m4a")

raw_prompt = str(result["text"]).lower()#prompt string

prompt_to_split=""
#remove spaces, question marks, full stops etc from prompt for easier keyword detection
for i in raw_prompt:
    if i.isalpha() or i==" " or i.isdigit():
        prompt_to_split+=i
prompt = prompt_to_split.split()
#break prompt down into words in a list

print(prompt)

#library for city names
gc = geonamescache.GeonamesCache()
cities = gc.get_cities()
city_names = [cities[city]['name'].lower() for city in cities]


multi_word=[]
#print(city_names)
# print(len(city_names))

#TODO
# cache multi word cities

#store cities with names with multiple words
for i in city_names:
    if " " in i:
        multi_word.append(i)
        city_names.remove(i)#remove from og list, to speed up check

# print(len(multi_word))
# print(len(city_names))

found_city = ""

#check if prompt contains a multi word city first
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

keywords = ["weather", "temperature", "rain", "forecast", "sunny", "wind", "humidity", "snow"]

#if user doesnt specify what day they want forecast for, get current forecast
if any(word in prompt for word in keywords) and forecast_llm["days_ahead"] < 1:
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
