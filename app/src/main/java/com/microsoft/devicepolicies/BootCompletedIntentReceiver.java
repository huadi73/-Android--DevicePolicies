package com.microsoft.devicepolicies;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by huadi on 2015/5/12.
 */
public class BootCompletedIntentReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
//            Intent pushIntent = new Intent(context, WifiDetectService.class);
//            context.startService(pushIntent);
        }
    }
}
