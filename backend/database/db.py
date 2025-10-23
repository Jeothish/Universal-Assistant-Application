import psycopg2
import os

def get_connection():
    db_connection = psycopg2.connect(
    dbname="elderly_assistant_app",
    user="",
    password="",
    host="localhost",
    port="5432",
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


def get_reminders_db(rem_title=None,rem_date=None,rem_description=None):
     connection = get_connection()
     cursor = connection.cursor()
     
     QUERY = """
     SELECT (%s,%s,%s)
     FROM reminders
     WHERE
     VALUES (%s,%s,%s,)
     """
     cursor.execute(QUERY,(rem_title,rem_date,rem_description))
     connection.commit()
     cursor.close()
     connection.close()
        
def add_reminders_db(rem_title,rem_date,rem_description=None,is_complete=False,recur_type='none',recur_day_of_week=None,recur_time=None):
     connection = get_connection()
     cursor = connection.cursor()
     
     QUERY = """
     INSERT INTO reminders (title,reminder_date,reminder_description,is_complete,recurrence_type,recurrence_day_of_week,recurrence_time)
     VALUES (%s,%s,%s,%s,%s,%s,%s)
     """
     cursor.execute(QUERY,(rem_title,rem_date,rem_description,is_complete,recur_type,recur_day_of_week,recur_time))
     connection.commit()
     cursor.close()
     connection.close()
     
def delete_reminders_db(reminder_id):
    
    connection = get_connection()
    cursor = connection.cursor()
     
    QUERY = """
    DELETE FROM reminders
    WHERE reminder_id = %s
    """
    cursor.execute(QUERY,(reminder_id,))
    connection.commit()
    cursor.close()
    connection.close()     
     
     
    
     
     
    
       
    
    
        