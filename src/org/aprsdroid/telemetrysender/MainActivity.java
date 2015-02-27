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
    /*
     *  SENSORTYPE                          NR                      UNIT            VALUES
     *  --------------------------------------------------------------------------------------------
     *  TYPE_ACCELEROMETER                   1                      m/s^2           3
     *  TYPE_MAGNETIC_FIELD                  2                      µT              3
     *  TYPE_ORIENTATION                     3  (deprecated)        deg.            3
     *  TYPE_GYROSCOPE                       4                      rad/s           3
     *  TYPE_LIGHT                           5                      lx              1
     *  TYPE_PRESSURE                        6                      hPa/mbar        1
     *  TYPE_TEMPERATURE                     7  (deprecated)        deg.C           1
     *  TYPE_PROXIMITY                       8                      cm              1
     *  TYPE_GRAVITY                         9                      m/s^2           3
     *  TYPE_LINEAR_ACCELERATION            10                      m/s^2           3
     *  TYPE_ROTATION_VECTOR                11                                      4
     *  TYPE_RELATIVE_HUMIDITY              12                      %               1
     *  TYPE_AMBIENT_TEMPERATURE            13                      deg.C           1
     *  TYPE_MAGNETIC_FIELD_UNCALIBRATED    14                      uT              6
     *  TYPE_GAME_ROTATION_VECTOR           15                                      3
     *  TYPE_GYROSCOPE_UNCALIBRATED         16                      rad/s           6
     *  TYPE_SIGNIFICANT_MOTION             17
     *  TYPE_STEP_DETECTOR                  18
     *  TYPE_STEP_COUNTER                   19                      Steps           1
     *  TYPE_GEOMAGNETIC_ROTATION_VECTOR    20                                      3
     *  TYPE_HEART_RATE                     21
     */
    // APRS telemetry specification only support 5 analogue channels but we can display more inside the textview!!
    // maximal length of channel names are 6, 6, 5, 5, 4
    static final String SENSOR_NAMES[] = {
            "Accel", "Mfield", "Orient", "Gyro", "Light", "Press", "Temp", "Prox", "Grav", "Laccel", "RotV", "RH", "Atemp", "MfielU", "GRotV", "GyroU", "SigM", "StepD", "StepC", "GMRotV", "HeartR"
    };
    static final String SENSOR_UNITS[] = {
            "m/s^2", "uT", "deg.", "rad/s", "lx", "hPa", "deg.C", "cm", "m/s^2", "m/s^2", "", "%", "deg.C", "uT", "", "rad/s", "", "", "Steps", "", ""
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
        startService(sensorServiceIntent);
        registerReceiver(sensorServiceReceiver, new IntentFilter(SensorListenerService.BROADCAST_ACTION));

        // alarm manager
        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        doStartAlarm();

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
    }

    @Override
    public void onPause() {
        super.onPause();
//        unregisterReceiver(sensorServiceReceiver);
//        stopService(sensorServiceIntent);
    }

    @Override
    protected void onDestroy() {
        doCancelAlarm();
        unregisterReceiver(sensorServiceReceiver);
        stopService(sensorServiceIntent);
        super.onDestroy();
    }


    /**
     * sensor service
     */

    private BroadcastReceiver sensorServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        Log.d("SensorListenerService", intent.getAction());

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
        int sensor_count = 0;
        StringBuilder sb_names = new StringBuilder("PARM.");
        StringBuilder sb_units = new StringBuilder("UNIT.");
        StringBuilder sb_eqns = new StringBuilder("EQNS.");

        for (Sensor sensor: sensors) {
            int sensor_type = sensor.getType();
            // is that sensor activated through preferences?
            if (sharedPrefs.getBoolean("pref_sensor_sensor_type_" + sensor_type, false)) {
                sb_names.append(SENSOR_NAMES[sensor_type]);
                sb_names.append(",");

                sb_units.append(SENSOR_UNITS[sensor_type]);
                sb_units.append(",");

                sb_eqns.append("0,");
                float scale = sensor.getMaximumRange() / 255;
                sb_eqns.append(scale);
                sb_eqns.append(",0,");

                sensor_count++;
            }

            // APRS telemetry specification only support 5 analogue channels!!
            if (sensor_count >= 4) {
                break;
            }
        }

        StringBuilder sb_bits = new StringBuilder("BITS.");
        sb_bits.append("00000000,");
        sb_bits.append(sharedPrefs.getString("pref_aprs_project_name", "APRSdroid TelemetrySender").trim().substring(0, 23));

        doSendPacket(sb_names.toString());
        doSendPacket(sb_units.toString());
        doSendPacket(sb_eqns.toString());
        doSendPacket(sb_bits.toString());
    }

    public void doSendValues() {
        StringBuilder sb = new StringBuilder(String.format("T#%03d,", seq_no++));
        int sensor_count = 0;

        for (int id = 0; id < sensors.length; id++) {
            int sensor_type = sensors[id].getType();
            // is that sensor activated through preferences?
            if (sharedPrefs.getBoolean("pref_sensor_sensor_type_" + sensor_type, false)) {
                // we want to send a specific axis value for sensors with coordinate system
                int value;
                if (values[id][sensor_axis] != 0.0f) {
                    value = (int)(values[id][sensor_axis] * 255 / sensors[id].getMaximumRange());
                } else {
                    value = (int)(values[id][0] * 255 / sensors[id].getMaximumRange());
                }
                sb.append(String.format("%03d", value));
                sb.append(",");

                sensor_count++;
            }

            // APRS telemetry specification only support 5 analogue channels!!
            if (sensor_count >= 4) {
                break;
            }
        }
        while (sensor_count < 5) {
            sb.append("000,");
            sensor_count++;
        }

        sb.append("00000000");
        doSendPacket(sb.toString());
    }

    public void doSendPacket(String packet) {
        Intent i = new Intent("org.aprsdroid.app.SEND_PACKET");

        if (packet.endsWith(","))
            packet = packet.substring(0, packet.length() - 1);
        Log.d("SendPacket", "TX >>> " + packet);
        i.putExtra("data", packet);

        startService(i);
    }
}
