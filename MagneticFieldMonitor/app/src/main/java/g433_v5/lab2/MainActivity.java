package g433_v5.lab2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

/** Displays live magnetometer readings; releases the listener while off screen. */
public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager manager;
    private Sensor magnet;
    private TextView values, status, accuracy;
    private Button toggle;
    private boolean running = true;
    private boolean registered;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        // Insets protect controls from system bars in both screen orientations.
        findViewById(R.id.root).setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        values = findViewById(R.id.values);
        status = findViewById(R.id.status);
        accuracy = findViewById(R.id.accuracy);
        toggle = findViewById(R.id.toggle);
        if (state != null) running = state.getBoolean(getString(R.string.state_running), true);
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        magnet = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        TextView info = findViewById(R.id.sensor_info);
        if (magnet == null) {
            info.setText(R.string.no_sensor);
            status.setText(R.string.no_sensor);
            toggle.setEnabled(false);
        } else {
            info.setText(getString(R.string.sensor_summary, magnet.getName(), magnet.getVendor(),
                    magnet.getMaximumRange(), magnet.getResolution()));
        }
        showAccuracy(-1);
        toggle.setText(running ? R.string.stop : R.string.start);
        toggle.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_magnet_button, 0, 0, 0);
        toggle.setOnClickListener(view -> {
            running = !running;
            if (running) startListening(); else stopListening();
            toggle.setText(running ? R.string.stop : R.string.start);
        });
        findViewById(R.id.sensors).setOnClickListener(view ->
                startActivity(new Intent(this, SensorsActivity.class)));
        findViewById(R.id.about).setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.about_title).setMessage(R.string.authors)
                .setPositiveButton(R.string.close, null).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (running) startListening();
        else if (magnet != null) status.setText(R.string.stopped);
    }

    private void startListening() {
        if (magnet == null || registered) return;
        registered = manager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_UI);
        status.setText(registered ? R.string.waiting : R.string.register_failed);
        if (!registered) {
            running = false;
            toggle.setText(R.string.start);
        }
    }

    private void stopListening() {
        if (manager != null) manager.unregisterListener(this);
        registered = false;
        if (magnet != null) status.setText(R.string.stopped);
    }

    @Override
    protected void onPause() {
        stopListening();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean(getString(R.string.state_running), running);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!registered || event.sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD) return;
        if (event.values.length < 3 || !Float.isFinite(event.values[0])
                || !Float.isFinite(event.values[1]) || !Float.isFinite(event.values[2])) {
            status.setText(R.string.bad_event);
            return;
        }
        double x = event.values[0], y = event.values[1], z = event.values[2];
        double magnitude = Math.hypot(Math.hypot(x, y), z);
        values.setText(getString(R.string.values_format, x, y, z, magnitude));
        status.setText(R.string.measuring);
        showAccuracy(event.accuracy);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int value) {
        if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) showAccuracy(value);
    }

    private void showAccuracy(int value) {
        String[] labels = getResources().getStringArray(R.array.accuracy_labels);
        String label = value >= 0 && value < labels.length ? labels[value] : getString(R.string.accuracy_unknown);
        accuracy.setText(getString(R.string.accuracy_format, label));
    }
}
