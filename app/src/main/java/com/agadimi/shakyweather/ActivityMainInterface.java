package com.agadimi.shakyweather;

import android.graphics.Bitmap;

public interface ActivityMainInterface
{
    void cityName(String cityName);
    void weather(String weather);
    void temp(int temp);
    void desc(String desc);
    void pressure(int pressure);
    void humidity(int humidity);
    void weatherImage(Bitmap icon);
}
