/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.activityrecognition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.content.Intent;
import android.graphics.Color;

/**
 * Activity to create a map and Car and User location markers on the map.
 */
public class CreateMapActivity extends FragmentActivity {

    /**
     * Note that this may be null if the Google Play services APK is not
     * available.
     */
    private GoogleMap mMap;

    // List of Car and User marker point
    ArrayList<LatLng> mMarkerPoints;

    // Current location
    Location location;

    // Latitude and Longitude of a location
    double mLatitude = 0;

    double mLongitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.basic_demo);

        mMarkerPoints = new ArrayList<LatLng>();

        /**
         * Intent to capture coordinates of Car and User location send by Main
         * activity
         */
        Intent intent = getIntent();

        mMarkerPoints = intent.getParcelableArrayListExtra(ACTIVITY_SERVICE);

        // Create the map if map doesn't exists
        setUpMapIfNeeded();
    }

    /**
     * When Map is activated again, render the google map
     */
    @Override
    protected void onResume() {

        super.onResume();

        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play
     * services APK is correctly installed) and the map has not already been
     * instantiated.. This will ensure that we only ever call
     * {@link #setUpMap()} once when {@link #mMap} is not null.
     */
    public void setUpMapIfNeeded() {

        // Do a null check to confirm that we have not already instantiated the
        // map.
        if (mMap == null) {

            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map)).getMap();

            // Enable MyLocation Button in the Map
            mMap.setMyLocationEnabled(true);

            // Check if we were successful in obtaining the map.
            if (mMap != null) {

                setUpMap();

            }
        }
    }

    /**
     * Create a Map and route between User and Car location
     */
    private void setUpMap() {

        try {

            if (mMarkerPoints.size() == 2) {

                // Fetch Car and User coordinates from Marker list
                LatLng carLocation = mMarkerPoints.get(0);

                LatLng curLocation = mMarkerPoints.get(1);

                // Zoom to User location on Google Map
                mMap.moveCamera(CameraUpdateFactory.newLatLng(curLocation));

                mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

                // Draw markers displaying User and Car location tags
                drawMarker(curLocation, "Me");

                drawMarker(carLocation, "Car Location");

                // Create route using car and user location
                createRoute(curLocation, carLocation);
            }

        } catch (Exception e) {
            Log.d("setUpMap failed", e.toString());

            Log.d("BasicDemo's", "STACKTRACE");

            Log.d("BasicDemo's", Log.getStackTraceString(e));
        }
    }

    /**
     * Call Google API to draw route on Google map
     */
    private void createRoute(LatLng origin, LatLng dest) {

        String url = getDirectionsUrl(origin, dest);

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
    }

    /**
     * Draw user and car markers on Map
     */
    private void drawMarker(LatLng point, String markertitle) {
        try {
            // Creating MarkerOptions
            MarkerOptions options = new MarkerOptions();

            // Setting the position of the marker
            options.position(point);

            /**
             * For the User location, the color of marker is GREEN and for the
             * Car location, the color of marker is RED.
             */
            options.title(markertitle);

            if (markertitle.equals("Car Location")) {

                options.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_RED));

            } else if (markertitle.equals("Me")) {

                options.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

            }

            // Add new marker to the Google Map Android API V2
            mMap.addMarker(options);

        } catch (Exception e) {
            Log.e("Create Map's", point.toString());

            Log.e("Create Map's", Log.getStackTraceString(e));
        }
    }

    /**
     * Get the url for obtaining route information
     */
    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + ","
                + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + parameters;

        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {

        String data = "";

        InputStream iStream = null;

        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();

            urlConnection.disconnect();
        }
        return data;
    }

    /**
     * A class to download data from Google Directions URL
     */
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        /**
         * Executes in UI thread, after the execution of doInBackground()
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /**
     * A class to parse the Google Directions in JSON format
     */
    private class ParserTask extends
            AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {

            JSONObject jObject;

            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);

                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            ArrayList<LatLng> points = null;

            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {

                points = new ArrayList<LatLng>();

                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {

                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));

                    double lng = Double.parseDouble(point.get("lng"));

                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);

                lineOptions.width(2);

                lineOptions.color(Color.RED);
            }

            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }
}
