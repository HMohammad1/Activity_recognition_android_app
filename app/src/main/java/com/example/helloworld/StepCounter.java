package com.example.helloworld;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class StepCounter extends Service implements SensorEventListener {
    public final static String ACTION_STEP_COUNT = "UPDATE_STEP_COUNT";
    private SensorManager sensorManager;
    private final static String CHANNEL_ID = "Step Counter";

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Log.w("StepCounterService", "No Step Counter sensor found!");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(1, buildForegroundNotification(intent));
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Step Counter Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildForegroundNotification(Intent intent) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Step Counter Service")
                .setContentText("Counting your steps in the background")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private float initialStepCount = -1; // To store the initial step count
    private float sessionSteps = 0; // Steps taken in the current session

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount == -1) {
                initialStepCount = event.values[0];
            }

            sessionSteps = event.values[0] - initialStepCount;

            Intent intent = new Intent(ACTION_STEP_COUNT);
            intent.putExtra("steps", sessionSteps);
            sendBroadcast(intent);
            writeFile((int) sessionSteps);
            Log.d("StepCounterService", "Session Steps: " + sessionSteps);
        }
    }

    public void writeFile(int fileContents) {
        DataOutputStream dos = null;
        try {
            File file = new File(getFilesDir(), "steps.txt");
            dos = new DataOutputStream(new FileOutputStream(file));
            dos.writeInt(fileContents);
            Log.d("YourTag", "Successfully wrote steps to file: " + fileContents);
        } catch (IOException e) {
            Log.e("YourTag", "File write failed: ", e);
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    Log.e("YourTag", "Error closing file: ", e);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}

