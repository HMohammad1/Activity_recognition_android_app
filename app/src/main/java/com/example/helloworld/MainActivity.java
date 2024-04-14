package com.example.helloworld;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

    // Activity recognition API code gotten from :https://developer.android.com/codelabs/activity-recognition-transition#0
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check permissions have been granted
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

        // Get the phones orientation from the sensor
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;
                ori = orientation;
            }
        };

        // Check if orientation can be received or not
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        } else {
            orientationEventListener.disable();
        }

        // Activity tracking part

        activityTrackingEnabled = false;

        // List of activity transitions to track.
        activityTransitionList = new ArrayList<>();

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

        // Global queue for API calls using Volley
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
        // unregister all receivers
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // when app is back in the main view enable orientation sensor
        orientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // when app is in the background disable orientation sensor
        orientationEventListener.disable();
    }

    // Used for testing on emulator. Needs to be pressed to load in the data to be spoofed before algorithm starts running
    public void onLoad(View view) {
        try {
            // All files saved in the asset manager
            AssetManager assetManager = getAssets();
            // Replace the name with the journey to be ran (use the -copy ones)
            InputStream is = assetManager.open("bus-19-02-23-1-copy.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            // Populate the data to be emulated
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
                //testPrediction.add(columns[16]);

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

    // refresh the steps if they haven't been updated
    public void onSteps(View view) {
        refreshStepCountDisplay();
    }

    // Button to get the closest bus stops to the user
    public void onAPI(View view) {
        //EditText edtTxtFName = findViewById(R.id.edtRadius);
        TextView tvBus = findViewById(R.id.tvBus);
        tvBus.setMovementMethod(new ScrollingMovementMethod());
        // make sure users location is on first
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
                    tvBus.setText(busses.toString());
                }
            };
            api.getClosestBusStops(requestQueue, lat, lonG, 17, tvBus, callback);
        } else {
            tvBus.setText("Turn on location");
        }
    }

    // Main button to start the algorithm
    public void onBtnClick(View view) {
        // get all the text fields to update
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
        // The intents to be used in the broadcast receiver
        Intent locationIntent = new Intent(this, LocationService.class);
        Intent acceleromterIntent = new Intent(this, AccelerometerSensor.class);
        Intent barrometerIntent = new Intent(this, BarometerSensor.class);
        Intent stepCounter = new Intent(this, StepCounter.class);
        Intent orientation = new Intent(this, OrientationSensor.class);
        Intent test = new Intent(this, MyForegroundService.class);

        // start / stop activity tracking when clicked
        // also enable / disable the activity recognition API
        if(!activityTrackingEnabled) {
            activityTrackingEnabled = true;
            enableActivityTransitions();
        }else {
            activityTrackingEnabled = false;
            disableActivityTransitions();
        }

        // Once started, start all foreground services
        if (!isServiceRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(locationIntent);
                startForegroundService(acceleromterIntent);
                startForegroundService(barrometerIntent);
                startForegroundService(stepCounter);
                startForegroundService(orientation);
                startForegroundService(test);

            }
            // reset all fields when initially starting the app
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
            // stop all the serves when the algorithm is stopped
            // Stop the location service
            stopService(locationIntent);
            stopService(acceleromterIntent);
            stopService(barrometerIntent);
            stopService(stepCounter);
            stopService(orientation);
            stopService(test);
            // reset all the fields to be empty
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
            //tvPrediction.setText("Prediction:");
            tvResponse.setText("Response:");
            btn.setText("Not Running");
            System.out.println("Car count is: " + carCount);
            // Log the final results to a file
            try {
                logID ++;

                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "trip.txt");

                FileWriter writer = new FileWriter(file, true);

                String locationInfo = "Car count: " + carCount + " Bus count: " + busCount + " Near Bus stop count: " + busStopCount + " Inital step count: " + initialSteps + " Final step count: " + finalSteps + " Potential Busses: " + busesForRoutes.toString() + " BusRouteInt: " + busRouteInt + " NotBusRouteInt: " + notBusRouteInt + " Vehicle Type: " + vehicleType.toString();
                writer.write(locationInfo);
                System.out.println(locationInfo);

                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Used for recording the magnitude data (deprecated)
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
        // Enable activity tracking and sign up for specific activities
        ActivityTransitionRequest request = new ActivityTransitionRequest(activityTransitionList);
        Task<Void> task = ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, mActivityTransitionsPendingIntent);
        task.addOnSuccessListener(
                result -> {
                    activityTrackingEnabled = true;
                    TextView tvActivity = findViewById(R.id.tvActivity);
                    tvActivity.setMovementMethod(new ScrollingMovementMethod());
                    tvActivity.setText("Transitions Api was successfully registered.");

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

    // Disables activity recognition API
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

    // Helper method for activity recognition API
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

    // Helper method for activity recognition API
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

    // Update the DB by posting a record
    private void UpdateDB() {
        // average all the sensor data

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
            // Counter use for testing, uncomment when using the actual app
            testSenC ++;

            // Un comment when using the actual to post data

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

            // end of uncomment

            // posting data using the emulated data

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

            // API call to post a record
            //api.postNewRecord(requestQueue, postData, tvResponse, logID);

            // callback to get prediction result for deprecated API call
            PredictionCallback callback = result -> {
                prediction = result;

                runOnUiThread(() -> {
                });
            };

            // deprecated API call for old predictions
            //api.GetPredictions(requestQueue, postData2, tvPrediction, callback);

            // count the number of times bus or car is predicted, deprecated
            if (Objects.equals(prediction, "car")) {
                carCount ++;
            } else if (Objects.equals(prediction, "bus")) {
                busCount ++;
            }

            // write the data to a file, uncomment when not testing

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

            // end of uncomment

            // writing data to the console for testing data
            // for testing

            String locationInfo = String.format("%s,%f,%f,%f,%f,%f,%f,%f,%s,%s,%s,%f,%f,%s,%f,%f,%s\n", testID.get(testSenC),
                    testSpeed.get(testSenC), testStandardD.get(testSenC), testX.get(testSenC), testY.get(testSenC),
                    testZ.get(testSenC), testGForce.get(testSenC), testBar.get(testSenC), edtTxtTransport.getText(), activityType, transitionType,
                    testLat1.get(testSenC), testLong1.get(testSenC), formattedTime, testLat2.get(testSenC), testLong2.get(testSenC), prediction);
            System.out.println(locationInfo);
            tvResponse.setText(testID.get(testSenC));

            lat2.clear();
            long2.clear();

            logID ++;

            // automatically stop the algorithm when at the end of the journey
            if (Integer.parseInt(testID.get(testSenC)) == Integer.parseInt(testID.get(testID.size() - 1))) {
                Button btn = findViewById(R.id.btnStartServices);
                btn.performClick();
            }

            // end of testing

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Used for testing on the emulator, depicted
    private void UpdateDB2() {
        try {
            logID ++;

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "trip.txt");

            FileWriter writer = new FileWriter(file, true);

            if (lat2.size() > 0) {
                String locationInfo = String.format("%d,%s,%s,%f,%f,%f,%f\n", logID, activityType, transitionType, lat2.get(0), long2.get(0), lat2.get(1), long2.get(1));
                writer.write(locationInfo);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        lat2.clear();
        long2.clear();

    }

    // USed to record the magnitude values and write it to a file
    private void UpdateDB3() {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "t.txt");
            FileWriter writer = new FileWriter(file, true);
            // Amount of data to slice once values are written to the txt file
            int x = accSensorValue.size();
            int y = accTime.size();
            for (int i = 0; i < x; i++) {
                String locationInfo = accSensorValue.get(i) + ",     " + accTime.get(i) + "\n";
                writer.write(locationInfo);
            }
            // Clear the list with the values that have been written
            accSensorValue.subList(0, x).clear();
            accTime.subList(0, y).clear();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Gets a list of all the sensors

//    private void GetAllSensors() {
//        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
//        RecyclerView recyclerView = findViewById(R.id.recyclerView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        SensorAdapter adapter = new SensorAdapter(this, sensorList);
//        recyclerView.setAdapter(adapter);
//    }

    // variables used for the APIs
    private ArrayList<Float> accSensorValue = new ArrayList<>();
    private ArrayList<String> accTime = new ArrayList<>();
    private ArrayList<String> vehicleType = new ArrayList<>();
    private String busStopCode;
    private int busRouteInt;
    private int notBusRouteInt;
    private int busScore = 0;
    private int carScore = 0;
    private String vType;
    private int startID = 0;
    private boolean vehicleBool = true;

    private String checkVehicleType(int busStopCount, int onBusRouteInt, int notOnBusRouteInt) {
        // Weights for being at a bus stop / not being on a bus route (only cars are not on bus routes hence higher weight)
        int busStopWeight = 50;
        int notOnBusRouteWeight = 100;

        busScore = busStopCount * busStopWeight;
        carScore = notOnBusRouteInt * notOnBusRouteWeight;

        busScore += onBusRouteInt;

        if (busStopCount == 0) {
            // ID to keep track of how many times this is called (every 5 seconds)
            startID ++;
            // If the length is less than 10 (45s) then leave as undefined
            if (startID < 10) {
                System.out.println("startID < 10, prediction:   Undefined");
                return "Undefined";
            }
            // If the user is still at a bus stop and has not left we can assume they are on a bus
            // since they haven't left a bus stop yet.
            if (startID < 40 && start) {
                return "Bus";
            }
            // Otherwise assume they are in a car
            System.out.println("BusStop count is 0, prediction:   Car");
            return "Car";
        }

        int bus = 0;
        int car = 0;
        // Count the number of times the algorithm detects busses / cars
        for (String type : vehicleType) {
            if ("Bus".equals(type)) {
                bus++;
            } else if ("Car".equals(type)) {
                car++;
            }
        }
        // If the number of bus stops stopped at is low then check to see how many times the
        // algorithm predicted bus / car, if its predominantly car then stick to car.
        if (busStopCount < 2) {
            if (car >= 30) {
                System.out.println("BusStop count is < 2, prediction:   Car");
                return "Car";
            }
        }

        // otherwise use the heuristic values
        if (busScore > carScore) {
            System.out.println("Current vehicle prediction:   Bus");
            return "Bus";
        } else {
            System.out.println("Current vehicle prediction:   Car");
            return "Car";
        }
    }

    // Broadcast receiver
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // intent for location
            if (intent.getAction().equals(LocationService.BROADCAST_LOCATION_ACTION)) {
                double latV = intent.getDoubleExtra("latitude", 0);
                double longV = intent.getDoubleExtra("longitude", 0);
                float speedV = intent.getFloatExtra("speed", 0);
                speedV *= 2.23694;
                TextView tvLat = findViewById(R.id.tvLat);
                TextView tvLong = findViewById(R.id.tvLong);
                TextView tvSpeed = findViewById(R.id.tvSpeed);
                tvLat.setText("Lat: " + latV);
                tvLong.setText("Long: " + longV);
                tvSpeed.setText(String.valueOf(speedV));
                // uncomment when using actual app
//                lat = latV;
//                lonG = longV;

                // add data to arrays so they can be recorded
                speed.add(speedV);
                lat2.add(latV);
                long2.add(longV);

                // for testing

                if (firstLat) {
                    testLocC ++;
                    lat = testLat1.get(testLocC);
                    lonG = testLong1.get(testLocC);
                    firstLat = false;
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
                TextView tvBus = findViewById(R.id.tvBus);
                TextView tvBusRouteInt = findViewById(R.id.tvBusRouteInt);
                TextView tvNotBusRouteInt = findViewById(R.id.tvNotBusRouteInt);
                TextView tvCurrentVehicle = findViewById(R.id.tvCurrentVehicle);
                tvBus.setMovementMethod(new ScrollingMovementMethod());
                //EditText edtRadius = findViewById(R.id.edtRadius);
                //EditText edtD = findViewById(R.id.edtDuration);

                // check if near bus stop
                start = value > 0;
                tvSStatus.setText("Near Bus stop: " + String.valueOf(start));
                //System.out.println("Start value is: " + start);
                // if near a bus stop then start timer
                if (start) {
                    //System.out.println("Calling if start is true: current value of sttartMilliSeconds: " + startTimeMillis);
                    startTimeMillis = System.currentTimeMillis();
                }


                BusStopCallback callback = new BusStopCallback() {
                    @Override
                    // if bus stop length is more then 0 then you are near a bus stop
                    public void onResult(int resultLength) {
                        if (resultLength > 0) {
                            //System.out.println("Near bus stop");
                            value = resultLength;
                        } else {
                            value = 0;
                        }
                    }
                    @Override
                    // Get the bus stop code for the bus stop
                    public void onAllBusStops(ArrayList<String> busses) {
                        // Get the first bus stop code from the list
                        busStopCode = busses.get(0);
                        for (int i=0; i < busses.size(); i++) {
                            System.out.println(busses.get(i) + " bus stop code");
                        }
                    }
                };

                api.getClosestStops(requestQueue, lat, lonG, 17, callback);

                // Once you leave a bus stop check how long you stayed at it
                if (!start) {
                    durationMillis = durationMillis - startTimeMillis;
                    durationMillis = Math.abs(durationMillis / 1000);
                    //System.out.println("Duration while 'start' was true: " + durationMillis + " seconds");
                    tvDuration.setText("Duration: " + String.valueOf(durationMillis));

                    // if the duration is more than 20 seconds then record this bus stop
                    if (durationMillis >= 20 && durationMillis < 500) {
                        busStopCount ++;
                        tvBusStopCount.setText("Bus stop count: " + String.valueOf(busStopCount));
                    }

                    //reset timers
                    startTimeMillis = 0;
                    durationMillis = System.currentTimeMillis();
                }

                // callback used to check if on a bus route and to filter the bus stops based on the routes
                BusRouteCallback callback2 = result -> {
                    busRoutes = result;
                    for (String s: busRoutes) {
                        //System.out.println(s + " from callback2");
                    }
                    if (busRoutes.size() == 0) {
                        notBusRouteInt ++;
                        System.out.println(notBusRouteInt + " not bus route int");
                        tvNotBusRouteInt.setText("NotBusRouteInt: " + String.valueOf(notBusRouteInt));
                        //System.out.println("EMPTY bus routes");
                    } else {
                        busRouteInt ++;
                        System.out.println(busRouteInt + " bus route int");
                        tvBusRouteInt.setText("BusRouteInt: " + String.valueOf(busRouteInt));
                        List<String> filteredNewBuses = result.stream()
                                .filter(busesForRoutes::contains)
                                .collect(Collectors.toList());
                        System.out.println("filtered array called from callback 2 " + filteredNewBuses.toString());
                        if (!filteredNewBuses.isEmpty()) {
                            busesForRoutes.retainAll(filteredNewBuses);
                        }
                        System.out.println("new bus routes called from callback 2 " + busesForRoutes.toString());
                        tvBus.setText(busesForRoutes.toString());
                    }
                    runOnUiThread(() -> {
                    });
                };

                // get the buses you are on and filter based on bus stops
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
                        tvBus.setText(busesForRoutes.toString());
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

                // get the vehicle predictions
                vType = checkVehicleType(busStopCount, busRouteInt, notBusRouteInt);
                vehicleType.add(vType);
                tvCurrentVehicle.setText("CurrentVehicle: " + vType);
            }

            // accelerometer intent
            if (intent.getAction().equals(AccelerometerSensor.ACTION_ACCELERATION)) {
                float x = intent.getFloatExtra("x", 0);
                float y = intent.getFloatExtra("y", 0);
                float z = intent.getFloatExtra("z", 0);
                //float gF = intent.getFloatExtra("gForce", 0);
                // for recording acceleration
                float gF = intent.getFloatExtra("gForce", 0);
                String tt = intent.getStringExtra("time");
                accSensorValue.add(gF);
                accTime.add(tt);
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

            // barometer intent
            if (intent.getAction().equals(BarometerSensor.ACTION_PRESSURE)) {
                float pressure = intent.getFloatExtra("pressure", 0);
                TextView tvBar = findViewById(R.id.tvBar);
                tvBar.setText("Barometer: " + String.valueOf(pressure));
                barometer.add(pressure);
            }

            // step counter intent
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

            // check if the app is on the main screen or not
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                // Screen turned on; refresh the step count
                refreshStepCountDisplay();
            }

            // orientation sensor intent
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

            // intent that tells when to post a record to the DB (comes in every 10s)
            if (intent.getAction().equals(MyForegroundService.ACTION_UPDATE_TEXT)) {
                String test = intent.getStringExtra("sample");
                //System.out.println(test);
                UpdateDB();
                //UpdateDB3();
                //System.out.println("Foreground services called now");
                //UpdateDB2();

            }

            // Activity recognition API intent to receive updates when a new activity is detected
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

    // refresh the step count
    private void refreshStepCountDisplay() {
        int steps = readIntFromInternalStorage("steps.txt");
        TextView tvSteps = findViewById(R.id.tvSteps);
        tvSteps.setText("Steps: " + steps);
    }

    // reads the data from internal storage based on a filename
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

    // request permissions
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