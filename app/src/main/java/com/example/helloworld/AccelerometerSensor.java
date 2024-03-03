package com.example.helloworld;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.app.Notification;
import android.app.PendingIntent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class AccelerometerSensor extends Service implements SensorEventListener {
    public final static String ACTION_ACCELERATION = "UPDATE_ACCELERATION";
    private SensorManager sensorManager;
    private final static String CHANNEL_ID = "AccelerometerService";

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
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
                    "Foreground Service Channel",
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
                .setContentTitle("Accelerometer Service")
                .setContentText("Collecting accelerometer data in background")
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

    //private ArrayList<Float> g = new ArrayList<>();
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // acceleration magnitude
            float m = (float) Math.sqrt(x * x + y * y + z * z);
            //g.add(m);
            // Convert to g-force
            float gForce = m / 9.81f;

            //WriteFile(x, y, z, gForce, m);

            LocalTime now = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String formattedTime = now.format(formatter);

            Intent intent = new Intent(ACTION_ACCELERATION);
            intent.putExtra("x", x);
            intent.putExtra("y", y);
            intent.putExtra("z", z);
            intent.putExtra("gForce", m);
            //intent.putExtra("time", formattedTime);
            sendBroadcast(intent);
        }
    }

    private void WriteFile(float x, float y, float z, float gForce, float m) {
        try {

            LocalTime now = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String formattedTime = now.format(formatter);

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "acceleration.txt");

            FileWriter writer = new FileWriter(file, true);

            //String locationInfo = x + "," + y + "," + z + "," + gForce + "," + m + "," + formattedTime + "\n";
            String locationInfo = gForce + ",       " + m + ",      " + formattedTime + "\n";
            writer.write(locationInfo);
            //System.out.println(locationInfo);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}



