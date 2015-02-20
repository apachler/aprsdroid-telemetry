package org.aprsdroid.telemetrysender;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity mainActivity = ((TelemetrySender)context.getApplicationContext()).mainActivity;
        System.out.println("broadcast: " + intent.getAction());

        if (intent.getAction().equals("org.aprsdroid.telemetrysender.UPDATE_VALUES")) {
//            mainActivity.doDisplayValues();
        } else if (intent.getAction().equals("org.aprsdroid.telemetrysender.PERIODIC_SENDING")) {
//            mainActivity.doSendParams();
//            mainActivity.doSendValues();
        }
    }
}
