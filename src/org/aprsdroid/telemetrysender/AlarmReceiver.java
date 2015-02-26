package org.aprsdroid.telemetrysender;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity mainActivity = TelemetrySender.mainActivity;

        if (intent.getAction().equals("org.aprsdroid.telemetrysender.UPDATE_DISPLAY")) {
            mainActivity.doDisplayValues();
        }
        if (intent.getAction().equals("org.aprsdroid.telemetrysender.PERIODIC_SENDING")) {
            if (mainActivity.sharedPrefs.getBoolean("pref_interval_perform_sending", false)) {
                mainActivity.doSendParams();
                mainActivity.doSendValues();
            }
        }
    }
}
