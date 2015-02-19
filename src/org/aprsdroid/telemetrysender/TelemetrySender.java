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
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class TelemetrySender extends Activity implements SensorEventListener {
    /**
     * constants / statics
     */

    // interval definitions
    final static private long INTERVAL_ONE_SECOND = 1000;
    final static private long INTERVAL_TEN_SECONDS = INTERVAL_ONE_SECOND * 10;
    final static private long INTERVAL_ONE_MINUTE = INTERVAL_TEN_SECONDS * 6;

    // constants for sensor configuration - http://developer.android.com/reference/android/hardware/Sensor.html
    // APRS telemetry specification only support 5 analogue channels but we can display more inside the textview!!
    // maximal length of channel names are 6, 6, 5, 5, 4
    static final int SENSOR_TYPES[] = {
            Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_AMBIENT_TEMPERATURE, Sensor.TYPE_RELATIVE_HUMIDITY, Sensor.TYPE_PRESSURE, Sensor.TYPE_GRAVITY, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD
    };
    static final String SENSOR_NAMES[] = {
            "Laccel", "Atemp", "RH", "Press", "Grav", "Gyro", "Mfield"
    };
    static final String SENSOR_UNITS[] = {
            "m/s^2", "deg.C", "%rh", "hPa", "m/s^2", "rad/s", "uT"
    };


    /**
     * variables
     */

    // preferences
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    // alarm manager
    AlarmManager am;

    // UI elements
    TextView mInfoText = (TextView)findViewById(R.id.info);

    // the sensors and values are kept in two arrays to allow asynchronous sending
    Sensor sensors[] = new Sensor[SENSOR_TYPES.length];
    float values[][] = new float[SENSOR_TYPES.length][3];
    int seq_no = 0;

    // select which sensor axis we want to send over APRS for sensors with coordinate system (0=x, 1=y, 2=z)
    int sensor_axis = 2;


    /**
     * main methods
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // main layout
        setContentView(R.layout.main);

        // actionbar
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // setup alarm manager for periodic sending of telemetry data
        am = (AlarmManager) (this.getSystemService(Context.ALARM_SERVICE));
        amPeriodicSending();
        amPeriodicDisplayValues();
    }

    @Override
    public void onResume() {
        super.onResume();
		doRegisterSensors();
    }

    @Override
    public void onPause() {
        super.onPause();
//		doUnregisterSensors();
    }

    @Override
    protected void onDestroy() {
//		am.cancel(pi);
//		unregisterReceiver(br);
        doUnregisterSensors();
        super.onDestroy();
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


    /**
     * alarm manager
     */

    private void amPeriodicSending() {
        PendingIntent pi;
        BroadcastReceiver br;

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                doSendParams();
                doSendValues();
            }
        };

        registerReceiver(br, new IntentFilter("org.aprsdroid.telemetrysender"));
        final int _id = (int) System.currentTimeMillis();
        pi = PendingIntent.getBroadcast(this, _id, new Intent("org.aprsdroid.telemetrysender"), 0);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, sharedPrefs.getLong("pref_interval_send_interval", -1), sharedPrefs.getLong("pref_interval_send_interval", -1), pi);
    }

    private void amPeriodicDisplayValues() {
        PendingIntent pi;
        BroadcastReceiver br;

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                doDisplayValues();
            }
        };

        registerReceiver(br, new IntentFilter("org.aprsdroid.telemetrysender"));
        final int _id = (int) System.currentTimeMillis();
        pi = PendingIntent.getBroadcast(this, _id, new Intent("org.aprsdroid.telemetrysender"), 0);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, INTERVAL_ONE_SECOND, INTERVAL_ONE_SECOND, pi);
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
     * SensorEventListener callbacks
     */

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        for (int id = 0; id < SENSOR_TYPES.length; id++)
            if (sensors[id] == event.sensor) {
                values[id] = event.values.clone();
                for (int value = event.values.length; value < values[id].length; value++)
                    values[id][value] = 0.0f;
            }
    }


    /**
     * helper methods
     */

    public void doDisplayValues() {
        StringBuilder sb = new StringBuilder();
        for (int id = 0; id < SENSOR_TYPES.length; id++)
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
                    value = (int) (values[id][sensor_axis] * 255 / sensors[id].getMaximumRange());
                } else {
                    value = (int) (values[id][0] * 255 / sensors[id].getMaximumRange());
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

    public void doRegisterSensors() {
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        for (int id = 0; id < SENSOR_TYPES.length; id++) {
            sensors[id] = sm.getDefaultSensor(SENSOR_TYPES[id]);
            if (sensors[id] != null)
                sm.registerListener(this, sensors[id], SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void doUnregisterSensors() {
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        for (int id = 0; id < SENSOR_TYPES.length; id++) {
            if (sensors[id] != null)
                sm.unregisterListener(this, sensors[id]);
        }
    }
}
