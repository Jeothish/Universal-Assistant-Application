import requests
from datetime import datetime,timezone,timedelta

API_KEY = "REMOVED"

def get_news(city,topic=None,date=datetime.now(),language="en",sort_by = "publishedAt"):
    if not city:
        return{"error" :"City is required"}
    
    query= f"{city} {topic}"
    date = date.strftime(%Y-%m-%d)
    
    url = f"https://newsapi.org/v2/everything?q={query}&from={date}&sortBy={sort_by}&language={language}&apiKey={API_KEY}"
