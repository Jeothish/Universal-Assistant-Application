"""
app.py

Flask server providing endpoints for: 
                    current and forecasted weather data
                    news data

Author:
    Jeothish Senthilkumar
    Muhammad Amir Maier 
"""



from flask import Flask,request,jsonify
from datetime import datetime,timedelta
from backend.services.weather_api import get_current_weather,get_forecast_weather_specific_time,get_coordinates,get_forecast_weather_day
from backend.services.news_api import get_news

app = Flask(__name__)

@app.route("/weather/current",methods=["GET"])
def current_weather():
    """
    API endpoint to fetch current weather data for a given city

    Query Parameters:
        city(str): Name of the city to get weather data

    Returns:
        JSON : A dictionary containing current weather data for the given city    
    """
    city = request.args.get("city")

    if not city:
        return jsonify({"error": "No city selected"})
    

    coords = get_coordinates(city)

    if not coords:
        return jsonify({"error" : "City not found"})
    
    latitude,longitude = coords
    data = get_current_weather(latitude,longitude)
    
    
    return jsonify({"city" : city , 
                    "weather": data
                    })

@app.route("/weather/forecast/specific",methods=["GET"])
def forecast_weather_specific():
    """
    API endpoint to fetch forecasted weather data for a future day and hour

    Query Parameters:
        city(str): Name of the city to forecast data
        days(int): Number of days from today
        hour(int): Hour of the day in 24-hour format

    Returns:
        JSON : A dictionary containing forecasted weather data for the given city for a given day and hour   
    """
    city = request.args.get("city")
    days_ahead = int(request.args.get("days",0))
    hour = int(request.args.get("hours",12))

    if not city:
        return jsonify({"error": "No city selected"})
    
    coords = get_coordinates(city)

    if not coords:
        return jsonify({"error" : "City not found"})
    
    latitude,longitude = coords

    target_datetime = datetime.now() + timedelta(days=days_ahead)
    target_datetime = target_datetime.replace(hour=hour, minute=0,second=0,microsecond=0)

    forecast = get_forecast_weather_specific_time(latitude,longitude,target_datetime)

    return jsonify({
        "City": city,
        "Target_Time": target_datetime.strftime("%d %B %Y %H:%M"),
        "Forecast" : forecast
    })
    
@app.route("/weather/forecast/day",methods=["GET"])
def forecast_weather_day():
    """
    API endpoint to fetch forecasted hourly weather data for a day  

    Query Parameters:
        city(str): Name of the city to forecast data
        days(int): Number of days from today

    Returns:
        JSON : A dictionary containing forecasted hourly weather data for the given city for a given day  
    """
    city = request.args.get("city")
    days_ahead = int(request.args.get("days",0))
    

    if not city:
        return jsonify({"error": "No city selected"})
    
    coords = get_coordinates(city)

    if not coords:
        return jsonify({"error" : "City not found"})
    
    latitude,longitude = coords

    target_day = datetime.now() + timedelta(days=days_ahead)
    target_day = target_day.replace(minute=0,second=0,microsecond=0)

    forecast = get_forecast_weather_day(latitude,longitude,target_day)

    return jsonify({
        "City": city,
        "Target_Day": target_day.strftime("%d %B %Y %H:%M"),
        "Forecast" : forecast
    })    
    
@app.route("/news",methods=["GET"])    
def news():
    """
    API endpoint to fetch news articles for a given city and topic

    Query Parameters:
        city (str): The name of the city you want news data for
        topic (str, optional): The topic you want news data for. Defaults to None.
        language (str, optional): Language of news. Defaults to "en".

    Returns:
        JSON : A dictionary containing news articles  for the given city and topic   
    """
    city = request.args.get("city")
    topic = request.args.get("topic")
    language = request.args.get("language")
    
    if not city:
        return jsonify({"error": "No city selected"})
    
    news_data = get_news(city,topic,language)
    return jsonify(news_data)
    
        

if __name__ == "__main__":
    """
    Starts the Flask development server on localhost:5000 in debug mode
    """
    app.run(debug=True)