package com.example.helloworld;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import android.Manifest;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LocationService extends Service {

    public static final String BROADCAST_LOCATION_ACTION = "LocationAction";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Make sure location permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Parameters for the location request such as accuracy and interval
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build();
            // Call the location callback to retrieve results
            LocationServices.getFusedLocationProviderClient(this)
                    .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            Log.d("LocationService", "Location permission not granted");
        }
        // Starts the foreground service and makes the notification
        startForeground(1, getNotification());
        return START_STICKY;
    }

    private Notification getNotification() {
        String channelId = "location_service_channel";
        String channelName = "Location Service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Location Service")
                .setContentText("Service is running...")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();
    }

    // Int used as an ID to make sure location is updating
    int g = 0;
    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult != null) {
                // Get the latest location
                Location location = locationResult.getLastLocation();
                g++;
                assert location != null;
                // Send location data to the main activity
                Intent intent = new Intent(BROADCAST_LOCATION_ACTION);
                intent.putExtra("latitude", location.getLatitude());
                intent.putExtra("longitude", location.getLongitude());
                intent.putExtra("speed", (location.getSpeed()));
                intent.putExtra("g", g);
                sendBroadcast(intent);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove location updates to stop receiving location changes
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(locationCallback);
    }

}

