import psycopg2
from dotenv import load_dotenv
import os

# Load environment variables from .env
# load_dotenv()
#
def get_connection():
    # connect to database
    try:
        connection = psycopg2.connect(
            os.getenv("DATABASE_URL")
        )
        print("Connection successful!")
        return connection

    except Exception as e:
        print(f"Failed to connect: {e}")
        return None



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
    # cursor.close()
    # connection.close()


def get_reminders_db(connection,reminder_title=None,reminder_date=None,reminder_description=None,is_complete=None,recurrence_type=None,reminder_time=None):
    connection = connection
    cursor = connection.cursor()
    parameters=[]
     
    QUERY = """
     
    SELECT * 
    FROM reminders WHERE TRUE
    """
    
    if reminder_title != None:
        QUERY += " AND reminder_title = %s"
        parameters.append(reminder_title)
        
    if reminder_date != None:
        QUERY += " AND reminder_date = %s"
        parameters.append(reminder_date)
        
    if reminder_description != None:
        QUERY += " AND reminder_description = %s"
        parameters.append(reminder_description)
        
    if is_complete != None:
        QUERY += " AND is_complete = %s"
        parameters.append(is_complete)
        
    if recurrence_type != None:
        QUERY += " AND recurrence_type = %s"
        parameters.append(recurrence_type)
        
        
    if reminder_time != None:
        QUERY += " AND reminder_time = %s"
        parameters.append(reminder_time)

    try:
        cursor.execute(QUERY,tuple(parameters))
        results = cursor.fetchall()
        cursor.close()
        return results
    except Exception as e:
        connection.rollback()
        print(f"Database error when getting reminders: {e}")
        return []
    finally:
        cursor.close()



        
def add_reminders_db(connection,reminder_title,reminder_date,reminder_description=None,is_complete=False,recurrence_type='none',reminder_time=None):
     connection = connection
     cursor = connection.cursor()
     
     QUERY = """
     INSERT INTO reminders (reminder_title,reminder_date,reminder_description,is_complete,recurrence_type,reminder_time)
     VALUES (%s,%s,%s,%s,%s,%s)
     """
     try:
        cursor.execute(QUERY,(reminder_title,reminder_date,reminder_description,is_complete,recurrence_type,reminder_time))
        connection.commit()
     except Exception as e:
         connection.rollback()
         print(f"Database error when adding reminders: {e}")
     finally:
         cursor.close()

     

def edit_reminders_db(connection,reminder_id,reminder_title=None,reminder_date=None,reminder_description=None,is_complete=None,recurrence_type=None,reminder_time=None):
    connection = connection
    cursor = connection.cursor()
    parameters=[]
    
    QUERY = """
    UPDATE reminders 
    SET 
    """

    if reminder_title != None:
        QUERY += "reminder_title = %s, "
        parameters.append(reminder_title)
        
    if reminder_date != None:
        QUERY += "reminder_date = %s, "
        parameters.append(reminder_date)
        
    if reminder_description != None:
        QUERY += "reminder_description = %s, "
        parameters.append(reminder_description)
        
    if is_complete != None:
        QUERY += "is_complete = %s, "
        parameters.append(is_complete)
        
    if recurrence_type != None:
        QUERY += "recurrence_type = %s, "
        parameters.append(recurrence_type)
        
        
    
    QUERY = QUERY.rstrip(", ")  
    QUERY += " WHERE reminder_id = %s"
    parameters.append(reminder_id)
    cursor.execute(QUERY,tuple(parameters))        
    connection.commit()
    cursor.close()
                     
        
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