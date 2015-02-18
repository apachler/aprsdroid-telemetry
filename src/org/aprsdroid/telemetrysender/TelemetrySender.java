package org.aprsdroid.telemetrysender;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

public class TelemetrySender extends Activity implements SensorEventListener
{
	// constants for sensor configuration - http://developer.android.com/reference/android/hardware/Sensor.html
	// APRS telemetry specification only support 5 analogue channels but we can display more inside the textview!!
	// maximal length of channel names are 6, 6, 5, 5, 4
	static final int SENSOR_TYPES[] = {
		TYPE_LINEAR_ACCELERATION, TYPE_AMBIENT_TEMPERATURE, TYPE_RELATIVE_HUMIDITY, TYPE_PRESSURE, TYPE_GRAVITY, TYPE_GYROSCOPE, TYPE_MAGNETIC_FIELD
	};
	static final String SENSOR_NAMES[] = {
		"Laccel", "Atemp", "RH", "Press", "Grav", "Gyro", "Magfield"
	};
	static final String SENSOR_UNITS[] = {
		"m/s^2", "deg.C", "%rh", "hPa", "m/s^2", "rad/s", "uT"
	};

	// the sensors and values are kept in two arrays to allow asynchronous sending
	Sensor sensors[];
	float values[][];
	int seq_no = 0;

    // select which sensor axis we want to send over APRS for sensors with coordinate system (0=x, 1=y, 2=z)
	int sensor_axis = 2;
	
	// alarm manager
	final static private long ONE_SECOND = 1000;
	final static private long SEND_INTERVAL = ONE_SECOND * 60 * 3;
	PendingIntent pi;
	BroadcastReceiver br;
	AlarmManager am;

	// UI elements
	TextView mInfoText = null;
	EditText mCallSsid = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mInfoText = (TextView)findViewById(R.id.info);
		mCallSsid = (EditText)findViewById(R.id.callssid);

		sensors = new Sensor[SENSOR_TYPES.length];
		values = new float[SENSOR_TYPES.length][3];

		// setup alarm manager for periodic sending of telemetry data
		setupAM();
	}

	@Override
	public void onResume() {
		super.onResume();
		registerSensors();
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterSensors();
	}

	@Override
	protected void onDestroy() {
		am.cancel(pi);
		unregisterReceiver(br);
		unregisterSensors();
		super.onDestroy();
	}
	
	private void setupAM() {
		br = new BroadcastReceiver() {
            @Override
			public void onReceive(Context c, Intent i) {
				onSendParams(View view);
				onSendValues(View view);
			}
        };
		
		registerReceiver(br, new IntentFilter("org.aprsdroid.telemetrysender"));
        pi = PendingIntent.getBroadcast(this, 0, new Intent("org.aprsdroid.telemetrysender"), 0);
        am = (AlarmManager)(this.getSystemService(Context.ALARM_SERVICE));
		am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + SEND_INTERVAL, pi);
	}

	// button handlers
	public void onStartService(View view) {
		Intent i = new Intent("org.aprsdroid.app.SERVICE");
		startService(i);
	}

	public void onSendParams(View view) {
		String callssid = String.format(":%-9s:", mCallSsid.getText().toString());
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
				float scale = sensors[id].getMaximumRange()/255;
				sb_eqns.append(scale);
				sb_eqns.append(",0,");
			}
		}

		sendPacket(callssid + sb_names.toString());
		sendPacket(callssid + sb_units.toString());
		sendPacket(callssid + sb_eqns.toString());
	}

	public void onSendValues(View view) {
		StringBuilder sb = new StringBuilder(String.format("T#%03d,", seq_no++));
		int count = 0;
		
		// APRS telemetry specification only support 5 analogue channels!!
		for (int id = 0; id < 5; id++) {
			if (sensors[id] != null) {
				// we want to send just a specific axis value for sensors with coordinate system
                if (values[id].isArray())
				    int value = (int)(values[id][sensoraxis] * 255 / sensors[id].getMaximumRange());
                else
				    int value = (int)(values[id] * 255 / sensors[id].getMaximumRange());
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
		sendPacket(sb.toString());
	}

	// SensorEventListener callbacks
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		for (int id = 0; id < SENSOR_TYPES.length; id++)
			if (sensors[id] == event.sensor) {
                for (int value = 0; value < event.values.length; value++)
    				values[id][value] = event.values[value];
				displayValues();
			}
	}

	// helper methods
	public void sendPacket(String packet) {
		Intent i = new Intent("org.aprsdroid.app.SEND_PACKET");
		if (packet.endsWith(","))
			packet = packet.substring(0, packet.length() - 1);
		Log.d("SENSOR", "TX >>> " + packet);
		i.putExtra("data", packet);
		startService(i);
	}

	public void displayValues() {
		StringBuilder sb = new StringBuilder();
		for (int id = 0; id < SENSOR_TYPES.length; id++)
			if (sensors[id] != null) {
   				sb.append(sensors[id].getName());
   				sb.append(": ");
                for (int value = 0; value < values[id].length; value++) {
                    if (values[id][value] != null) {
                        sb.append(values[id][value]);
                        if (value < values[id].length - 1)
                            sb.append(", ");
                    }
                }
   				sb.append("\n");
			}
		mInfoText.setText(sb.toString());
	}

	public void registerSensors() {
		SensorManager sm = (SensorManager)getSystemService(SENSOR_SERVICE);
		for (int id = 0; id < SENSOR_TYPES.length; id++) {
			sensors[id] = sm.getDefaultSensor(SENSOR_TYPES[id]);
			if (sensors[id] != null)
				sm.registerListener(this, sensors[id], SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	public void unregisterSensors() {
		SensorManager sm = (SensorManager)getSystemService(SENSOR_SERVICE);
		for (int id = 0; id < SENSOR_TYPES.length; id++) {
			if (sensors[id] != null)
				sm.unregisterListener(this, sensors[id]);
		}
	}
}
