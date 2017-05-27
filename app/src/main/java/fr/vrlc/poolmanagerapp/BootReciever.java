package fr.vrlc.poolmanagerapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by rlalanne on 23/05/2017.
 */

public class BootReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Register your reporting alarms here.
            PoolTempService.registerAlarm(context);
        }
    }
}
