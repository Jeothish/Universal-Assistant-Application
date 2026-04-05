package com.example.myapplication
import java.text.SimpleDateFormat
import java.util.Locale

//Parses natural language date/time strings into structured data used for adding remidners via STT
object NattyParsing {
    private val parser = com.joestelmach.natty.Parser()

    /**
     * Identifies a date from natural language input
     *
     * @param input The spoken text
     * @return A formatted date string "yyyy-MM-dd"
     */
    fun extractDate(input: String) : String? {
        val groups = parser.parse(input)
        if(groups.isEmpty() || groups[0].dates.isEmpty()) return null
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(groups[0].dates[0])
    }

    /**
     * Identifies a time from natural language input
     *
     * @param input The spoken text
     * @return A formatted 24-hour time string "HH:mm:00"
     */
    fun extractTime(input: String) : String? {
        val groups = parser.parse(input)
        if(groups.isEmpty() || groups[0].dates.isEmpty()) return null
        return SimpleDateFormat("HH:mm:00", Locale.getDefault()).format(groups[0].dates[0])
    }

}