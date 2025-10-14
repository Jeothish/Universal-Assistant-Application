from flask import Flask,request,jsonify
from services.weather_api import format_current_weather_data , get_current_weather
app = Flask(__name__)

@app.route("/weather",methods=["GET"])
def weather():
    city = request.args.get("city")

    if not city:
        return jsonify({"error": "No city selected"})
    
    data = format_current_weather_data(get_current_weather(city))
    return jsonify(data)

if __name__ == "__main__":
    app.run(debug=True)