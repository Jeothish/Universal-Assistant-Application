"""
weather_api.py

Description:
    This script connects to the OpenWeatherMap API to get current weather and 5 day forecast data for a given city.
    The data from the API is then formatted further

Author:
    Jeothish Senthilkumar
    Muhammad Amir Maier    
    
"""


import requests
from datetime import datetime

API_KEY = "f57e4e15a233f51887069167cb3b8bd4"
UNITS = "metric"


def get_current_weather(city_name):
    """Calculates the current weather in a given city 

    Args:
        city_name (str): The name of the city from which you want the current weather

    Returns:
        dict: The API's JSON responses parsed into a python dictionary
    """

    url = f"https://api.openweathermap.org/data/2.5/weather?q={city_name}&units={UNITS}&appid={API_KEY}"
    response = requests.get(url)
    return response.json()

def get_forecast(city_name):
    """Calculates the weather forecast for the next 5 days in a given city

    Args:
        city_name (str): The name of the city from which you want the current weather

    Returns:
        dict: The API's JSON responses parsed into a python dictionary
    """
    url = f"https://api.openweathermap.org/data/2.5/forecast?q={city_name}&units={UNITS}&appid={API_KEY}"
    response = requests.get(url)
    return response.json()

#TO DO
def format_weather_data(city_name):
    current_weather_data = get_current_weather(city_name)
    forecast_data = get_forecast(city_name)

if __name__ == "__main__":
    city_name = input("Enter the city name: ")
    print(f"======CURRENT WEATHER IN {city_name}==== \n" , get_current_weather(city_name))
    print()
    print(f"=====FORECAST IN {city_name}===== \n" , get_forecast(city_name))
