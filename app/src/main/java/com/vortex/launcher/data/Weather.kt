package com.vortex.launcher.data
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Weather(val temp: Int, val text: String)

/** Open-Meteo: без API-ключа. Геокодинг по названию города + текущая погода. */
object WeatherApi {
    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        return c.inputStream.bufferedReader().use { it.readText() }
    }
    fun fetch(city: String, fahrenheit: Boolean): Weather? = runCatching {
        val g = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(city, "UTF-8")}&count=1&language=ru"))
            .getJSONArray("results").getJSONObject(0)
        val unit = if (fahrenheit) "fahrenheit" else "celsius"
        val cur = JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=${g.getDouble("latitude")}&longitude=${g.getDouble("longitude")}&current=temperature_2m,weather_code&temperature_unit=$unit"))
            .getJSONObject("current")
        Weather(Math.round(cur.getDouble("temperature_2m")).toInt(), text(cur.getInt("weather_code")))
    }.getOrNull()

    private fun text(c: Int) = when (c) {
        0 -> "Ясно"; 1, 2 -> "Переменная облачность"; 3 -> "Облачно"; 45, 48 -> "Туман"
        in 51..57 -> "Морось"; in 61..67 -> "Дождь"; in 71..77 -> "Снег"; in 80..82 -> "Ливень"
        in 85..86 -> "Снегопад"; in 95..99 -> "Гроза"; else -> "Облачно"
    }
}
