package com.agadimi.shakyweather

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.squareup.seismic.ShakeDetector
import org.json.JSONArray
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
            if (value != null)
                downloadData()
        }

    private lateinit var preferences: SharedPreferences
    private lateinit var location: FusedLocationProviderClient
    private var queue: RequestQueue? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsWorking = false
    private val binder = WeatherBinder()
    private var city: String? = null
    private var weather: String? = null
    private var description: String? = null
    private var temp: Int? = null
    private var pressure: Int? = null
    private var humidity: Int? = null

    override fun onCreate()
    {
        super.onCreate()

        preferences = getSharedPreferences("shaky_weather", MODE_PRIVATE)
        city = preferences.getString("city", null)

        location = LocationServices.getFusedLocationProviderClient(applicationContext)

        queue = Volley.newRequestQueue(this)
        textToSpeech = TextToSpeech(this, this)
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val shakeDetector = ShakeDetector(this)
        shakeDetector.start(sensorManager, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val notificationIntent = Intent(this, ActivityMain::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, "shaky_weather")
            .setContentTitle("Shaky weather")
            .setContentText("Shake to know the weather")
            .setSmallIcon(R.drawable.notification_icon)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1002, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder
    {
        singleVibrate()
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

    fun permissionGranted()
    {
        fetchLocation()
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
                ttsWorking = true
//                textToSpeech!!.speak(
//                    "Text to speech initialized successfully",
//                    TextToSpeech.QUEUE_FLUSH, null, null
//                )
            }
        } else
        {
            Log.e("TTS", "Initilization Failed")
        }
    }

    private fun fetchLocation()
    {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        )
        {
            return
        }
        location.lastLocation.addOnSuccessListener { lastLocation ->
            if (lastLocation == null)
                return@addOnSuccessListener

            val jsonObjectRequest = JsonArrayRequest(
                Request.Method.GET,
                "http://api.openweathermap.org/geo/1.0/reverse?lat=${lastLocation.latitude}&lon=${lastLocation.longitude}&limit=1&appid=$API_KEY",
                null,
                { response: JSONArray? ->
                    city = response?.getJSONObject(0)?.getString("name")
                    city?.run {
                        view?.let{
                            it.cityName(city)
                        }
                        preferences!!.edit()
                            .putString("city", city)
                            .apply()
                    }
                }
            ) { error: VolleyError ->
                Log.d(TAG, error.toString())
            }

            //add request to the queue
            queue!!.add(jsonObjectRequest)
        }.addOnFailureListener { exception ->
        }
    }

    override fun hearShake()
    {
        downloadData()
    }

    private fun speakWeather(city: String, weather: String, temperature: String)
    {
        speak("$weather. The temperature is $temperature degrees Celsius in $city.", true)
    }

    private fun speak(speech: String, flush: Boolean = false)
    {
        if (!ttsWorking)
        {
            vibrateError()
            return
        }
        textToSpeech?.speak(speech, if(flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, null)
    }

    private fun downloadData()
    {
        if (city.isNullOrEmpty())
        {
            speak("I don't know where you are")
            fetchLocation()
            return
        }

        singleVibrate()

        val url = String.format(
            "http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s",
            city,
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

            doubleVibrate()
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

    fun singleVibrate()
    {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else
        {
            vibrator.vibrate(100)
        }
    }

    fun doubleVibrate()
    {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
            vibrator.vibrate(vibrationEffect)
        } else
        {
            vibrator.vibrate(longArrayOf(0, 50, 50, 50), -1)
        }
    }

    fun vibrateError()
    {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else
        {
            vibrator.vibrate(500)
        }
    }
}