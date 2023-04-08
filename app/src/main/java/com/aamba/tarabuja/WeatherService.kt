package com.aamba.tarabuja

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.squareup.seismic.ShakeDetector
import org.json.JSONObject
import java.util.*

class WeatherService : Service(), OnInitListener, ShakeDetector.Listener
{
    private val TAG = "TARABUJA"
    private val API_KEY = "3391172795f1e72315abb1740b751a93"

    var view: ActivityMainInterface? = null
        set(value)
        {
            field = value
            if(value != null)
                downloadData()
        }
    private var queue: RequestQueue? = null
    private var textToSpeech: TextToSpeech? = null
    private val binder = WeatherBinder()
    private var city: String? = null
    private var weather: String? = null
    private var description: String? = null
    private var temp: Int? = null
    private var pressure: Int? = null
    private var humidity: Int? = null


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        queue = Volley.newRequestQueue(this)
        textToSpeech = TextToSpeech(this, this)
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val shakeDetector = ShakeDetector(this)
        shakeDetector.start(sensorManager, SensorManager.SENSOR_DELAY_GAME)

        val notificationIntent = Intent(this, ActivityMain::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, "shaky_weather")
            .setContentTitle("Shaky weather")
            .setContentText("Shake to know the weather")
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1002, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder
    {
        return binder
    }

    inner class WeatherBinder : Binder()
    {
        fun getService(): WeatherService = this@WeatherService
    }

    override fun onDestroy()
    {
        if (textToSpeech != null)
        {
            textToSpeech!!.stop()
            textToSpeech!!.shutdown()
        }
        super.onDestroy()
    }

    override fun onInit(status: Int)
    {
        if (status == TextToSpeech.SUCCESS)
        {
            val result = textToSpeech!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED
            )
            {
                Log.e("TTS", "Language is not supported")
            } else
            {
                textToSpeech!!.speak(
                    "Text to speech initialized successfully",
                    TextToSpeech.QUEUE_FLUSH, null, null
                )
            }
        } else
        {
            Log.e("TTS", "Initilization Failed")
        }
    }

    override fun hearShake()
    {
        downloadData();
    }

    private fun speakWeather(city: String, weather: String, temperature: String)
    {
        if (textToSpeech?.isSpeaking == true)
        {
            textToSpeech?.stop()
        }
        val speech = "$weather. The temperature is $temperature degrees Celsius in $city."
        textToSpeech?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun downloadData()
    {
        val url = String.format(
            "http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s",
            "tabriz",
            API_KEY
        )
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response: JSONObject? -> populateData(response!!) }
        ) { error: VolleyError ->
            Log.d(TAG, error.toString())
        }

        //add request to the queue
        queue!!.add(jsonObjectRequest)
    }


    private fun populateData(data: JSONObject)
    {
        try
        {
            city = data.getString("name")
            val weatherObj = data.getJSONArray("weather").getJSONObject(0)
            weather = weatherObj.getString("main")
            description = weatherObj.getString("description")
            val icon = weatherObj.getString("icon")
            val mainObj = data.getJSONObject("main")
            temp = mainObj.getInt("temp")
            pressure = mainObj.getInt("pressure")
            humidity = mainObj.getInt("humidity")

            view?.let {
                it.cityName(city)
                it.weather(weather)
                it.desc(description)
                it.temp(temp!!)
                it.pressure(pressure!!)
                it.humidity(humidity!!)
                downloadImage(icon)
            }

            speakWeather(city!!, weather!!, (temp!! - 273).toString())
        } catch (e: Exception)
        {
            Log.e(TAG, "Failed to parse response", e)
        }
    }

    private fun downloadImage(icon: String)
    {
        val URL = String.format("http://openweathermap.org/img/wn/%s@2x.png", icon)
        val imageRequest = ImageRequest(
            URL,
            { response -> view?.weatherImage(response) },
            0, 0, null
        ) {
            Log.d(TAG, it.toString())
        }
        queue!!.add(imageRequest)
    }
}