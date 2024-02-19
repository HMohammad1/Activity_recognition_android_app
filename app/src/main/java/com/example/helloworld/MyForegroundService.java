package com.example.helloworld;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class MyForegroundService extends Service {

    private final static String CHANNEL_ID = "ForegroundServiceChannel";
    private volatile boolean isRunning = true;
    private Thread backgroundThread;

    public final static String ACTION_UPDATE_TEXT = "UPDATE_TEXT";


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Foreground Service")
                    .setContentText("Service is running in foreground")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentIntent(pendingIntent)
                    .build();
        }

        startForeground(1, notification);

        performBackgroundTask();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void performBackgroundTask() {
        backgroundThread = new Thread(() -> {
            while (isRunning) {

                Intent intent = new Intent(ACTION_UPDATE_TEXT);
                intent.putExtra("sample", "sample");
                sendBroadcast(intent);

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    isRunning = false;
                }
            }
        });
        backgroundThread.start();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (backgroundThread != null) backgroundThread.interrupt(); // Interrupt the thread if it's sleeping
        stopForeground(true);
    }
}

