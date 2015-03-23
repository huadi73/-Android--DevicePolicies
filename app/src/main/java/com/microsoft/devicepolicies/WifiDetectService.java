package com.microsoft.devicepolicies;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by huadi on 2015/3/22.
 */
public class WifiDetectService extends Service {
    String TAG = "WifiDetectService";

    BroadcastReceiver awaitIPAddress = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                if (intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE) == SupplicantState.COMPLETED) {
                    //WiFi is associated
                    WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wi = wifiManager.getConnectionInfo();
                    if (wi != null) {
                        // Wifi info available (should be, we are associated)
                        if (wi.getIpAddress() != 0) {
                            Log.d(TAG, "IP " + wi.getSSID());
                            // Lucky us, we already have an ip address.
                            // This happens when a connection is complete, e.g. after rekeying
                            if (wi.getBSSID().equals("2h2f")) {
                                // ... Do your stuff here
                                // ...
                                // ...
                            }
                        } else {
                            // No ip address yet, we need to wait...
                            // Battery friendly method, using events
                            if (awaitIPAddress == null) {
                                awaitIPAddress = new BroadcastReceiver() {
                                    @Override
                                    public void onReceive(Context ctx, Intent in) {
                                        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                                        WifiInfo wi = wifiManager.getConnectionInfo();
                                        if (wi != null) {
                                            if (wi.getIpAddress() != 0) {
                                                Log.d(TAG, wi.getSSID());
                                                if (wi.getBSSID().equals("2h2f")) {
                                                    // ... Do your stuff here
                                                    // ...
                                                    // ...
                                                }
                                            }
                                        } else {
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
                } else {
                    // wifi connection not complete, release ip address receiver if registered
                    if (awaitIPAddress != null) {
                        context.unregisterReceiver(awaitIPAddress);
                        awaitIPAddress = null;
                    }
                }
            }
        }
    };


}
