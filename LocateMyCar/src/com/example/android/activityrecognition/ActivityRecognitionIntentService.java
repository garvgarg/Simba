/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.support.v4.content.LocalBroadcastManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Service that receives ActivityRecognition updates. It receives updates in the
 * background, even if the main Activity is not visible.
 */

public class ActivityRecognitionIntentService extends IntentService implements
        LocationListener {

    // Formats the timestamp in the log
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSZ";

    // Delimits the timestamp from the log info
    private static final String LOG_DELIMITER = ";;";

    // A date formatter
    private SimpleDateFormat mDateFormat;

    // Store the app's shared preferences repository
    private SharedPreferences mPrefs;

    // Represents strength of vehicle mode
    private static int vehicle_mode_weight = 0;

    // Represents strength of walking mode
    private static int onFoot_mode_weight = 0;

    public ActivityRecognitionIntentService() {
        // Set the label for the service's background thread
        super("ActivityRecognitionIntentService");
        System.out.println("Intent Activity initiated");

    }

    /**
     * Called when a new activity detection update is available.
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        // Get a handle to the repository
        mPrefs = getApplicationContext().getSharedPreferences(
                ActivityUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        // Get a date formatter, and catch errors in the returned timestamp
        try {
            mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        } catch (Exception e) {
            Log.e(ActivityUtils.APPTAG, getString(R.string.date_format_error));
        }

        // Format the timestamp according to the pattern, then localize the
        // pattern
        mDateFormat.applyPattern(DATE_FORMAT_PATTERN);
        mDateFormat.applyLocalizedPattern(mDateFormat.toLocalizedPattern());

        // If the intent contains an update
        if (ActivityRecognitionResult.hasResult(intent)) {

            // Get the update
            ActivityRecognitionResult result = ActivityRecognitionResult
                    .extractResult(intent);

            // Get the most probable activity from the list of activities in the
            // update
            DetectedActivity mostProbableActivity = result
                    .getMostProbableActivity();

            // Get the confidence percentage for the most probable activity
            int confidence = mostProbableActivity.getConfidence();

            // Get the type of activity
            int activityType = mostProbableActivity.getType();

            // Get the previous type, otherwise return the "unknown" type
            int previousType = mPrefs.getInt(
                    ActivityUtils.KEY_PREVIOUS_ACTIVITY_TYPE,
                    DetectedActivity.UNKNOWN);

            String timeStamp = mDateFormat.format(new Date());

            // If confidence is greater than 70 store the activity
            if (confidence >= 70) {
                // Store the type
                Editor editor = mPrefs.edit();

                editor.putInt(ActivityUtils.KEY_PREVIOUS_ACTIVITY_TYPE,
                        activityType);

                editor.commit();
            }

            if (activityType == DetectedActivity.STILL && (confidence >= 70) &&
            // if (activityType == DetectedActivity.IN_VEHICLE && (confidence >=
            // 70) &&
                    (vehicle_mode_weight < 100)) {

                // Clear coordinates on map as soon user is in vehicle mode
                if (vehicle_mode_weight == 0) {
                    System.out.println("Request send to reset Map");

                    Intent resetMap = new Intent(ActivityUtils.RESET_MAP);

                    resetMap.putExtra("Reset Map", true);

                    LocalBroadcastManager.getInstance(this).sendBroadcast(
                            resetMap);
                }

                ++vehicle_mode_weight;

                onFoot_mode_weight = 0;

                LogFile.getInstance(getApplicationContext()).log(
                        timeStamp + LOG_DELIMITER + "In Vehicle Activity "
                                + getNameFromType(activityType) + " "
                                + vehicle_mode_weight);
            }

            if (activityType == DetectedActivity.TILTING && (confidence >= 50)
                    &&
                    // if (activityType == DetectedActivity.ON_FOOT &&
                    // (confidence >= 40) &&
                    (onFoot_mode_weight < 100)) {
                ++onFoot_mode_weight;

                LogFile.getInstance(getApplicationContext()).log(
                        timeStamp + LOG_DELIMITER + "On Foot Activity "
                                + getNameFromType(activityType) + " "
                                + onFoot_mode_weight);

            }

            if ((confidence >= 50) && (activityType != previousType)) {

                // Get the current log file or create a new one, then log the
                // activity
                LogFile.getInstance(getApplicationContext()).log(
                        timeStamp + LOG_DELIMITER + " Activity changed "
                                + getNameFromType(previousType) + " to "
                                + getNameFromType(activityType));
            }

            if ((onFoot_mode_weight >= 1) && (vehicle_mode_weight >= 1)) {

                vehicle_mode_weight = 0;

                onFoot_mode_weight = 0;

                // Get the current log file or create a new one, then log the
                // activity
                LogFile.getInstance(getApplicationContext())
                        .log(timeStamp
                                + LOG_DELIMITER
                                + "CAR IS PARKED. GPS COORDINATES ARE COLLECTED.");

                System.out.println("Request send to Capture Car Location");

                Intent getCarLocation = new Intent(
                        ActivityUtils.GET_CAR_lOCATION);

                getCarLocation.putExtra("Get_Coordinates", true);

                LocalBroadcastManager.getInstance(this).sendBroadcast(
                        getCarLocation);

            }
        }
    }

    /**
     * Map detected activity types to strings
     * 
     * @param activityType
     *            The detected activity type
     * @return A user-readable name for the type
     */
    private String getNameFromType(int activityType) {
        switch (activityType) {
        case DetectedActivity.IN_VEHICLE:
            return "in_vehicle";
        case DetectedActivity.ON_BICYCLE:
            return "on_bicycle";
        case DetectedActivity.ON_FOOT:
            return "on_foot";
        case DetectedActivity.STILL:
            return "still";
        case DetectedActivity.UNKNOWN:
            return "unknown";
        case DetectedActivity.TILTING:
            return "tilting";
        }
        return "unknown";
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
    }
}
