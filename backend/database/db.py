import psycopg2
import os

def get_connection():
    db_connection = psycopg2.connect(
        dbname=os.environ.get("DB_NAME", "elderly_assistant_app"),
        user=os.environ.get("DB_USER", ""),
        password=os.environ.get("DB_PASS", ""),
        host=os.environ.get("DB_HOST", "localhost"),
        port=os.environ.get("DB_PORT", "5432"),
    )
    return db_connection

def get_cities_db(city):
    connection = get_connection()
    cursor = connection.cursor()
    
    QUERY = """
    SELECT latitude,longitude 
    FROM cities
    WHERE cityname = %s
    """
    cursor.execute(QUERY,(city,))
    result = cursor.fetchone()
    
    return result
    
     
def add_city_db(city,latitude,longitude):
    
    connection = get_connection()
    cursor = connection.cursor()
    
    QUERY = """
    INSERT INTO cities (cityname, latitude, longitude)
    VALUES ( %s,%s,%s)
    """
    cursor.execute(QUERY,(city,latitude,longitude))
    connection.commit()
    cursor.close()
    connection.close()
    
def add_reminders_db(title,description,date):
     connection = get_connection()
     cursor = connection.cursor()
     
     QUERY = """
     INSERT INTO reminders (title,reminder_description,reminder_date)
     VALUES (%s,%s,%s)
     """
     cursor.execute(QUERY,(title,description,date))
     connection.commit()
     cursor.close()
     connection.close()
     
    
     
     
    
       
    
    
        