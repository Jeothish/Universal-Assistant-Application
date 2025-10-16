"""
weather_api.py

This script connects to the NewsData.io API to get top news articles for a given city and topic
    

Author:
    Jeothish Senthilkumar
    Muhammad Amir Maier    
    
"""


import requests
from datetime import datetime,timezone,timedelta


API_KEY = "pub_a5b78b04509c4f26a27113fd9ae03147"

def get_news(city,topic=None,language="en"):
    """
    Gets the top headlines of a given city and topic

    Args:
        city (str): The name of the city you want news data for
        topic (str, optional): THe topic you want news data for. Defaults to None.
        language (str, optional): Language of news. Defaults to "en".

    Returns:
        dict: Formatted dictionary containing top news articles
    """
    
    if not city:
        return{"error" :"City is required"}
    
    query= f"{city} {topic}"
    
    
    url = f"https://newsdata.io/api/1/news"
    
    params = {
        "q": query,
        "language" : language,
        "apikey" : API_KEY,
    }
    
    response = requests.get(url,params=params)
    data = response.json()
    top_articles = data["results"][:5]
    formatted_news = []
    
    for article in top_articles:
        formatted_news.append({
            "Title": article.get("title"),
            "Link" : article.get("link"),
            "Creator" : article.get("creator"),
            "Description" : article.get("description"),
            "Published" : article.get("pubDate")
        })
        
    return formatted_news    
        
        
    
#if __name__ == "__main__":
    #print(get_news("London"))