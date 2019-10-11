package com.aamba.tarabuja;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class ActivityMain extends AppCompatActivity
{
    private final String TAG = "WEATHER_APP";
    private final String API_KEY = "b51001a2373eaaa91b65b7f505ca5ca4";

    private TextView cityNameTv, weatherTv, tempTv, descTv, pressureTv, humidityTv;
    private ImageView weatherIv;

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init volley singleton
        queue = Volley.newRequestQueue(this);

        bindViews();
        downloadData();
    }

    private void bindViews()
    {
        cityNameTv = findViewById(R.id.city_name_tv);
        weatherTv = findViewById(R.id.weather_tv);
        tempTv = findViewById(R.id.temp_tv);
        descTv = findViewById(R.id.desc_tv);
        pressureTv = findViewById(R.id.pres_tv);
        humidityTv = findViewById(R.id.humid_tv);
        weatherIv = findViewById(R.id.weather_iv);
    }

    private void downloadData()
    {
        String url = String.format(
                "http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s",
                "tabriz",
                API_KEY
        );

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        populateData(response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Toast.makeText(ActivityMain.this, "Request failed!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        //add request to the queue
        queue.add(jsonObjectRequest);
    }

    private void populateData(JSONObject data)
    {
        try
        {
            String cityName = data.getString("name");

            JSONObject weatherObj = data.getJSONArray("weather").getJSONObject(0);
            String weather = weatherObj.getString("main");
            String description = weatherObj.getString("description");
            String icon = weatherObj.getString("icon");

            JSONObject mainObj = data.getJSONObject("main");
            int temp = mainObj.getInt("temp");
            int pressure = mainObj.getInt("pressure");
            int humidity = mainObj.getInt("humidity");

            cityNameTv.setText(cityName);
            weatherTv.setText(weather);
            descTv.setText(description);
            tempTv.setText(String.valueOf(temp - 273) + (char) 0x00b0);
            pressureTv.setText(pressure + " hPa");
            humidityTv.setText(humidity + "%");
            downloadImage(icon);
        }
        catch (Exception e)
        {
            Log.e(TAG, "Failed to parse response", e);
        }
    }

    private void downloadImage(String icon)
    {
        String URL = String.format("http://openweathermap.org/img/wn/%s@2x.png", icon);

        ImageRequest imageRequest = new ImageRequest(URL,
                new Response.Listener<Bitmap>()
                {
                    @Override
                    public void
                    onResponse(Bitmap response)
                    {
                        weatherIv.setImageBitmap(response);
                    }
                },
                0, 0, null,
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Toast.makeText(ActivityMain.this, "Failed to download image!", Toast.LENGTH_SHORT).show();
                    }
                });

        queue.add(imageRequest);
    }
}
