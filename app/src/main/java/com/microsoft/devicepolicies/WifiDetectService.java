package com.microsoft.devicepolicies;


import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by huadi on 2015/3/22.
 */
public class WifiDetectService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener
{
    String TAG = "WifiDetectService";

    String wifiName = "2h2f";
    boolean isConnectCompanyWifi = false;

    DevicePolicyManager mDPM;
    ComponentName mDeviceAdminSample;
    BroadcastReceiver awaitIPAddress = null;

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
    private LocationRequest locationRequest;
    private Location mCurrentLocation;


    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "onCreate");

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
//        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand");

        // Connect the client.
        if (!mResolvingError)
        {
            Log.d(TAG, "GoogleApiClient Connect");
            mGoogleApiClient.connect(); // Connect the client.
        }
        else
        {
            Log.d(TAG, "GoogleApiClient mResolvingError");
        }

        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDeviceAdminSample = new ComponentName(this, MyDeviceAdminReceiver.class);
        //mDeviceAdminSample = (ComponentName) intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN);
        if (!mDPM.isAdminActive(mDeviceAdminSample))
        {
            // try to become active – must happen here in this activity, to get result
            Log.d(TAG, "NO AdminActive");
        }
        else
        {
            // Already is a device administrator, can do security operations now.
            Log.d(TAG, "AdminActive");
//            mDPM.setCameraDisabled(mDeviceAdminSample, true);
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            registerReceiver(receiver, filter);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        mGoogleApiClient.disconnect();
        unregisterReceiver(receiver);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind");
        return null;
    }

    private void SetCameraDisable(boolean isToDisable)
    {
        mDPM.setCameraDisabled(mDeviceAdminSample, isToDisable);
        Log.d(TAG, "Camera Disabled = " + isToDisable);

        isConnectCompanyWifi = isToDisable;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION))
            {
                if (intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE) == SupplicantState.COMPLETED)
                {
                    //WiFi is associated
                    WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (wifiInfo != null)
                    {
                        // Wifi info available (should be, we are associated)
                        if (wifiInfo.getIpAddress() != 0)
                        {
                            Log.d(TAG, "Already Connected " + wifiInfo.getSSID());
                            // Lucky us, we already have an ip address.
                            // This happens when a connection is complete, e.g. after rekeying
                            if (wifiInfo.getSSID().equals("\"" + wifiName + "\""))
                            {
                                SetCameraDisable(true);
                            }
                            else
                            {
                                Log.d(TAG, "NOT eq wifi " + wifiName + ", Current: " + wifiInfo.getSSID());
                                SetCameraDisable(false);
                            }
                        }
                        else
                        {
                            // No ip address yet, we need to wait...
                            // Battery friendly method, using events
                            if (awaitIPAddress == null)
                            {
                                awaitIPAddress = new BroadcastReceiver()
                                {
                                    @Override
                                    public void onReceive(Context ctx, Intent in)
                                    {
                                        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                                        if (wifiInfo != null)
                                        {
                                            if (wifiInfo.getIpAddress() != 0)
                                            {
                                                Log.d(TAG, "Now Connected " + wifiInfo.getSSID());
                                                if (wifiInfo.getSSID().equals("\"" + wifiName + "\""))
                                                {
                                                    SetCameraDisable(true);
                                                }
                                                else
                                                {
                                                    Log.d(TAG, "NOT eq wifi " + wifiInfo.getSSID());
                                                    SetCameraDisable(false);
                                                }
                                            }
                                        }
                                        else
                                        {
                                            SetCameraDisable(false);
                                            Log.d(TAG, "NO WiFi");
                                            ctx.unregisterReceiver(this);
                                            awaitIPAddress = null;
                                        }
                                    }
                                };
                                // We register a new receiver for connectivity events
                                // (getting a new IP address for example)
                                context.registerReceiver(awaitIPAddress, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                            }
                        }
                    }
                }
                else
                {
                    // wifi connection not complete, release ip address receiver if registered
                    if (awaitIPAddress != null)
                    {
                        SetCameraDisable(false);
                        Log.d(TAG, "wifi connection not complete");
                        context.unregisterReceiver(awaitIPAddress);
                        awaitIPAddress = null;
                    }
                }
            }
        }
    };


    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(TAG, "onConnected");
        Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (currentLocation != null && currentLocation.getTime() > 20000)
        {
            mCurrentLocation = currentLocation;
            Log.d(TAG, String.valueOf(mCurrentLocation.getLatitude()) + ", " + String.valueOf(mCurrentLocation.getLongitude()));



            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(new LatLng(25.043523, 121.577258));
            builder.include(new LatLng(25.044802, 121.575536));
            builder.include(new LatLng(25.043441, 121.574077));
            builder.include(new LatLng(25.044171, 121.576506));
            LatLngBounds bound = builder.build();

            if (bound.contains(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())))
            {
                Log.d(TAG, "123, " + bound.getCenter());
            }
            else
                Log.d(TAG, "456, " + bound.getCenter());


        }
        else
        {
            fusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
            // Schedule a Thread to unregister location listeners
            Executors.newScheduledThreadPool(1).schedule(new Runnable() {
                @Override
                public void run() {
                    fusedLocationProviderApi.removeLocationUpdates(mGoogleApiClient, WifiDetectService.this);
                }
            }, 60000, TimeUnit.MILLISECONDS);
            Log.d(TAG, "requestLocationUpdates");
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.d(TAG, "onConnectionFailed");
        if (mResolvingError) {
            Log.d("","Already attempting to resolve an error");
            return;
        } else if (connectionResult.hasResolution()) {
            Log.d(TAG, "connectionResult.hasResolution()");
        } else {
            mResolvingError = true;
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
        mCurrentLocation = location;
        Toast.makeText(getApplicationContext(), String.valueOf(mCurrentLocation.getLatitude()) + ", " + String.valueOf(mCurrentLocation.getLongitude()), Toast.LENGTH_SHORT).show();


    }
}
