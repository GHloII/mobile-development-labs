package com.g433_v5.lab2;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Collections;
import java.util.List;

/** Lists all sensors advertised by Android, including virtual sensors. */
public class SensorsActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_sensors);
        findViewById(R.id.root).setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        SensorManager manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = manager == null ? Collections.emptyList() : manager.getSensorList(Sensor.TYPE_ALL);
        ((TextView) findViewById(R.id.count)).setText(getString(R.string.sensors_count, sensors.size()));
        LinearLayout list = findViewById(R.id.sensor_list);
        // The resource defines the card appearance; Java supplies only hardware data.
        for (Sensor sensor : sensors) {
            TextView item = (TextView) getLayoutInflater().inflate(R.layout.item_sensor, list, false);
            item.setText(getString(R.string.sensor_details, sensor.getName(), sensor.getVendor(),
                    sensor.getType(), sensor.getVersion(), sensor.getMaximumRange(), sensor.getResolution(),
                    sensor.getPower(), sensor.getMinDelay(), sensor.getMaxDelay(), sensor.getFifoReservedEventCount(),
                    sensor.getFifoMaxEventCount(), sensor.getReportingMode(),
                    getString(sensor.isWakeUpSensor() ? R.string.yes : R.string.no)));
            list.addView(item);
        }
        findViewById(R.id.back).setOnClickListener(view -> finish());
    }
}
