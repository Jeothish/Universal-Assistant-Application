"""
weather_api.py

This script connects to the OpenMateo API to get current weather and forecast data for any city for any day ahead and for any hour
    

Author:
    Jeothish Senthilkumar
    Muhammad Amir Maier    
    
"""


import requests
from datetime import datetime,timezone,timedelta

#API_KEY = "f57e4e15a233f51887069167cb3b8bd4"
#UNITS = "metric"

#Weather codes from Open-Mateo
WEATHER_CODES = {
    0: "Clear sky",
    1: "Mainly clear",
    2: "Partly cloudy",
    3: "Overcast",
    45: "Fog",
    48: "Depositing rime fog",
    51: "Light drizzle",
    53: "Moderate drizzle",
    55: "Dense drizzle",
    56: "Light freezing drizzle",
    57: "Dense freezing drizzle",
    61: "Slight rain",
    63: "Moderate rain",
    65: "Heavy rain",
    66: "Light freezing rain",
    67: "Heavy freezing rain",
    71: "Slight snow fall",
    73: "Moderate snow fall",
    75: "Heavy snow fall",
    77: "Snow grains",
    80: "Slight rain showers",
    81: "Moderate rain showers",
    82: "Violent rain showers",
    85: "Slight snow showers",
    86: "Heavy snow showers",
    95: "Thunderstorm (slight/moderate)",
    96: "Thunderstorm with slight hail",
    99: "Thunderstorm with heavy hail"
}

def get_coordinates(city_name):
    """
    Used to get the coordinates of any given city name

    Args:
        city_name (str): The name of the city you wish to get the coordinates of

    Returns:
        tuple: (latitude, longitude) if the city is found, otherwise None
    """

    url = f"https://geocoding-api.open-meteo.com/v1/search?name={city_name}"
    response = requests.get(url)
    data = response.json()

    if "results" in data and len(data["results"]) > 0:
        #[0] gets most relevant city name
        latitude = data["results"][0]['latitude']
        longitude = data["results"][0]['longitude']
        return latitude,longitude
    else:
        return None
    
def get_current_weather(latitude,longitude):
    """
    Gets the current weather for the specified coordinates

    Args:
        latitude (float): Latitude of given city
        longitude (float): Longitude of given city

    Returns:
        dict: Formatted dictionary containing current weather data for the given city, otherwise None
    """
    url = f"https://api.open-meteo.com/v1/forecast?latitude={latitude}&longitude={longitude}&current_weather=true"

    response = requests.get(url)
    data = response.json()

    if "current_weather" in data:

        weather = data["current_weather"]

        raw_time = weather["time"]
        local_time = datetime.fromisoformat(raw_time)
        formatted_time = local_time.strftime("%d %B %Y %H:%M")

        formatted_weather = {
            "Temperature (°C)" : weather["temperature"],
            "Wind Speed (km/h)": weather["windspeed"],
            "Wind Direction (°)": weather["winddirection"],
            "Weather Condition" : WEATHER_CODES.get(weather["weathercode"]),
            "Local Time": formatted_time
        }

        return formatted_weather
    else:
        return None

def get_forecast_weather(latitude,longitude,target_datetime):
    """
    Gets the forecasted weather for a future date and hour

    Args:
       latitude (float): Latitude of given city
        longitude (float): Longitude of given city
        target_datetime (datetime): The future date and hour to forecast

    Returns:
        dict: Formatted dictionary containing current weather data for the given city for a future day and time, otherwise an error message
    """

    url = f"https://api.open-meteo.com/v1/forecast?latitude={latitude}&longitude={longitude}&hourly=temperature_2m,weathercode,windspeed_10m,winddirection_10m&timezone=auto"
    response = requests.get(url)
    data = response.json()

    target_time = target_datetime.strftime("%Y-%m-%dT%H:00")
    if target_time in data["hourly"]["time"]:
        idx = data["hourly"]["time"].index(target_time)
        forecast_weather = {
        "Temperature (°C)" :  data["hourly"]["temperature_2m"][idx],
        "Wind Speed (km/h)": data["hourly"]["windspeed_10m"][idx],
        "Wind Direction (°)": data["hourly"]["winddirection_10m"][idx],
        "Weather Condition" : WEATHER_CODES.get(data["hourly"]["weathercode"][idx]),
        "Local Time": target_datetime.strftime("%d %B %Y %H:%M")
        }
        return forecast_weather
    else:
        return{"error": "Forecast for this hour not available"}


##Used for testing within the current file 

#if __name__ == "__main__":
    #current_city_name = input("What city would you like to get the current weather for? ")
    #latitude,longitude = (get_coordinates(current_city_name))
    #print(f"===== Current Weather IN {current_city_name} =====")
    #print(get_current_weather(latitude,longitude))

    #forecast_city_name = input("What city would you like forecast data with?")
    #lat,lon = (get_coordinates(forecast_city_name))
    #days_ahead = int(input("How many days from today? "))
    #hour = int(input("Hour of the day (0-23)? "))
    #target_datetime = datetime.now() + timedelta(days=days_ahead)
    #target_datetime = target_datetime.replace(hour=hour, minute=0,second=0,microsecond=0)
    
    #print(f"\n ==== Forcast for {forecast_city_name} on {target_datetime.strftime("%d %B %Y %H:%M")} ====")

    #print(get_forecast_weather(lat,lon,target_datetime))


    

    
    

    

