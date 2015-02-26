package org.aprsdroid.telemetrysender;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import java.util.ArrayList;
import java.util.List;

public class SensorListenerService extends Service implements SensorEventListener {
    /**
     * constants / statics
     */

    public static final String BROADCAST_ACTION = "org.aprsdroid.telemetrysender.UPDATE_VALUES";


    /**
     * variables
     */

    private final Handler handler = new Handler();
    Intent intent;

    SensorManager sensorManager;
    List<Sensor> listSensor;
    Sensor sensors[];
    float values[][];


    /**
     * main methods
     */

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        listSensor = sensorManager.getSensorList(Sensor.TYPE_ALL);

        int sensorQty = listSensor.size();
        sensors = new Sensor[sensorQty];
        values = new float[sensorQty][3];

        doRegisterSensors();

        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 5000); // 5 seconds

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(sendUpdatesToUI);
        doUnregisterSensors();
        super.onDestroy();
    }


    /**
     * SensorEventListener callbacks
     */

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        for (int id = 0; id < sensors.length; id++)
            if (sensors[id] == event.sensor) {
                values[id] = event.values.clone();
                for (int value = event.values.length; value < values[id].length; value++)
                    values[id][value] = 0.0f;
            }
    }


    /**
     * helper methods
     */

    public void doRegisterSensors() {
        for (int id = 0; id < sensors.length; id++) {
            sensors[id] = sensorManager.getDefaultSensor(listSensor.get(id).getType());
            if (sensors[id] != null)
                sensorManager.registerListener(this, sensors[id], SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void doUnregisterSensors() {
        for (Sensor sensor: sensors) {
            if (sensor != null)
                sensorManager.unregisterListener(this, sensor);
        }
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            SensorsObject s = SensorsObject.getSingletonObject();
            s.setSensors(sensors);
            s.setValues(values);

            sendBroadcast(intent);

            handler.postDelayed(this, 1000); // 1 second
        }
    };
}
