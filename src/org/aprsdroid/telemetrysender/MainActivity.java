package org.aprsdroid.telemetrysender;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
    /**
     * constants / statics
     */

    // constants for sensor configuration - http://developer.android.com/reference/android/hardware/Sensor.html
    // APRS telemetry specification only support 5 analogue channels but we can display more inside the textview!!
    // maximal length of channel names are 6, 6, 5, 5, 4
/*    static final int SENSOR_TYPES[] = {
            Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_AMBIENT_TEMPERATURE, Sensor.TYPE_RELATIVE_HUMIDITY, Sensor.TYPE_PRESSURE, Sensor.TYPE_GRAVITY, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD
    };*/
    static final String SENSOR_NAMES[] = {
            "Laccel", "Atemp", "RH", "Press", "Grav", "Gyro", "Mfield"
    };
    static final String SENSOR_UNITS[] = {
            "m/s^2", "deg.C", "%rh", "hPa", "m/s^2", "rad/s", "uT"
    };

    // interval definitions
    static final private long INTERVAL_ONE_SECOND = 1000;
    static final private long INTERVAL_TEN_SECONDS = INTERVAL_ONE_SECOND * 10;
    static final private long INTERVAL_ONE_MINUTE = INTERVAL_TEN_SECONDS * 6;


    /**
     * variables
     */

    int seq_no = 0;

    // preferences
    SharedPreferences sharedPrefs;

    // sensor service
    private Intent sensorServiceIntent;
    Sensor sensors[];
    float values[][];
    // select which sensor axis we want to send over APRS for sensors with coordinate system (0=x, 1=y, 2=z)
    int sensor_axis = 2;

    // alarm manager
    AlarmManager alarmManager;
    Intent alarmIntent;
    PendingIntent pendingIntent;

    // UI elements
    TextView mInfoText = null;


    /**
     * main methods
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TelemetrySender.mainActivity = this;

        // preferences
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // sensor service
        sensorServiceIntent = new Intent(this, SensorListenerService.class);

        // alarm manager
        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        // main layout
        setContentView(R.layout.main);
        mInfoText = (TextView)findViewById(R.id.info);

        // actionbar
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        startService(sensorServiceIntent);
        registerReceiver(sensorServiceReceiver, new IntentFilter(SensorListenerService.BROADCAST_ACTION));
        doStartAlarm();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(sensorServiceReceiver);
        stopService(sensorServiceIntent);
    }

    @Override
    protected void onDestroy() {
        doCancelAlarm();
        super.onDestroy();
    }


    /**
     * sensor service
     */

    private BroadcastReceiver sensorServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        SensorsObject s = SensorsObject.getSingletonObject();
        sensors = s.getSensors();
        values = s.getValues();
        }
    };

    /**
     * alarm manager
     */

    public void doStartAlarm() {
        // periodic update of display values
        alarmIntent = new Intent("org.aprsdroid.telemetrysender.UPDATE_DISPLAY");
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + INTERVAL_TEN_SECONDS, INTERVAL_ONE_SECOND, pendingIntent);
        // periodic sending of telemetry data
        alarmIntent = new Intent("org.aprsdroid.telemetrysender.PERIODIC_SENDING");
        pendingIntent = PendingIntent.getBroadcast(this, 1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + INTERVAL_TEN_SECONDS, Long.parseLong(sharedPrefs.getString("pref_interval_send_interval", "600000")), pendingIntent);
    }

    public void doCancelAlarm() {
        // cancel periodic update of display values
        alarmIntent = new Intent("org.aprsdroid.telemetrysender.UPDATE_DISPLAY");
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        // cancel periodic sending of telemetry data
        alarmIntent = new Intent("org.aprsdroid.telemetrysender.PERIODIC_SENDING");
        pendingIntent = PendingIntent.getBroadcast(this, 1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
    }

    /**
     * actionbar
     */

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onClickActionbarPreferences(MenuItem item) {
        startActivity(new Intent(this, ShowPreferencesActivity.class));
    }

    public void onClickActionbarAbout(MenuItem item) {
        startActivity(new Intent(this, ShowAboutActivity.class));
    }


    /**
     * button handlers
     */

    public void onStartService(View view) {
        Intent i = new Intent("org.aprsdroid.app.SERVICE");
        startService(i);
    }

    public void onSendParams(View view) {
        doSendParams();
    }


    public void onSendValues(View view) {
        doSendValues();
    }


    /**
     * helper methods
     */

    public void doDisplayValues() {
        StringBuilder sb = new StringBuilder();
        for (int id = 0; id < sensors.length; id++)
            if (sensors[id] != null) {
                sb.append(sensors[id].getName());
                sb.append(": ");
                for (int value = 0; value < values[id].length; value++) {
                    if (values[id][value] != 0.0f) {
                        if (value > 0)
                            sb.append(", ");
                        sb.append(values[id][value]);
                    }
                }
                sb.append("\n");
            }
        mInfoText.setText(sb.toString());
    }

    public void doSendParams() {
        StringBuilder sb_names = new StringBuilder("PARM.");
        StringBuilder sb_units = new StringBuilder("UNIT.");
        StringBuilder sb_eqns = new StringBuilder("EQNS.");

        // APRS telemetry specification only support 5 analogue channels!!
        for (int id = 0; id < 5; id++) {
            if (sensors[id] != null) {
                sb_names.append(SENSOR_NAMES[id]);
                sb_names.append(",");

                sb_units.append(SENSOR_UNITS[id]);
                sb_units.append(",");

                sb_eqns.append("0,");
                float scale = sensors[id].getMaximumRange() / 255;
                sb_eqns.append(scale);
                sb_eqns.append(",0,");
            }
        }

        doSendPacket(sb_names.toString());
        doSendPacket(sb_units.toString());
        doSendPacket(sb_eqns.toString());
    }

    public void doSendValues() {
        StringBuilder sb = new StringBuilder(String.format("T#%03d,", seq_no++));
        int count = 0;

        // APRS telemetry specification only support 5 analogue channels!!
        for (int id = 0; id < 5; id++) {
            if (sensors[id] != null) {
                // we want to send just a specific axis value for sensors with coordinate system
                int value;
                if (values[id][sensor_axis] != 0.0f) {
                    value = (int)(values[id][sensor_axis] * 255 / sensors[id].getMaximumRange());
                } else {
                    value = (int)(values[id][0] * 255 / sensors[id].getMaximumRange());
                }
                sb.append(String.format("%03d", value));
                sb.append(",");
                count++;
            }
        }
        while (count < 5) {
            sb.append("000,");
            count++;
        }

        sb.append("00000000");
        doSendPacket(sb.toString());
    }

    public void doSendPacket(String packet) {
        Intent i = new Intent("org.aprsdroid.app.SEND_PACKET");

        if (packet.endsWith(","))
            packet = packet.substring(0, packet.length() - 1);
        Log.d("SENSOR", "TX >>> " + packet);
        i.putExtra("data", packet);

        startService(i);
    }
}
