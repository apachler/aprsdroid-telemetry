package org.aprsdroid.telemetrysender;

import android.hardware.Sensor;
import java.io.Serializable;

class SensorsObject implements Serializable {
    /**
     * constants / statics
     */

    private static final long serialVersionUID = 1L;
    private static SensorsObject singletonObject;


    /**
     * variables
     */

    Sensor sensors[];
    float values[][];

    public static SensorsObject getSingletonObject() {
        if (singletonObject == null) {
            singletonObject = new SensorsObject();
        }
        return singletonObject;
    }


    /**
     * main methods
     */

    public void setSensors(Sensor sensors[]) {
        this.sensors = sensors;
    }

    public Sensor[] getSensors() {
        return sensors;
    }

    public void setValues(float values[][]) {
        this.values = values;
    }

    public float[][] getValues() {
        return values;
    }
}
