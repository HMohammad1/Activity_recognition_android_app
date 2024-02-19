package com.example.helloworld;

import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class APIConnections {

    private String ngrok = "https://34421d6b48d0.ngrok.app";
    public void getDistance(RequestQueue queue, double latitude, double longitude) {
        String url = ngrok + "/myapp/distance/";

        JSONObject postData = new JSONObject();
        try {
            postData.put("latitude", latitude);
            postData.put("longitude", longitude);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    try {
                        double distanceMeters = response.getDouble("distance_meters");
                        Log.d("Distance", "Distance in meters: " + distanceMeters);
                        //tv.setText(String.valueOf(distanceMeters));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.d("Error.Response", error.toString());
                });
        queue.add(jsonObjectRequest);
    }

    public void getClosestBusStops(RequestQueue queue, double latitude, double longitude, int radius, TextView tv, BusStopCallback callback) {
        String url = ngrok + "/myapp/transport/";
        StringBuilder sb = new StringBuilder();
        JSONObject postData = new JSONObject();
        try {
            postData.put("latitude", latitude);
            postData.put("longitude", longitude);
            postData.put("radius", radius);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        //System.out.println("length" + results.length());
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject busStopObject = results.getJSONObject(i);
                            int gid = busStopObject.optInt("gid");
                            String name = busStopObject.optString("name");
                            sb.append("GID: ").append(gid).append(", Name: ").append(name).append("\n");
                        }
                        tv.setText(sb.toString());
                        callback.onResult(results.length());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    tv.setText("No bus stops found");
                    Log.d("Error.Response", error.toString());
                });
        queue.add(request);
    }

    public void getClosestStops(RequestQueue queue, double latitude, double longitude, int radius, BusStopCallback callback) {
        String url = ngrok + "/myapp/transport/";
        JSONObject postData = new JSONObject();
        try {
            postData.put("latitude", latitude);
            postData.put("longitude", longitude);
            postData.put("radius", radius);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        callback.onResult(results.length());
                    } catch (JSONException e) {
                        e.printStackTrace();

                    }
                },
                error -> {
                    Log.d("Error.Response", error.toString());
                    callback.onResult(0);
                });
        queue.add(request);
    }

    public void postNewRecord(RequestQueue queue, JSONObject postData, TextView tv, Integer ID) {
        String url = ngrok + "/myapp/";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    float speed = response.optLong("speed");
                    System.out.println("Returned response" + speed);
                    tv.setText(ID + ": Response returned SUCCESS");

                }, error -> {
                    tv.setText(ID + ": Response returned FAILURE");
                });
        queue.add(request);
    }

    public void GetPredictions(RequestQueue queue, JSONObject postData, TextView tv, PredictionCallback callback) {
        String url = ngrok + "/myapp/predict/";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    try {
                        String prediction = response.getString("prediction");
                        //System.out.println("Predicted activity type was: " + prediction);
                        tv.setText("Prediction: " + prediction);
                        callback.onResult(prediction);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }, error -> {
            tv.setText("Prediction encountered error");
        });
        queue.add(request);
    }
}
