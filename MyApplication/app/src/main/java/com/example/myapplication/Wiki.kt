import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WikiSearch {

    private fun fetch(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "MyWikiApp/1.0")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        return connection.inputStream.bufferedReader().readText()
    }

    private fun getSummary(title: String): String {
        val params = "action=query&prop=extracts&exintro=1&explaintext=1&format=json&titles=${URLEncoder.encode(title, "UTF-8")}"
        val data = JSONObject(fetch("https://en.wikipedia.org/w/api.php?$params"))
        val pages = data.getJSONObject("query").getJSONObject("pages")
        val page = pages.getJSONObject(pages.keys().next())
        return page.optString("extract", "")
    }

    suspend fun search(query: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val params = "action=opensearch&search=${URLEncoder.encode(query, "UTF-8")}&limit=5&format=json"
            val data = JSONArray(fetch("https://en.wikipedia.org/w/api.php?$params"))

            val titles = data.getJSONArray(1)
            val urls = data.getJSONArray(3)

            if (titles.length() == 0) return@withContext null

            val title = titles.getString(0)
            val url = urls.getString(0)
            val summary = getSummary(title).take(500)

            Log.d("WikiSearch", "Found: $title")

            JSONObject().apply {
                put("title", title)
                put("summary", summary)
                put("url", url)
            }
        } catch (e: Exception) {
            Log.e("WikiSearch", "Failed: ${e.message}", e)
            null
        }
    }
}