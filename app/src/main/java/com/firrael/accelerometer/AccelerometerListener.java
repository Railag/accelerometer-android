package com.firrael.accelerometer;

/**
 * Created by railag on 13.02.2018.
 */

public interface AccelerometerListener {
    void onLeft();
    void onRight();
    void onMinThreshold();
    void onUpdate(double x, double y, double z);
}