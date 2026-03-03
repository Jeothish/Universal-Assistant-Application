package com.example.myapplication

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

object Database {

    fun getConnection(): Connection? {
        return try {
            val databaseUrl = System.getenv("DATABASE_URL")
            val connection = DriverManager.getConnection(databaseUrl)
            println("Connection successful!")
            connection
        } catch (e: Exception) {
            println("Failed to connect: ${e.message}")
            null
        }
    }

    fun getCitiesDb(city: String, connection: Connection): Pair<Double, Double>? {
        val query = """
            SELECT latitude, longitude
            FROM cities
            WHERE cityname = ?
        """.trimIndent()

        connection.prepareStatement(query).use { statement ->
            statement.setString(1, city)

            val resultSet: ResultSet = statement.executeQuery()

            return if (resultSet.next()) {
                val latitude = resultSet.getDouble("latitude")
                val longitude = resultSet.getDouble("longitude")
                Pair(latitude, longitude)
            } else {
                null
            }
        }
    }

    fun addCityDb(city: String, latitude: Double, longitude: Double, connection: Connection) {
        val query = """
            INSERT INTO cities (cityname, latitude, longitude)
            VALUES (?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(query).use { statement ->
            statement.setString(1, city)
            statement.setDouble(2, latitude)
            statement.setDouble(3, longitude)

            statement.executeUpdate()
        }

        connection.commit()
    }
}