package com.firrael.accelerometer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by Railag on 03.06.2016.
 */
public class Utils {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public static float dp2px(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(App.PREFS, Context.MODE_PRIVATE);
    }

    public static void hideKeyboard(Activity act) {
        if (act != null && act.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) act.getSystemService(Activity.INPUT_METHOD_SERVICE);

            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(act.getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    public static void showKeyboard(Context context, View focus) {
        if (context != null && focus != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);

            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(focus, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private final static double NANO = 1000000000;

    public static double calcTime(long startTime) {
        long currTime = System.nanoTime();
        long diff = currTime - startTime;

        return diff / NANO;
    }

    public static boolean canWrite(Activity activity) {
        return ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private final static String SENSOR_TAG = "Sensor";
    public final static float THRESHOLD_ACCELEROMETER_MAX = 7.0f;
    private final static float THRESHOLD_ACCELEROMETER_MIN = 1.0f;

    public static SensorEventListener registerSensor(Context context, AccelerometerListener listener, int degreesMin, int degreesMax) {
        if (context == null || listener == null) {
            return null;
        }

        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        final double thresholdMin = calculateThreshold(degreesMin);
        final double thresholdMax = calculateThreshold(degreesMax);

        SensorEventListener sensorEventListener = new SensorEventListener() {
            boolean sensorLock;

            @Override
            public void onSensorChanged(SensorEvent event) {
                // In this example, alpha is calculated as t / (t + dT),
                // where t is the low-pass filter's time-constant and
                // dT is the event delivery rate.

                final float alpha = 0.8f;

                float[] gravity = new float[3];

                // Isolate the force of gravity with the low-pass filter.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                float[] linear_acceleration = new float[3];

                // Remove the gravity contribution with the high-pass filter.
                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];

                listener.onUpdate(linear_acceleration[0], linear_acceleration[1], linear_acceleration[2]);

                float currentX = linear_acceleration[0];

                if (currentX < thresholdMin && currentX > -thresholdMin) {
                    listener.onMinThreshold();
                    sensorLock = false;
                }

                if (sensorLock) {
                    return;
                }

                if (currentX > thresholdMax) {
                    sensorLock = true;
                    Log.i(SENSOR_TAG, "onLeft");
                    listener.onLeft();
                } else if (currentX < -thresholdMax) {
                    sensorLock = true;
                    Log.i(SENSOR_TAG, "onRight");
                    listener.onRight();
                }

                //    float delta = Math.abs(previousX - currentX);

                //Log.i(TAG, "x: " + linear_acceleration[0] + " y: " + linear_acceleration[1] + " z: " + linear_acceleration[2]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI);

        return sensorEventListener;
    }

    private static double calculateThreshold(int degrees) { // 3 degrees - ?
                                                         // 90 degrees - MAX_THRESHOLD (7.0)
        double thresholdValue =  degrees * THRESHOLD_ACCELEROMETER_MAX / 90.0;
        Log.i(SENSOR_TAG, "Threshold value: " + thresholdValue);
        return thresholdValue;
    }

    public static void unregisterSensor(Context context, SensorEventListener listener) {
        if (context == null || listener == null) {
            return;
        }

        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        sensorManager.unregisterListener(listener);
    }
}