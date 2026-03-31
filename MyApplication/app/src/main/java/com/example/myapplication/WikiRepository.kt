package com.example.myapplication

import android.util.Log

class WikiRepository(private val wikiDao: WikiDao) {

    suspend fun getWikiData(topic: String): String{
        val cacheExpiry = 7L * 24 * 60 * 60 * 1000
        wikiDao.deleteOldCache(cacheExpiry)

        val processedTopic = topic.lowercase().trim()

        val cached = wikiDao.getWikiByTopic(processedTopic)

        if(cached != null && (System.currentTimeMillis() - cached.timestamp) < cacheExpiry){
            Log.d("WIKI_CACHING","Using database data for $processedTopic")
            return "Article: ${cached.title}\n\n${cached.summary}"
        }

        Log.d("WIKI_CACHING","Using Wikipedia API to fetch data for $processedTopic")
        val result = WikiSearch.search(topic)

        if (result != null){
            val title = result.getString("title")
            val summary = result.getString("summary")
            val url = result.getString("url")

            wikiDao.insertWiki(WikiCache(
                topic = processedTopic,
                title = title,
                summary = summary,
                url = url
            ))
            return "Article: $title\n\n$summary"
        }

        else{
            return "No Wikipedia results found for $topic."
        }
    }
}