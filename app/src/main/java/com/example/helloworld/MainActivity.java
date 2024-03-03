package com.example.helloworld;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    // User permissions check
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    // enable tracking
    private boolean isServiceRunning = false;
    // orientation variables
    private OrientationEventListener orientationEventListener;
    private int ori;
    // activity recognition variables
    private boolean activityTrackingEnabled = false;
    private List<ActivityTransition> activityTransitionList;
    private PendingIntent mActivityTransitionsPendingIntent;
    private final String TRANSITIONS_RECEIVER_ACTION = "TRANSITIONS_RECEIVER_ACTION";
    // location variables
    private double lat = 0.0;
    private double lonG = 0.0;
    // variables to send to db
    private ArrayList<Float> speed = new ArrayList<>();
    private ArrayList<Float> accXAvg = new ArrayList<>();
    private ArrayList<Float> accYAvg = new ArrayList<>();
    private ArrayList<Float> accZAvg = new ArrayList<>();
    private ArrayList<Float> barometer = new ArrayList<>();
    private ArrayList<Float> gForce = new ArrayList<>();
    private ArrayList<Double> lat2 = new ArrayList<>();
    private ArrayList<Double> long2 = new ArrayList<>();
    private String activityType = "NONE";
    private String transitionType = "NONE";
    // api variables
    private RequestQueue requestQueue;
    APIConnections api = new APIConnections();
    // counter for logs
    private int logID = 0;
    // variables for home testing
    private ArrayList<String> testID = new ArrayList<>();
    private ArrayList<Float> testSpeed = new ArrayList<>();
    private ArrayList<Double> testStandardD = new ArrayList<>();
    private ArrayList<Float> testX = new ArrayList<>();
    private ArrayList<Float> testY = new ArrayList<>();
    private ArrayList<Float> testZ = new ArrayList<>();
    private ArrayList<Float> testGForce = new ArrayList<>();
    private ArrayList<Float> testBar = new ArrayList<>();
    private ArrayList<String> testCurrentVehicle = new ArrayList<>();
    private ArrayList<String> testAPIVehicle = new ArrayList<>();
    private ArrayList<String> testAPITransition = new ArrayList<>();
    private ArrayList<Double> testLat1 = new ArrayList<>();
    private ArrayList<Double> testLong1 = new ArrayList<>();
    private ArrayList<Double> testLat2 = new ArrayList<>();
    private ArrayList<Double> testLong2 = new ArrayList<>();
    private ArrayList<String> testTime = new ArrayList<>();
    private ArrayList<String> testPrediction = new ArrayList<>();
    private int testLocC = -1;
    private int testSenC = -1;
    private boolean firstLat = true;
    private boolean secondLat = false;
    private int second = -1;
    // Callback
    private String prediction = null;
    private ArrayList<String> busRoutes = new ArrayList<>();
    private ArrayList<String> busesForRoutes = new ArrayList<>();
    // Bus stop tracking
    private boolean start = false;
    private int value = 0;
    private long startTimeMillis = 0;
    private long durationMillis = System.currentTimeMillis();
    private int busStopCount = 0;
    // prediction tracking
    private int carCount;
    private int busCount;
    // steps tracking
    private float initialSteps = 0;
    private float finalSteps = 0;
    private boolean initialStepsBool = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        } else {
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 5);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 6);
        }

        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;
                ori = orientation;
            }
        };

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        } else {

            orientationEventListener.disable();
        }

        // Activity tracking part

        activityTrackingEnabled = false;

        // List of activity transitions to track.
        activityTransitionList = new ArrayList<>();

        // TODO: Add activity transitions to track.
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        mActivityTransitionsPendingIntent =
                PendingIntent.getBroadcast(MainActivity.this, 0, intent, PendingIntent.FLAG_MUTABLE);

        requestQueue = Volley.newRequestQueue(this);
    }

    @Override
    protected void onStart() {
        // Register existing broadcast receivers for sensor updates
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationService.BROADCAST_LOCATION_ACTION);
        filter.addAction(AccelerometerSensor.ACTION_ACCELERATION);
        filter.addAction(BarometerSensor.ACTION_PRESSURE);
        filter.addAction(StepCounter.ACTION_STEP_COUNT);
        filter.addAction(OrientationSensor.ACTION_ORIENTATION);
        filter.addAction(MyForegroundService.ACTION_UPDATE_TEXT);
        filter.addAction(TRANSITIONS_RECEIVER_ACTION);
        // Register for screen on and off events
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(broadcastReceiver, filter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        orientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        orientationEventListener.disable();
    }

    public void onLoad(View view) {
        try {
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open("car-19-02-23-2.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                testID.add(columns[0]);
                testSpeed.add(Float.valueOf(columns[1]));
                testStandardD.add(Double.valueOf(columns[2]));
                testX.add(Float.valueOf(columns[3]));
                testY.add(Float.valueOf(columns[4]));
                testZ.add(Float.valueOf(columns[5]));
                testGForce.add(Float.valueOf(columns[6]));
                testBar.add(Float.valueOf(columns[7]));
                testCurrentVehicle.add(columns[8]);
                testAPIVehicle.add(columns[9]);
                testAPITransition.add(columns[10]);
                testLat1.add(Double.valueOf(columns[11]));
                testLong1.add(Double.valueOf(columns[12]));
                testTime.add(columns[13]);
                testLat2.add(Double.valueOf(columns[14]));
                testLong2.add(Double.valueOf(columns[15]));
                testPrediction.add(columns[16]);

//                for (String column : columns) {
//                    System.out.print(column + ",");
//                }
                //System.out.println(columns[1]);
            }
            for (int i = 0; i < testID.size(); i++) {
                System.out.println(testID.get(i));
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onSteps(View view) {
        refreshStepCountDisplay();
    }

    public void onAPI(View view) {
        EditText edtTxtFName = findViewById(R.id.edtRadius);
        TextView tvBus = findViewById(R.id.tvBus);
        tvBus.setMovementMethod(new ScrollingMovementMethod());

        if (lat != 0.0 && lonG != 0.0) {
            BusStopCallback callback = new BusStopCallback() {
                @Override
                public void onResult(int resultLength) {
                    System.out.println(resultLength);
                    runOnUiThread(() -> {
                    });
                }
                @Override
                public void onAllBusStops(ArrayList<String> busses) {
                    for (int i=0; i < busses.size(); i++) {
                        System.out.println(busses.get(i) + " bus stop code");
                    }
                }
            };
            api.getClosestBusStops(requestQueue, lat, lonG, Integer.parseInt(edtTxtFName.getText().toString()), tvBus, callback);
        } else {
            tvBus.setText("Turn on location");
        }
    }

    public void onBtnClick(View view) {
        TextView tvLat = findViewById(R.id.tvLat);
        TextView tvLong = findViewById(R.id.tvLong);
        TextView accX = findViewById(R.id.tvX);
        TextView accY = findViewById(R.id.tvY);
        TextView accZ = findViewById(R.id.tvZ);
        TextView tvBar = findViewById(R.id.tvBar);
        TextView tvSteps = findViewById(R.id.tvSteps);
        TextView tvOrient = findViewById(R.id.tvOrient);
        TextView tvSStatus = findViewById(R.id.tvSStatus);
        TextView tvDuration = findViewById(R.id.tvDuration);
        TextView tvBusStopCount = findViewById(R.id.tvBusStopC);
        TextView tvPrediction = findViewById(R.id.tvPrediction);
        TextView tvResponse = findViewById(R.id.tvResponse);
        Button btn = findViewById(R.id.btnStartServices);
        //GetAllSensors();
        Intent locationIntent = new Intent(this, LocationService.class);
        Intent acceleromterIntent = new Intent(this, AccelerometerSensor.class);
        Intent barrometerIntent = new Intent(this, BarometerSensor.class);
        Intent stepCounter = new Intent(this, StepCounter.class);
        Intent orientation = new Intent(this, OrientationSensor.class);
        Intent test = new Intent(this, MyForegroundService.class);


        if(!activityTrackingEnabled) {
            activityTrackingEnabled = true;
            enableActivityTransitions();
        }else {
            activityTrackingEnabled = false;
            disableActivityTransitions();
        }

        if (!isServiceRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(locationIntent);
                startForegroundService(acceleromterIntent);
                startForegroundService(barrometerIntent);
                startForegroundService(stepCounter);
                startForegroundService(orientation);
                startForegroundService(test);

            }
            btn.setText("Running...");
            logID = 0;
            busStopCount = 0;
            carCount = 0;
            busCount = 0;
            start = false;
            value = 0;
            startTimeMillis = 0;
            durationMillis = System.currentTimeMillis();
            initialSteps = 0;
            finalSteps = 0;
            initialStepsBool = true;
            isServiceRunning = true;
            firstLat = true;
            secondLat = false;
        } else {
            // Stop the location service
            stopService(locationIntent);
            stopService(acceleromterIntent);
            stopService(barrometerIntent);
            stopService(stepCounter);
            stopService(orientation);
            stopService(test);

            tvLat.setText("Lat: ");
            tvLong.setText("Long: ");
            accX.setText("accX: ");
            accY.setText("accY: ");
            accZ.setText("accZ: ");
            tvBar.setText("Barometer: ");
            tvSteps.setText("Steps: ");
            tvOrient.setText("Orientation: ");
            tvSStatus.setText("Near bus stop: ");
            tvDuration.setText("Duration Stopped: ");
            tvBusStopCount.setText("Bus Stop Count: ");
            tvPrediction.setText("Prediction:");
            tvResponse.setText("Response:");
            btn.setText("Not Running");
            System.out.println("Car count is: " + carCount);
            try {
                logID ++;

                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "trip.txt");

                FileWriter writer = new FileWriter(file, true);

                String locationInfo = "Car count: " + carCount + " Bus count: " + busCount + " Near Bus stop count: " + busStopCount + " Inital step count: " + initialSteps + " Final step count: " + finalSteps + " Potential Busses: " + busesForRoutes.toString() + " BusRouteInt: " + busRouteInt + " NotBusRouteInt: " + notBusRouteInt;
                writer.write(locationInfo);
                System.out.println(locationInfo);

                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // acceleration data recording
//            try {
//                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "t.txt");
//
//                FileWriter writer = new FileWriter(file, true);
//
//                for (int i = 0; i < g.size(); i++) {
//                    String locationInfo = g.get(i) + ",     " + t.get(i) + "\n";
//                    writer.write(locationInfo);
//                }
//                //String locationInfo =
//
//                //System.out.println(locationInfo);
//
//                writer.close();
//                btn.setText("Now done");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            isServiceRunning = false;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Start activity recognition if the permission was approved.
        enableActivityTransitions();
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void enableActivityTransitions() {
        ActivityTransitionRequest request = new ActivityTransitionRequest(activityTransitionList);
        Task<Void> task = ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, mActivityTransitionsPendingIntent);
        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        activityTrackingEnabled = true;
                        TextView tvActivity = findViewById(R.id.tvActivity);
                        tvActivity.setMovementMethod(new ScrollingMovementMethod());
                        tvActivity.setText("Transitions Api was successfully registered.");

                    }
                });

        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        TextView tvActivity = findViewById(R.id.tvActivity);
                        tvActivity.setMovementMethod(new ScrollingMovementMethod());
                        tvActivity.setText("Transitions Api could NOT be registered: " + e);
                        Log.e("TAG", "Transitions Api could NOT be registered: " + e);

                    }
                });
    }

    private void disableActivityTransitions() {
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mActivityTransitionsPendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        activityTrackingEnabled = false;
                        TextView tvActivity = findViewById(R.id.tvActivity);
                        tvActivity.setMovementMethod(new ScrollingMovementMethod());
                        tvActivity.setText("Transitions successfully unregistered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("TAG","Transitions could not be unregistered: " + e);
                    }
                });
    }

    private static String toActivityString(int activity) {
        switch (activity) {
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.WALKING:
                return "WALKING";
            case DetectedActivity.IN_VEHICLE:
                return "VEHICLE";
            default:
                return "UNKNOWN";
        }
    }

    private static String toTransitionType(int transitionType) {
        switch (transitionType) {
            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                return "ENTER";
            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                return "EXIT";
            default:
                return "UNKNOWN";
        }
    }

    private void UpdateDB() {
        // Speed component
        int sC = speed.size();
        float avgS = 0;
        ArrayList<Float> speedC = new ArrayList<>(speed);
        for (int i = 0; i < speedC.size(); i++) {
            avgS += speedC.get(i);
        }
        speed.subList(0, sC).clear();
        avgS = avgS / sC;
        // Calculate the variance
        double sumOfSquares = 0.0;
        for (double s : speedC) {
            sumOfSquares += Math.pow(s - avgS, 2);
        }
        double variance = sumOfSquares / (speedC.size() - 1);
        // Calculate the standard deviation
        double standardDeviation = Math.sqrt(variance);

        // Accelerometer component
        int aXC = accXAvg.size();
        int aYC = accYAvg.size();
        int aZC = accZAvg.size();

        float avgX = 0;
        float avgY = 0;
        float avgZ = 0;

        ArrayList<Float> x = new ArrayList<>(accXAvg);
        ArrayList<Float> y = new ArrayList<>(accYAvg);
        ArrayList<Float> z = new ArrayList<>(accZAvg);

        for (int i = 0; i < x.size(); i++) {
            avgX += x.get(i);
        }
        x.subList(0, aXC).clear();
        avgX = avgX / aXC;

        for (int i = 0; i < y.size(); i++) {
            avgY += y.get(i);
        }
        y.subList(0, aYC).clear();
        avgY = avgY / aYC;

        for (int i = 0; i < z.size(); i++) {
            avgZ += z.get(i);
        }
        z.subList(0, aZC).clear();
        avgZ = avgZ / aZC;

        // transport currently on
        EditText edtTxtTransport = findViewById(R.id.edtTransport);

        // Barometer
        int barC = barometer.size();
        float avgBar = 0;
        ArrayList<Float> barCopy = new ArrayList<>(barometer);
        for (int i = 0; i < barCopy.size(); i++) {
            avgBar += barCopy.get(i);
        }
        barometer.subList(0, barC).clear();
        avgBar = avgBar / barC;

        // gForce
        int gFC = gForce.size();
        float avgGF = 0;
        ArrayList<Float> gFCopy = new ArrayList<>(gForce);
        for (int i = 0; i < gFCopy.size(); i++) {
            avgGF += gFCopy.get(i);
        }
        gForce.subList(0, gFC).clear();
        avgGF = avgGF / gFC;

        // Current time
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = now.format(formatter);


        try {
            testSenC ++;

//            JSONObject postData = new JSONObject();
//            postData.put("avgSpeed", avgS);
//            postData.put("standardD", standardDeviation);
//            postData.put("avgX", avgX);
//            postData.put("avgY", avgY);
//            postData.put("avgZ", avgZ);
//            postData.put("gForce", avgGF);
//            postData.put("barometer", avgBar);
//            postData.put("transport", edtTxtTransport.getText());
//            postData.put("activityType", activityType);
//            postData.put("transitionType", transitionType);
//            postData.put("lat", lat2.get(0));
//            postData.put("long", long2.get(0));
//            postData.put("lat2", lat2.get(1));
//            postData.put("long2", long2.get(1));
//            postData.put("time", formattedTime);
//            postData.put("prediction", prediction);
//
//            JSONObject postData2 = new JSONObject();
//            postData2.put("speed", avgS);
//            postData2.put("standardD", standardDeviation);
//            postData2.put("avgX", avgX);
//            postData2.put("avgY", avgY);
//            postData2.put("avgZ", avgZ);
//            postData2.put("gForce", avgGF);
//            postData2.put("bar", avgBar);

            JSONObject postData = new JSONObject();
            postData.put("avgSpeed", testSpeed.get(testSenC));
            postData.put("standardD", testStandardD.get(testSenC));
            postData.put("avgX", testX.get(testSenC));
            postData.put("avgY", testY.get(testSenC));
            postData.put("avgZ", testZ.get(testSenC));
            postData.put("gForce", testGForce.get(testSenC));
            postData.put("barometer", testBar.get(testSenC));
            postData.put("transport", edtTxtTransport.getText());
            postData.put("activityType", activityType);
            postData.put("transitionType", transitionType);
            postData.put("lat", testLat1.get(testSenC));
            postData.put("long", testLong1.get(testSenC));
            postData.put("lat2", testLat2.get(testSenC));
            postData.put("long2", testLong2.get(testSenC));
            postData.put("time", formattedTime);

            JSONObject postData2 = new JSONObject();
            postData2.put("speed", testSpeed.get(testSenC));
            postData2.put("standardD", testStandardD.get(testSenC));
            postData2.put("avgX", testX.get(testSenC));
            postData2.put("avgY", testY.get(testSenC));
            postData2.put("avgZ", testZ.get(testSenC));
            postData2.put("gForce", testGForce.get(testSenC));
            postData2.put("bar", testBar.get(testSenC));

            TextView tvResponse = findViewById(R.id.tvResponse);
            TextView tvPrediction = findViewById(R.id.tvPrediction);

            //api.postNewRecord(requestQueue, postData, tvResponse, logID);

            PredictionCallback callback = result -> {
                prediction = result;

                runOnUiThread(() -> {
                });
            };

            api.GetPredictions(requestQueue, postData2, tvPrediction, callback);

            if (Objects.equals(prediction, "car")) {
                carCount ++;
            } else if (Objects.equals(prediction, "bus")) {
                busCount ++;
            }

//            try {
//                logID ++;
//
//                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "trip.txt");
//
//                FileWriter writer = new FileWriter(file, true);
//
//                String locationInfo = String.format("%d,%f,%f,%f,%f,%f,%f,%f,%s,%s,%s,%f,%f,%s,%f,%f,%s\n", logID, avgS, standardDeviation, avgX, avgY, avgZ, avgGF, avgBar, edtTxtTransport.getText(), activityType, transitionType, lat2.get(0), long2.get(0), formattedTime, lat2.get(1), long2.get(1), prediction);
//                writer.write(locationInfo);
//
//                writer.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            // for testing

//            String locationInfo = String.format("%s,%f,%f,%f,%f,%f,%f,%f,%s,%s,%s,%f,%f,%s,%f,%f,%s\n", testID.get(testSenC),
//                    testSpeed.get(testSenC), testStandardD.get(testSenC), testX.get(testSenC), testY.get(testSenC),
//                    testZ.get(testSenC), testGForce.get(testSenC), testBar.get(testSenC), edtTxtTransport.getText(), activityType, transitionType,
//                    testLat1.get(testSenC), testLong1.get(testSenC), formattedTime, testLat2.get(testSenC), testLong2.get(testSenC), prediction);
//            System.out.println(locationInfo);
//            tvResponse.setText(testID.get(testSenC));

            lat2.clear();
            long2.clear();

            if (Integer.parseInt(testID.get(testSenC)) == Integer.parseInt(testID.get(testID.size() - 1))) {
                Button btn = findViewById(R.id.btnStartServices);
                btn.performClick();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

//    private void GetAllSensors() {
//        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
//        RecyclerView recyclerView = findViewById(R.id.recyclerView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        SensorAdapter adapter = new SensorAdapter(this, sensorList);
//        recyclerView.setAdapter(adapter);
//    }

    private ArrayList<Float> g = new ArrayList<>();
    private ArrayList<String> t = new ArrayList<>();
    private String busStopCode;
    private int busRouteInt;
    private int notBusRouteInt;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LocationService.BROADCAST_LOCATION_ACTION)) {


                double latV = intent.getDoubleExtra("latitude", 0);
                double longV = intent.getDoubleExtra("longitude", 0);
                float speedV = intent.getFloatExtra("speed", 0);
                speedV *= 2.23694;
                RequestQueue queue = Volley.newRequestQueue(context);
                TextView tvLat = findViewById(R.id.tvLat);
                TextView tvLong = findViewById(R.id.tvLong);
                TextView tvSpeed = findViewById(R.id.tvSpeed);
                tvLat.setText("Lat: " + latV);
                tvLong.setText("Long: " + longV);
                tvSpeed.setText(String.valueOf(speedV));
//                lat = latV;
//                lonG = longV;
//                lat = 55.931848;
//                lonG = -3.145822;
                speed.add(speedV);
                lat2.add(latV);
                long2.add(longV);

                // for testing

                if (firstLat) {
                    testLocC ++;
                    lat = testLat1.get(testLocC);
                    lonG = testLong1.get(testLocC);
                    firstLat = false;
                    //secondLat = true;
                } else {
                    second ++;
                    lat = testLat2.get(second);
                    lonG = testLong2.get(second);
                    firstLat = true;
                }



                // end of testing

                TextView tvSStatus = findViewById(R.id.tvSStatus);
                TextView tvDuration = findViewById(R.id.tvDuration);
                TextView tvBusStopCount = findViewById(R.id.tvBusStopC);
                EditText edtRadius = findViewById(R.id.edtRadius);
                EditText edtD = findViewById(R.id.edtDuration);

                start = value > 0;
                tvSStatus.setText("Near Bus stop: " + String.valueOf(start));
                //System.out.println("Start value is: " + start);
                if (start) {
                    //System.out.println("Calling if start is true: current value of sttartMilliSeconds: " + startTimeMillis);
                    startTimeMillis = System.currentTimeMillis();
                }

                BusStopCallback callback = new BusStopCallback() {
                    @Override
                    public void onResult(int resultLength) {
                        if (resultLength > 0) {
                            //System.out.println("Near bus stop");
                            value = resultLength;
                        } else {
                            value = 0;
                        }
                    }
                    @Override
                    public void onAllBusStops(ArrayList<String> busses) {
                        busStopCode = busses.get(0);
                        for (int i=0; i < busses.size(); i++) {
                            System.out.println(busses.get(i) + " bus stop code");
                        }
                    }
                };

                api.getClosestStops(requestQueue, lat, lonG, Integer.parseInt(edtRadius.getText().toString()), callback);

                if (!start) {
                    durationMillis = durationMillis - startTimeMillis;
                    durationMillis = Math.abs(durationMillis / 1000);
                    //System.out.println("Duration while 'start' was true: " + durationMillis + " seconds");
                    tvDuration.setText("Duration: " + String.valueOf(durationMillis));

                    if (durationMillis >= Integer.parseInt(edtD.getText().toString()) && durationMillis < 500) {
                        busStopCount ++;
                        tvBusStopCount.setText("Bus stop count: " + String.valueOf(busStopCount));
                    }

                    startTimeMillis = 0;
                    durationMillis = System.currentTimeMillis();
                }

                BusRouteCallback callback2 = result -> {
                    busRoutes = result;
                    for (String s: busRoutes) {
                        //System.out.println(s + " from callback2");
                    }
                    if (busRoutes.size() == 0) {
                        notBusRouteInt ++;
                        System.out.println(notBusRouteInt + " not bus route int");
                        //System.out.println("EMPTY bus routes");
                    } else {
                        busRouteInt ++;
                        System.out.println(busRouteInt + " bus route int");
                    }
                    runOnUiThread(() -> {
                    });
                };

                BusRouteCallback callback3 = result -> {
                    if (busesForRoutes.isEmpty()) {
                        busesForRoutes = result;
                        System.out.println("bussesForRoutes empty called " + busesForRoutes.toString());
                    } else {
                        List<String> filteredNewBuses = result.stream()
                                .filter(busesForRoutes::contains)
                                .collect(Collectors.toList());
                        System.out.println("filtered array called " + filteredNewBuses.toString());
                        if (!filteredNewBuses.isEmpty()) {
                            busesForRoutes.retainAll(filteredNewBuses);
                        }
                        System.out.println("new bus routes called " + busesForRoutes.toString());
                    }
                    for (String s: busesForRoutes) {
                        System.out.println(s);
                    }
//                    if (busesForRoutes.size() == 0) {
//                        System.out.println("EMPTY bus routes");
//                    }
                    runOnUiThread(() -> {
                    });
                };

                api.OnBusRoute(requestQueue, lat, lonG, 10, callback2);
                api.GetBusses(requestQueue, busStopCode, callback3);

            }

            if (intent.getAction().equals(AccelerometerSensor.ACTION_ACCELERATION)) {
                float x = intent.getFloatExtra("x", 0);
                float y = intent.getFloatExtra("y", 0);
                float z = intent.getFloatExtra("z", 0);
                float gF = intent.getFloatExtra("gForce", 0);
                // for recording acceleration
//                float gF = intent.getFloatExtra("gForce", 0);
//                String tt = intent.getStringExtra("time");
//                g.add(gF);
//                t.add(tt);
                TextView tvX = findViewById(R.id.tvX);
                TextView tvY = findViewById(R.id.tvY);
                TextView tvZ = findViewById(R.id.tvZ);
                TextView tvGF = findViewById(R.id.tvGF);
                tvX.setText("accX: " + String.valueOf(x));
                tvY.setText("accY: " + String.valueOf(y));
                tvZ.setText("accZ: " + String.valueOf(z));
                tvGF.setText("gForce: " + String.valueOf(gF));
                accXAvg.add(x);
                accYAvg.add(y);
                accZAvg.add(z);
                gForce.add(gF);
            }


            if (intent.getAction().equals(BarometerSensor.ACTION_PRESSURE)) {
                float pressure = intent.getFloatExtra("pressure", 0);
                TextView tvBar = findViewById(R.id.tvBar);
                tvBar.setText("Barometer: " + String.valueOf(pressure));
                barometer.add(pressure);
            }

            if (intent.getAction().equals(StepCounter.ACTION_STEP_COUNT)) {
                float steps = intent.getFloatExtra("steps", 0);
                TextView tvSteps = findViewById(R.id.tvSteps);
                tvSteps.setText("Steps: " + String.valueOf(steps));
                if (initialStepsBool) {
                    initialSteps = steps;
                    initialStepsBool = false;
                }
                finalSteps = steps;
            }

            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                // Screen turned on; refresh the step count
                refreshStepCountDisplay();
            }

            if (intent.getAction().equals(OrientationSensor.ACTION_ORIENTATION)) {
                String orientationStatus;
                // Determine the orientation based on angle range
                if ((ori >= 0 && ori < 45) || (ori >= 315)) {
                    orientationStatus = "Portrait";
                } else if (ori >= 45 && ori < 135) {
                    orientationStatus = "Landscape (Right)";
                } else if (ori >= 135 && ori < 225) {
                    orientationStatus = "Portrait (Upside Down)";
                } else if (ori >= 225 && ori < 315) {
                    orientationStatus = "Landscape (Left)";
                } else {
                    orientationStatus = "Unknown";
                }

                TextView tvOrient = findViewById(R.id.tvOrient);
                //tvOrient.setText("Orientation: " + azimuth + " " + " " + pitch + " " + roll + " " + orientation);
                tvOrient.setText("Orientation: " + orientationStatus);
            }

            if (intent.getAction().equals(MyForegroundService.ACTION_UPDATE_TEXT)) {
                String test = intent.getStringExtra("latitude");
                //System.out.println(test);
                UpdateDB();

            }

            if (ActivityTransitionResult.hasResult(intent)) {

                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);

                for (ActivityTransitionEvent event : result.getTransitionEvents()) {

                    String info = "Transition: " + toActivityString(event.getActivityType()) +
                            " (" + toTransitionType(event.getTransitionType()) + ")" + "   " +
                            new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());

                    TextView tvActivity = findViewById(R.id.tvActivity);
                    tvActivity.setText(info);
                    activityType = toActivityString(event.getActivityType());
                    transitionType = toTransitionType(event.getTransitionType());
                }
            }
        }
    };

    private void refreshStepCountDisplay() {
        int steps = readIntFromInternalStorage("steps.txt");
        TextView tvSteps = findViewById(R.id.tvSteps);
        tvSteps.setText("Steps: " + steps);
    }

    public int readIntFromInternalStorage(String filename) {
        DataInputStream dis = null;
        try {
            File file = new File(getFilesDir(), filename);
            dis = new DataInputStream(new FileInputStream(file));
            return dis.readInt();
        } catch (IOException e) {
            return 0;
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == 5) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {} else {
            }
        }

        if (requestCode == 6) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
            }
        }
    }

}