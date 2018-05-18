package com.firrael.accelerometer;

import android.os.Bundle;

import com.firrael.accelerometer.AccelerometerTestFragment;
import com.firrael.accelerometer.base.BasePresenter;

import java.util.ArrayList;

import icepick.State;


/**
 * Created by railag on 26.02.2018.
 */

public class AccelerometerTestPresenter extends BasePresenter<AccelerometerTestFragment> {
    private final static String TAG = AccelerometerTestFragment.class.getSimpleName();

    @State
    long userId;

    @State
    ArrayList<Double> x;

    @State
    ArrayList<Double> y;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

       /* RConnectorService service = App.restService();

        restartableLatestCache(REQUEST_RESULTS_ACCELEROMETER,
                () -> service.sendAccelerometerResults(userId, x, y)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread()),
                AccelerometerTestFragment::onSuccess,
                AccelerometerTestFragment::onError);*/
    }

    public void save(ArrayList<Double> x, ArrayList<Double> y) {
/*        this.userId = User.get(App.getMainActivity()).getId();
        this.x = x;
        this.y = y;

        Log.i(TAG, "Upload:" + x);

        start(REQUEST_RESULTS_ACCELEROMETER);*/
    }
}