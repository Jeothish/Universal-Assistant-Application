import psycopg2
from dotenv import load_dotenv
import os

# Load environment variables from .env
# load_dotenv()
#
# def get_connection():
#
#     USER = os.getenv("user")
#     PASSWORD = os.getenv("password")
#     HOST = os.getenv("host")
#     PORT = os.getenv("port")
#     DBNAME = os.getenv("dbname")
#     print(f"Connecting with USER={USER}, PASSWORD={'*' * len(PASSWORD)}, HOST={HOST}, PORT={PORT}, DBNAME={DBNAME}")
#
#     # Connect to the database
#     try:
#         connection = psycopg2.connect(
#             user=USER,
#             password=PASSWORD,
#             host=HOST,
#             port=PORT,
#             dbname=DBNAME,
#             sslmode = "require"
#         )
#         print("Connection successful!")
#         return connection
#
#     except Exception as e:
#         print(f"Failed to connect: {e}")
#         return None

def get_cities_db(city,connection):


    connection = connection
    cursor = connection.cursor()
    
    QUERY = """
    SELECT latitude,longitude 
    FROM cities
    WHERE cityname = %s
    """
    cursor.execute(QUERY,(city,))
    result = cursor.fetchone()
    
    return result
    
     
def add_city_db(city,latitude,longitude,connection):
    
    connection = connection
    cursor = connection.cursor()
    
    QUERY = """
    INSERT INTO cities (cityname, latitude, longitude)
    VALUES ( %s,%s,%s)
    """
    cursor.execute(QUERY,(city,latitude,longitude))
    connection.commit()
    cursor.close()
    connection.close()


def get_reminders_db(connection,rem_title=None,rem_date=None,rem_description=None,is_complete=None,recur_type='none',recur_day_of_week=None,recur_time=None):
    connection = connection
    cursor = connection.cursor()
    parameters=[]
     
    QUERY = """
     
    SELECT * 
    FROM reminders WHERE TRUE
    """
    
    if rem_title != None:
        QUERY += "AND title = %s"
        parameters.append(rem_title)
        
    if rem_date != None:
        QUERY += "AND date = %s"
        parameters.append(rem_date)
        
    if rem_description != None:
        QUERY += "AND reminder_des = %s"
        parameters.append(rem_description)
        
    if is_complete != None:
        QUERY += "AND title = %s"
        parameters.append(is_complete)
        
    if recur_type != None:
        QUERY += "AND title = %s"
        parameters.append(recur_type)
        
    if recur_day_of_week != None:
        QUERY += "AND title = %s"
        parameters.append(recur_day_of_week)
        
    if recur_time != None:
        QUERY += "AND title = %s"
        parameters.append(recur_time) 
         
     
    cursor.execute(QUERY,tuple(parameters))
    results = cursor.fetchall()
    cursor.close()
    connection.close()
    
    return results
        
def add_reminders_db(connection,rem_title,rem_date,rem_description=None,is_complete=False,recur_type='none',recur_day_of_week=None,recur_time=None):
     connection = connection
     cursor = connection.cursor()
     
     QUERY = """
     INSERT INTO reminders (title,reminder_date,reminder_description,is_complete,recurrence_type,recurrence_day_of_week,recurrence_time)
     VALUES (%s,%s,%s,%s,%s,%s,%s)
     """
     cursor.execute(QUERY,(rem_title,rem_date,rem_description,is_complete,recur_type,recur_day_of_week,recur_time))
     connection.commit()
     cursor.close()
     connection.close()

def edit_reminders_db(connection,rem_title=None,rem_date=None,rem_description=None,is_complete=False,recur_type='none',recur_day_of_week=None,recur_time=None):
    connection = connection
    cursor = connection.cursor()
    parameters=[]
    
    QUERY = """
    UPDATE reminders WHERE TRUE
    """
    
    if rem_title != None:
        QUERY += "AND title = %s"
        parameters.append(rem_title)
        
    if rem_date != None:
        QUERY += "AND date = %s"
        parameters.append(rem_date)
        
    if rem_description != None:
        QUERY += "AND reminder_des = %s"
        parameters.append(rem_description)
        
    if is_complete != None:
        QUERY += "AND title = %s"
        parameters.append(is_complete)
        
    if recur_type != None:
        QUERY += "AND title = %s"
        parameters.append(recur_type)
        
    if recur_day_of_week != None:
        QUERY += "AND title = %s"
        parameters.append(recur_day_of_week)
        
    if recur_time != None:
        QUERY += "AND title = %s"
        parameters.append(recur_time)  
        
    cursor.execute(QUERY,tuple(parameters))                          
        
     
        
            
def delete_reminders_db(reminder_id,connection):
    
    connection = connection
    cursor = connection.cursor()
     
    QUERY = """
    DELETE FROM reminders
    WHERE reminder_id = %s
    """
    cursor.execute(QUERY,(reminder_id,))
    connection.commit()
    cursor.close()
    connection.close()     
     
     
    
     
     
    
       
    
    
        