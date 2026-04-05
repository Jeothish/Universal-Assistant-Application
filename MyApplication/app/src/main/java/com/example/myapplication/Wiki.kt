import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WikiSearch {

    private fun fetch(url: String): String { // fetch connection
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "MyWikiApp/1.0")
        connection.connectTimeout = 10000 //timeout incase it takes too long for wiki to respond
        connection.readTimeout = 10000
        return connection.inputStream.bufferedReader().readText()
    }

    private fun getSummary(title: String): String { //only get extract from wiki article
        val params = "action=query&prop=extracts&exintro=1&explaintext=1&format=json&titles=${URLEncoder.encode(title, "UTF-8")}" //url parameters
        val data = JSONObject(fetch("https://en.wikipedia.org/w/api.php?$params"))
        val pages = data.getJSONObject("query").getJSONObject("pages")
        val page = pages.getJSONObject(pages.keys().next())
        return page.optString("extract", "") //return extract
    }

    suspend fun search(query: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            // First try opensearch (exact title match)
            val params = "action=opensearch&search=${URLEncoder.encode(query, "UTF-8")}&limit=5&format=json"
            val data = JSONArray(fetch("https://en.wikipedia.org/w/api.php?$params"))
            val titles = data.getJSONArray(1)
            val urls = data.getJSONArray(3)

            // if o.s finds something use that
            if (titles.length() > 0) {
                val title = titles.getString(0)
                val url = urls.getString(0)
                val summary = getSummary(title).take(500)

                // if summary is empt /ghost article skip to fulltext search (search in articles
                if (summary.isBlank()) {
                    Log.d("Wiki", "falling back to fulltext...")
                } else {
                    Log.d("Wiki", "Opensearch found: $title")
                    return@withContext JSONObject().apply {
                        put("title", title)
                        put("summary", summary)
                        put("url", url)
                    }
                }
            }

            // fallback to fultext
            Log.d("Wiki", "Opensearch found nothing, trying fulltext search...")
            val searchParams = "action=query&list=search&srsearch=${URLEncoder.encode(query, "UTF-8")}&srlimit=1&format=json"
            val searchData = JSONObject(fetch("https://en.wikipedia.org/w/api.php?$searchParams"))
            val searchResults = searchData.getJSONObject("query").getJSONArray("search")

            if (searchResults.length() == 0) return@withContext null

            val bestTitle = searchResults.getJSONObject(0).getString("title")
            val summary = getSummary(bestTitle).take(500)
            val url = "https://en.wikipedia.org/wiki/${URLEncoder.encode(bestTitle, "UTF-8")}"
            Log.d("Wiki", "Fulltext search found: $bestTitle")

            JSONObject().apply {
                put("title", bestTitle)
                put("summary", summary)
                put("url", url)
            }

        } catch (e: Exception) {
            Log.e("Wiki", "Failed: ${e.message}", e)
            null
        }
    }
}