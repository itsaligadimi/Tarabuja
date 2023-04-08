package com.aamba.tarabuja;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class ActivityMain extends AppCompatActivity implements ActivityMainInterface
{
    private SwitchCompat bgServiceSw;
    private TextView cityNameTv, weatherTv, tempTv, descTv, pressureTv, humidityTv;
    private ImageView weatherIv;

    private SharedPreferences preferences;
    private Intent serviceIntent;
    private WeatherService service;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bgServiceSw = findViewById(R.id.bg_service_sw);
        cityNameTv = findViewById(R.id.city_name_tv);
        weatherTv = findViewById(R.id.weather_tv);
        tempTv = findViewById(R.id.temp_tv);
        descTv = findViewById(R.id.desc_tv);
        pressureTv = findViewById(R.id.pres_tv);
        humidityTv = findViewById(R.id.humid_tv);
        weatherIv = findViewById(R.id.weather_iv);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            getSystemService(NotificationManager.class).createNotificationChannel(
                    new NotificationChannel("shaky_weather", "Shaky weather", NotificationManager.IMPORTANCE_DEFAULT)
            );
        }

        preferences = getSharedPreferences("shaky_weather", Context.MODE_PRIVATE);
        bgServiceSw.setChecked(preferences.getBoolean("keep_service_alive", true));
        bgServiceSw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit()
                    .putBoolean("keep_service_alive", isChecked)
                    .apply();
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        serviceIntent = new Intent(this, WeatherService.class);
        if (!isServiceRunning(this, WeatherService.class))
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                startForegroundService(serviceIntent);
            }
            else
            {
                startService(serviceIntent);
            }
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onPause()
    {
        if (service != null)
        {
            service.setView(null);
        }

        if (isServiceRunning(this, WeatherService.class))
        {
            unbindService(serviceConnection);
        }

        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        if (!preferences.getBoolean("keep_service_alive", true))
        {
            if (isServiceRunning(this, WeatherService.class))
            {
                stopService(serviceIntent);
            }
        }
        super.onDestroy();
    }

    @Override
    public void cityName(String cityName)
    {
        cityNameTv.setText(cityName);
    }

    @Override
    public void weather(String weather)
    {
        weatherTv.setText(weather);
    }

    @Override
    public void temp(int kelvin)
    {
        tempTv.setText(String.valueOf((kelvin - 273)) + (char) 0x00b0);
    }

    @Override
    public void desc(String description)
    {
        descTv.setText(description);
    }

    @Override
    public void pressure(int pressure)
    {
        pressureTv.setText(pressure + " hPa");
    }

    @Override
    public void humidity(int humidity)
    {
        humidityTv.setText(humidity + "%");
    }

    @Override
    public void weatherImage(Bitmap icon)
    {
        weatherIv.setImageBitmap(icon);
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder)
        {
            service = ((WeatherService.WeatherBinder) binder).getService();
            service.setView(ActivityMain.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {

        }
    };

    private boolean isServiceRunning(Context context, Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

}
