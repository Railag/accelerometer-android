package com.firrael.accelerometer;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.firrael.accelerometer.base.BaseFragment;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import nucleus.factory.RequiresPresenter;

/**
 * Created by railag on 26.02.2018.
 */

@RequiresPresenter(AccelerometerTestPresenter.class)
public class AccelerometerTestFragment extends BaseFragment<AccelerometerTestPresenter> implements AccelerometerListener {

    private final static String TAG = AccelerometerTestFragment.class.getSimpleName();

    private final static int PACKAGE_SIZE = 5;

    private final static int REQUEST_ENABLE_BT = 101;

    private Handler handler;

    private ConnectionState connectionState = ConnectionState.NO_CONNECTIONS;

    private int counter = 0;
    private ArrayList<Double> x = new ArrayList<>(), y = new ArrayList<>();

    private DisplayMetrics displayMetrics;

    private BluetoothAdapter bluetoothAdapter;

    @BindView(R.id.accelerometer_circle)
    ImageView accelerometerCircle;

    private SensorEventListener sensorListener;

    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;
    private ConnectedThread connectedThread2;

    private ValueAnimator yAnimator;
    private ValueAnimator xAnimator;

    private float currentX = -1f;
    private float currentY = -1f;

    private boolean connected = false;

    public static AccelerometerTestFragment newInstance() {

        Bundle args = new Bundle();

        AccelerometerTestFragment fragment = new AccelerometerTestFragment();
        fragment.setHasOptionsMenu(true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected String getTitle() {
        return getString(R.string.accelerometerTestTitle);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_test_accelerometer;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.bluetooth, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bluetooth:
                initBluetooth();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        handler = new Handler();

        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(receiver, filter);

        nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(2);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void initBluetooth() {
        startLoading();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.e(TAG, "Bluetooth is not supported!");
            stopLoading();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            stopLoading();
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.i(TAG, "Paired device: " + deviceName + " with MAC: " + deviceHardwareAddress);
            }
        }

        // start bluetooth host
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.i(TAG, "Discovered device: " + deviceName + " with MAC: " + deviceHardwareAddress);
            }
        }
    };

    private void upload() {
        //    startLoading();

        ArrayList<Double> xTmp = new ArrayList<>(x), yTmp = new ArrayList<>(y);
        //    getPresenter().save(xTmp, yTmp);

        // write to bluetooth
        writeToBluetooth(xTmp);
        writeToBluetooth(yTmp);

        x.clear();
        y.clear();

        counter = 0;
    }

    private void writeToBluetooth(ArrayList<Double> tmp) {
        if (connectedThread != null) { // if bluetooth socket is active
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                for (Double element : tmp) {
                    out.writeDouble(element);
                    Log.i(TAG, String.valueOf(element));
                }

                byte[] bytes = baos.toByteArray();
                connectedThread.write(bytes);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (connectedThread2 != null) { // if bluetooth socket is active
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                for (Double element : tmp) {
                    out.writeDouble(element);
                    Log.i(TAG, String.valueOf(element));
                }

                byte[] bytes = baos.toByteArray();
                connectedThread2.write(bytes);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorListener = Utils.registerSensor(getActivity(), this, 1, 3);
    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.unregisterSensor(getActivity(), sensorListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (getActivity() != null) {
            getActivity().unregisterReceiver(receiver);
        }

        if (xAnimator != null) {
            xAnimator.cancel();
        }

        if (yAnimator != null) {
            yAnimator.cancel();
        }

        stopBluetooth();
    }

    @Override
    public void onLeft() {
    }

    @Override
    public void onRight() {
    }

    @Override
    public void onMinThreshold() {
    }

    @Override
    public void onUpdate(double x, double y, double z) {
        if (connected) {
            update(x, y);
        }
    }

    //private DecimalFormat df = new DecimalFormat("#.00");
    private NumberFormat nf;

    private void update(double x, double y) {
        if (counter >= PACKAGE_SIZE) {
            int realWidth = displayMetrics.widthPixels; // 1920
            int realHeight = displayMetrics.heightPixels; // 1080

            float[] xValues = new float[this.x.size()];
            for (int i = 0; i < this.x.size(); i++) {
                xValues[i] = this.x.get(i).floatValue();
            }

            xAnimator = ValueAnimator.ofFloat(xValues);
            xAnimator.setInterpolator(new LinearInterpolator());
            xAnimator.setDuration(500);
            xAnimator.addUpdateListener(animation -> {

                float value = (float) animation.getAnimatedValue();

                float adjustedX = adjust(value, realWidth, true);
                if (currentX == -1f) {
                    if (accelerometerCircle != null) {
                        accelerometerCircle.setX(adjustedX);
                    }
                    currentX = adjustedX;
                } else {
                    float medianX = (currentX + adjustedX) / 2;
                    if (accelerometerCircle != null) {
                        accelerometerCircle.setX(medianX);
                    }
                    currentX = medianX;
                }
            });

            float[] yValues = new float[this.y.size()];
            for (int i = 0; i < this.y.size(); i++) {
                yValues[i] = this.y.get(i).floatValue();
            }

            yAnimator = ValueAnimator.ofFloat(yValues);
            yAnimator.setInterpolator(new LinearInterpolator());
            yAnimator.setDuration(500);
            yAnimator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();

                float adjustedY = adjust(value, realHeight, false);
                if (currentY == -1f) {
                    if (accelerometerCircle != null) {
                        accelerometerCircle.setY(adjustedY);
                    }
                    currentY = adjustedY;
                } else {
                    float medianY = (currentY + adjustedY) / 2;
                    if (accelerometerCircle != null) {
                        accelerometerCircle.setY(medianY);
                    }
                    currentY = medianY;
                }
            });

            xAnimator.start();
            yAnimator.start();

            upload();
        } else {
            this.x.add(Double.valueOf(nf.format(x)));
            this.y.add(Double.valueOf(nf.format(y)));
            counter++;
        }
    }

    private float adjust(double paramToAdjust, double maxValue, boolean inverse) { // 7 - xResolutionMax (e.g. 1920), 3 - y?
        //    7-1920
        //    paramToAdjust-x
        double value = (maxValue / 2) + (float) (paramToAdjust * maxValue / (Utils.THRESHOLD_ACCELEROMETER_MAX * 2));
        return inverse ? (float) Math.abs(maxValue - value) : (float) value;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                initBluetooth();
            }
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Accelerometer client",
                        UUID.fromString("0cbb85aa-7951-41a6-b891-b2ee53960860"));
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }

            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    stopBluetooth();
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private void manageMyConnectedSocket(BluetoothSocket socket) {
        getActivity().runOnUiThread(() -> {
            stopLoading();
            connected = true;
            accelerometerCircle.setVisibility(View.VISIBLE);
        });

        if (connectionState.equals(ConnectionState.NO_CONNECTIONS)) {
            connectedThread = new ConnectedThread(socket);
            connectionState = ConnectionState.FIRST_CONNECTION;
        } else if (connectionState.equals(ConnectionState.FIRST_CONNECTION)) {
            connectedThread2 = new ConnectedThread(socket);
            connectionState = ConnectionState.TWO_CONNECTIONS;
        } else if (connectionState.equals(ConnectionState.SECOND_CONNECTION)) {
            connectedThread = new ConnectedThread(socket);
            connectionState = ConnectionState.TWO_CONNECTIONS;
        }

        if (!connectionState.equals(ConnectionState.TWO_CONNECTIONS)) {
            if (connectedThread == null || connectedThread2 == null) {
                acceptThread = new AcceptThread();
                acceptThread.start();
            }
        }
    }


    public static final int MESSAGE_READ = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int MESSAGE_TOAST = 2;

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = handler.obtainMessage(
                            MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = handler.obtainMessage(
                        MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data");

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        handler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                handler.sendMessage(writeErrorMsg);

                stopBluetooth();
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private void stopBluetooth() {
        if (acceptThread != null) {
            acceptThread.cancel();
        }

        if (connectedThread != null) {
            connectedThread.cancel();
        }

        if (connectedThread2 != null) {
            connectedThread2.cancel();
        }

        connected = false;

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (accelerometerCircle != null) {
                    accelerometerCircle.setVisibility(View.GONE);
                }

                connectionState = ConnectionState.NO_CONNECTIONS;

                initBluetooth();
            });
        }

        Log.i(TAG, "Bluetooth connection stopped");

    }
}