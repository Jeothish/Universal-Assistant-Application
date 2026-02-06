"""
weather_api.py

This script connects to the NewsData.io API to get top news articles for a given city and topic
    

Author:
    Jeothish Senthilkumar
    Muhammad Amir Maier    
    
"""


import requests
from datetime import datetime,timezone,timedelta
from dotenv import load_dotenv

API_KEY = "REMOVED"

def get_news(country=None,topic=None,source=None,language="en"):
    """
    Gets the top headlines of a given city and topic

    Args:
        city (str): The name of the city you want news data for
        topic (str, optional): The topic you want news data for. Defaults to None.
        language (str, optional): Language of news. Defaults to "en".
        source (str, optional): news source url. Defaults to None.

    Returns:
        dict: Formatted dictionary containing top news articles

    """
    
    url = f"https://newsdata.io/api/1/news"

    params = {
        "language" : language,
        "apikey" : API_KEY,
        "domain" : source,
        "country" : country,
        "category" : topic

    }
    
    response = requests.get(url,params=params)
    data = response.json()
    articles = data.get("results")
    
    if not articles:
        return{"error":"No articles found"}
    
    formatted_news = []


    if len(articles) > 5:
        articles = articles[:5]
    print(articles)

    for article in articles:
        formatted_news.append({
            "Title": article.get("title"),
            "Link" : article.get("link"),
            # "Creator" : article.get("creator"),
            "Description" : article.get("description"),
            "Published" : article.get("pubDate")
        })
    print(formatted_news)
    return formatted_news    


        
    
