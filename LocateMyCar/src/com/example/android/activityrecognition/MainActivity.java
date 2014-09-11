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

import com.example.android.activityrecognition.ActivityUtils.REQUEST_TYPE;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.LocationListener;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 
 * An IntentService receives activity detection updates in the background
 * so that detection can continue even if the Activity is not visible.
 */
public class MainActivity extends Activity implements  OnClickListener,
			LocationListener,
			GooglePlayServicesClient.ConnectionCallbacks,
	        GooglePlayServicesClient.OnConnectionFailedListener{

    private static final int MAX_LOG_SIZE = 5000;

    // Instantiates a log file utility object, used to log status updates
    private LogFile mLogFile;

    // Store the current request type (ADD or REMOVE)
    private REQUEST_TYPE mRequestType;

    // Holds the ListView object in the UI
    private ListView mStatusListView;

    /**
     * Holds activity recognition data, in the form of
     * strings that can contain markup
     */
    private ArrayAdapter<Spanned> mStatusAdapter;

    /**
     * Intent filter for incoming broadcasts from the
     * IntentService.
     */
    IntentFilter mBroadcastFilter;

    // Instance of a local b1roadcast manager
    private LocalBroadcastManager mBroadcastManager;

    // The activity recognition update request object
    private DetectionRequester mDetectionRequester;

    // The activity recognition update removal object
    private DetectionRemover mDetectionRemover;

    // List to store parked car and user coordinates 
    public static ArrayList<LatLng> mMarkerPoints = new ArrayList<LatLng>(2);
    
    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;
    
    //for creating high accuracy in car coordinates
    private LocationRequest mRequest;
    
    // Formats the timestamp in the log
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSZ";
    
    // A date formatter
    private SimpleDateFormat mDateFormat;
    
    private static LatLng mCarLocation;
    
    private int mCarLocationUpdateRequest = 0;
    
    private int mCurrLocationUpdateRequest = 0;
    
    private int retryLocatingCar = 0;
    
    private int retryCurrentLocation = 0;
    
    private static int check = 0;
    
    private static int bRCheck = 0;
    
    private static int bRCheckResetMap = 0;

    /**
     * Set main UI layout, get a handle to the ListView for logs, and create the broadcast
     * receiver.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the main layout
        setContentView(R.layout.activity_main);
        
        // Get a handle to the activity update list
        mStatusListView = (ListView) findViewById(R.id.log_listview);

        // Instantiate an adapter to store update data from the log
        mStatusAdapter = new ArrayAdapter<Spanned>(
                this,
                R.layout.item_layout,
                R.id.log_text
        );

        // Bind the adapter to the status list
        mStatusListView.setAdapter(mStatusAdapter);

        // Set the broadcast receiver intent filer
        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Create a new Intent filter for the broadcast receiver
        mBroadcastFilter = new IntentFilter(ActivityUtils.ACTION_REFRESH_STATUS_LIST);
        mBroadcastFilter.addCategory(ActivityUtils.CATEGORY_LOCATION_SERVICES);

        // Get detection requester and remover objects
        mDetectionRequester = new DetectionRequester(this);
        mDetectionRemover = new DetectionRemover(this);

        // Create a new LogFile object
        mLogFile = LogFile.getInstance(this);
        
        // Button to show route on Map from user to car location
        Button createRoute = (Button)findViewById(R.id.Route_button);
        createRoute.setOnClickListener(this);

        // Button to reset the map
        Button clearRoute = (Button)findViewById(R.id.clearRoute_button);
        clearRoute.setOnClickListener(this);
        
        /**
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
        
        /**
         * Location requests for 3 times after every 5seconds
         * It is used to capture Car sand User coordinates
         * It takes 15 secs to obtain exact location which
         * results in approx. 5 - 10 meters error location from
         * actual car location. Here accuracy after 15 sec is given
         * more stress, since some time car's parked on shed locations  
         */
        mRequest = LocationRequest
	    				.create()
	    				.setInterval(5000)
	    				.setNumUpdates(3)
	    				.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
	    				.setFastestInterval(1000);
     
        /**
         * Register Broadcast listener to receive message
         * when to capture car coordinates
         */     
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
   
        IntentFilter intentFilter = new IntentFilter();
        
        intentFilter.addAction(ActivityUtils.GET_CAR_lOCATION);
        
        intentFilter.addAction(ActivityUtils.RESET_MAP);
        
        bManager.registerReceiver(bReceiver, intentFilter);
        
        try {
        	mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();        
        } catch (Exception e) {
            Log.e(ActivityUtils.APPTAG, getString(R.string.date_format_error));
        }

        // Format the timestamp according to the pattern, then localize the pattern
        mDateFormat.applyPattern(DATE_FORMAT_PATTERN);
        
        mDateFormat.applyLocalizedPattern(mDateFormat.toLocalizedPattern());

    }

    public BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	
        	/**
        	 * When Recognition activity detects change in motion
        	 * from vehicle to foot movement, it sends a broadcast
        	 * message to start locating car coordinates 
        	 */
            if(intent.getAction().equals(ActivityUtils.GET_CAR_lOCATION)
            		&& (intent.getBooleanExtra("Get_Coordinates", false))) {
            	
                // If no car coordinates exists then collect car coordinates
            	if (bRCheck == 0) {
            		
            		System.out.println("Try to Capture Car Coordinates");
            		
            		// Locate car
            		startLocatingCar();
            		
            		/** 
            		 * Indicate map is not in reset state, which means
            		 * car coordinates are collected
            		 */
            		bRCheckResetMap = 0;
            		
            		// Indicates Car coordinates are collected
            		bRCheck = 1;
            		
            	}
            }
            
            /**
             * When user wants to reset or clear the map, it sends the reset
             * message  
             */
            if(intent.getAction().equals(ActivityUtils.RESET_MAP)
            		&& (intent.getBooleanExtra("Reset Map", false))) {
            	
            	if(bRCheckResetMap == 0) {
            	
            		// Clear car and user coordinates stored earlier
            		clearMarkerArray();
            		
            		// Indicate there are no car coordinates  
            		bRCheck = 0;

            		// Indicates Map is in reset state
            		bRCheckResetMap = 1;
            		
            		System.out.println("Try to Reset Map");
            		
            		LogFile.getInstance(getApplicationContext()).log("Request to Reset Map");
            	}
            }
        }    
    };
    
    private void startLocatingCar(){
    
    	   // If Google Play Services is available
        if (mLocationClient.isConnected()) {
        	
           saveCarLocation();
           
        } else {
        	
        	/**
        	 * if client is not active restart client and wait for
        	 * onConnected to callback startLocatingCar
        	 */
        	retryLocatingCar = 1;
        	
        	mLocationClient.connect();
        }
    }
    private void saveCarLocation() {
    	
    	// Get the current location
    	retryLocatingCar = 0;
    	
    	mCarLocationUpdateRequest = 1;
 
    	mLocationClient.requestLocationUpdates(mRequest, this);
    	
    	// Reset the check count
    	check = 0;
    }
    
    private void saveCurrentLocation() {
    	
    	// Get the current location
    	retryCurrentLocation = 0;
    	
    	mCurrLocationUpdateRequest = 1;
 
    	mLocationClient.requestLocationUpdates(mRequest, this);
    	
    	// Reset Check count
    	check = 0;
    }
    
    @Override
	public void onLocationChanged(Location location) {
    	
    	String timeStamp = mDateFormat.format(new Date());
    	
    	System.out.println(timeStamp + " location " + location.getLatitude() 
				+ " " +location.getLongitude());
    	
    	LogFile.getInstance(getApplicationContext()).log( timeStamp +
    			" location lat  " + location.getLatitude() 
				+ " long " +location.getLongitude() + " Size of Array "
				+getMarkerArraySize());
    	
    	/**
    	 * Increment check count for 3 updates of location
    	 * By trial and error, it is found 3 update of location
    	 * gives the most reliable and accurate coordinate from GPS
    	 * for car or user location 
    	 */
    	++check;
    	
    	/**
    	 *  When location updates are received and this location update is 3rd car
    	 *  location update, save the car coordinates in Marker array
    	 */
    	if ((mCarLocationUpdateRequest == 1) && (getMarkerArraySize() == 0) && (check == 3)) {

    		// Reset check count
    		check = 0;
    		
    		// Car location is captured, reset car location update flag
    		mCarLocationUpdateRequest = 0;			
    		
    		// Car coordinates
    		mCarLocation = new LatLng(location.getLatitude(), location.getLongitude());
    		
    		// Add Car coordinates to Marker array
    	    mMarkerPoints.add(0, mCarLocation);   
    	    
    	    // Log the car coordinates in log file for user reference 
    	    LogFile.getInstance(getApplicationContext()).log( timeStamp +
        			" Car is located at lat " + location.getLatitude() 
    				+ " long " +location.getLongitude() + " Size of Array "
    				+getMarkerArraySize());
    	    
    		System.out.println(" Car is located at " + location.getLatitude() 
    						+ location.getLongitude()); 
    	}
    	
    	if ((mCurrLocationUpdateRequest == 1) && ((getMarkerArraySize() == 1) || (getMarkerArraySize() == 2)) 
    			&& (check == 3)){
        	check = 0;
    		mCurrLocationUpdateRequest = 0;    		
    		LatLng mCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());
    		if (getMarkerArraySize() == 1) {
        	     mMarkerPoints.add(1, mCurrentLocation);        	
    		} else {
    			 mMarkerPoints.set(1, mCurrentLocation);
    		}
        	System.out.println(" My position " + location.getLatitude() 
					+ location.getLongitude());
        	LogFile.getInstance(getApplicationContext()).log( timeStamp +
        			" My position at lat " + location.getLatitude() 
        			+ " long " +location.getLongitude() + " Size of Array "
        			+getMarkerArraySize());
        	
        	Toast.makeText(getApplicationContext(), " Marker Array size " + Integer.toString(getMarkerArraySize()),
					Toast.LENGTH_LONG).show();
	    	Intent intent = new Intent(this, CreateMapActivity.class);
	    	intent.putParcelableArrayListExtra(ACTIVITY_SERVICE, mMarkerPoints);
	    	startActivity(intent);
	    	bRCheck = 0;
    	}
   }
    
    public void onClick(View v) {
        
    	if (v.getId() == R.id.Route_button) {
        
    		if ((getMarkerArraySize() == 1) || (getMarkerArraySize() == 2)) {
        	
    			if (mLocationClient.isConnected()){
    				
    				saveCurrentLocation();

    			} else {
    				
    				//if location client is not available,
    				//try to connect and the onConnected, save coordinates
    				retryCurrentLocation = 1;
    				mLocationClient.connect();				
    			}
    		} else if (getMarkerArraySize() == 0){
    			Toast.makeText(getApplicationContext(), "No Car Coordinates yet!!!",
    					Toast.LENGTH_LONG).show();	
    	    } else {
    			Toast.makeText(getApplicationContext(), "More than two Coordinates" + " Size of Array "
    					+getMarkerArraySize(),
    					Toast.LENGTH_LONG).show();
    		} 
    	} else if (v.getId() == R.id.clearRoute_button){
    			check = 0;
    			bRCheck = 0;
    			bRCheckResetMap = 0;
    			clearMarkerArray();
    			Toast.makeText(getApplicationContext(), "Map Cleared",
					Toast.LENGTH_LONG).show();
    	}
    }
    
    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {

        super.onStart();

        /**
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        System.out.println(" Location Client Connected ");
        
        mLocationClient.connect();
    }
    
    /**
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    public void onStop() {
    
    	// After disconnect() is called, the client is considered "dead".
        System.out.println(" Location Client Disconnected ");
        
        mLocationClient.disconnect();

        super.onStop();
    }
    
	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub
		if (retryLocatingCar == 1) {
			saveCarLocation();
		} else if (retryCurrentLocation == 1){
			saveCurrentLocation();
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub	
	}
    
	/**
	 *  Get number of coordinates in Marker Array
	 */
    public int getMarkerArraySize(){
    	
    	return mMarkerPoints.size();
    }
    
    /**
     *  Add coordinates in Marker Array
     */
    public int addMarker(LatLng point) {
    	
    	mMarkerPoints.add(point);
    	
    	return 1;
    }
    
    /**
     *  Clear Marker Array
     */
    public int clearMarkerArray() {
    	
    	mMarkerPoints.clear();
    	
    	return 1;
    }

    /**
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * DetectionRemover and DetectionRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case ActivityUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // If the request was to start activity recognition updates
                        if (ActivityUtils.REQUEST_TYPE.ADD == mRequestType) {

                            // Restart the process of requesting activity recognition updates
                            mDetectionRequester.requestUpdates();

                        // If the request was to remove activity recognition updates
                        } else if (ActivityUtils.REQUEST_TYPE.REMOVE == mRequestType ){

                                /*
                                 * Restart the removal of all activity recognition updates for the 
                                 * PendingIntent.
                                 */
                                mDetectionRemover.removeUpdates(
                                		mDetectionRequester.getRequestPendingIntent());

                        }
                    break;

                    // If any other result was returned by Google Play services
                    default:

                        // Report that Google Play services was unable to resolve the problem.
                        Log.d(ActivityUtils.APPTAG, getString(R.string.no_resolution));
                }

            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               Log.d(ActivityUtils.APPTAG,
                       getString(R.string.unknown_activity_request_code, requestCode));

               break;
        }
    }

    /**
     * Register the broadcast receiver and update the log of activity updates
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Register the broadcast receiver
        mBroadcastManager.registerReceiver(updateListReceiver, mBroadcastFilter);

        // Load updated activity history
        updateActivityHistory();
    }

    /**
     * Create the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       
    	MenuInflater inflater = getMenuInflater();
        
    	inflater.inflate(R.menu.menu, menu);
        
    	return true;

    }

    /**
     * Handle selections from the menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();
		
        if (itemId == R.id.menu_item_clearlog) {
		
        	// Clear the list adapter
			mStatusAdapter.clear();
			
			// Update the ListView from the empty adapter
			mStatusAdapter.notifyDataSetChanged();
			
			// Remove log files
			if (!mLogFile.removeLogFiles()) {
			    Log.e(ActivityUtils.APPTAG, getString(R.string.log_file_deletion_error));

			// Display the results to the user
			} else {

			    Toast.makeText(this, R.string.logs_deleted, Toast.LENGTH_LONG).show();
			}
			
			// Continue by passing true to the menu handler
			
			return true;
		
        } else if (itemId == R.id.menu_item_showlog) {
		
        	// Update the ListView from log files
			updateActivityHistory();
			
			// Continue by passing true to the menu handler
			return true;
		} else {
			
			return super.onOptionsItemSelected(item);
		}
    }

    /**
     * Unregister the receiver during a pause
     */
    @Override
    protected void onPause() {

        // Stop listening to broadcasts when the Activity isn't visible.
        mBroadcastManager.unregisterReceiver(updateListReceiver);

        super.onPause();
    }

    /**
     * Verify that Google Play services is available before making a request.
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // In debug mode, log the status
            Log.d(ActivityUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;

        // Google Play services was not available for some reason
        } else {

            // Display an error dialog
            GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0).show();
            return false;
        }
    }
    
    /**
     * Respond to "Start" button by requesting activity recognition updates.
     */
    public void onStartUpdates(View view) {

        // Check for Google Play services
        if (!servicesConnected()) {

            return;
        }

        /**
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = ActivityUtils.REQUEST_TYPE.ADD;

        // Pass the update request to the requester object
        mDetectionRequester.requestUpdates();
    }

    /**
     * Respond to "Stop" button by canceling updates.
     */
    public void onStopUpdates(View view) {

        // Check for Google Play services
        if (!servicesConnected()) {

            return;
        }

        /**
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = ActivityUtils.REQUEST_TYPE.REMOVE;

        // Pass the remove request to the remover object
        mDetectionRemover.removeUpdates(mDetectionRequester.getRequestPendingIntent());

        /*
         * Cancel the PendingIntent. Even if the removal request fails, canceling the PendingIntent
         * will stop the updates.
         */
        mDetectionRequester.getRequestPendingIntent().cancel();
        
    }

    /**
     * Display the activity detection history stored in the
     * log file
     */
    private void updateActivityHistory() {
        // Try to load data from the history file
        try {
            // Load log file records into the List
            List<Spanned> activityDetectionHistory = mLogFile.loadLogFile();

            // Clear the adapter of existing data
            mStatusAdapter.clear();

            // Add each element of the history to the adapter
            for (Spanned activity : activityDetectionHistory) {
                mStatusAdapter.add(activity);
            }

            // If the number of loaded records is greater than the max log size
            if (mStatusAdapter.getCount() > MAX_LOG_SIZE) {

                // Delete the old log file
                if (!mLogFile.removeLogFiles()) {

                    // Log an error if unable to delete the log file
                    Log.e(ActivityUtils.APPTAG, getString(R.string.log_file_deletion_error));
                }
            }

            // Trigger the adapter to update the display
            mStatusAdapter.notifyDataSetChanged();

        // If an error occurs while reading the history file
        } catch (IOException e) {
            Log.e(ActivityUtils.APPTAG, e.getMessage(), e);
        }
    }

    /**
     * Broadcast receiver that receives activity update intents
     * It checks to see if the ListView contains items. If it
     * doesn't, it pulls in history.
     * This receiver is local only. It can't read broadcast Intents from other apps.
     */
    BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            /**
             * When an Intent is received from the update listener IntentService, update
             * the displayed log.
             */
            updateActivityHistory();
        }
    };
}
