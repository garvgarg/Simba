package com.example.android.activityrecognition;

/**
 * Utility class defining variables used across activity recognition
 */
public final class ActivityUtils {

    // Used to track what type of request is in process
    public enum REQUEST_TYPE {
    ADD, REMOVE
    }

    public static final String APPTAG = "ActivitySample";

    /*
     * Define a request code to send to Google Play services This code is
     * returned in Activity.onActivityResult
     */
    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    // Intent actions and extras for sending information from the IntentService
    // to the Activity
    public static final String ACTION_CONNECTION_ERROR = "com.example.android.activityrecognition.ACTION_CONNECTION_ERROR";

    public static final String ACTION_REFRESH_STATUS_LIST = "com.example.android.activityrecognition.ACTION_REFRESH_STATUS_LIST";

    public static final String CATEGORY_LOCATION_SERVICES = "com.example.android.activityrecognition.CATEGORY_LOCATION_SERVICES";

    public static final String EXTRA_CONNECTION_ERROR_CODE = "com.example.android.activityrecognition.EXTRA_CONNECTION_ERROR_CODE";

    public static final String EXTRA_CONNECTION_ERROR_MESSAGE = "com.example.android.activityrecognition.EXTRA_CONNECTION_ERROR_MESSAGE";

    public static final String GET_CAR_lOCATION = "com.example.android.activityrecognition.GET_CAR_LOCATION";

    public static final String RESET_MAP = "com.example.android.activityrecognition.RESET_MAP";

    // Constants used to establish the activity update interval
    public static final int MILLISECONDS_PER_SECOND = 1000;

    public static final int DETECTION_INTERVAL_SECONDS = 1;

    public static final int DETECTION_INTERVAL_MILLISECONDS = MILLISECONDS_PER_SECOND
            * DETECTION_INTERVAL_SECONDS;

    // Shared Preferences repository name
    public static final String SHARED_PREFERENCES = "com.example.android.activityrecognition.SHARED_PREFERENCES";

    // Key in the repository for the previous activity
    public static final String KEY_PREVIOUS_ACTIVITY_TYPE = "com.example.android.activityrecognition.KEY_PREVIOUS_ACTIVITY_TYPE";

    // Constants for constructing the log file name
    public static final String LOG_FILE_NAME_PREFIX = "activityrecognition";
    public static final String LOG_FILE_NAME_SUFFIX = ".log";

    // Keys in the repository for storing the log file info
    public static final String KEY_LOG_FILE_NUMBER = "com.example.android.activityrecognition.KEY_LOG_FILE_NUMBER";
    public static final String KEY_LOG_FILE_NAME = "com.example.android.activityrecognition.KEY_LOG_FILE_NAME";

    // Values to define accuracy of location informations
    public static final int ACCURACY_FINE = 1;
    public static final int ACCURACY_HIGH = 3;

    // Values to define power state
    public static final int POWER_HIGH = 3;
    public static final int POWER_MEDIUM = 2;
    public static final int POWER_LOW = 1;

}