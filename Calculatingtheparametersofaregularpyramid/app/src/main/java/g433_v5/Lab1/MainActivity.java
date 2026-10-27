package g433_v5.Lab1;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText sideA = findViewById(R.id.side_a);
        EditText sideB = findViewById(R.id.side_b);
        EditText height = findViewById(R.id.height);
        TextView result = findViewById(R.id.result);
        TextView status = findViewById(R.id.status);

        // Keep author details in resources so the same text works in both layouts.
        findViewById(R.id.about).setOnClickListener(view ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.about_title)
                        .setMessage(R.string.about_message)
                        .setPositiveButton(R.string.close, null)
                        .show());

        findViewById(R.id.calculate).setOnClickListener(view -> {
            try {
                double a = readPositive(sideA);
                double b = readPositive(sideB);
                double h = readPositive(height);

                double[] values = calculate(a, b, h);
                result.setText(getString(R.string.result_format, values[0], values[1]));
                status.setText(R.string.status_done);
            } catch (NumberFormatException e) {
                result.setText(R.string.invalid_input);
                status.setText(R.string.status_error);
                Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // EditText saves its own contents; preserve the calculated text as well.
        outState.putCharSequence(getString(R.string.state_status),
                ((TextView) findViewById(R.id.status)).getText());
        outState.putCharSequence(getString(R.string.state_result),
                ((TextView) findViewById(R.id.result)).getText());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ((TextView) findViewById(R.id.status)).setText(
                savedInstanceState.getCharSequence(getString(R.string.state_status),
                        getString(R.string.status_ready)));
        ((TextView) findViewById(R.id.result)).setText(
                savedInstanceState.getCharSequence(getString(R.string.state_result),
                        getString(R.string.result_empty)));
    }

    private double readPositive(EditText input) {
        double value = Double.parseDouble(input.getText().toString().trim());
        if (!Double.isFinite(value) || value <= 0) {
            throw new NumberFormatException();
        }
        return value;
    }

    /**
     * Calculates a right pyramid with a rectangular base and its apex over the center.
     * a and b are base side lengths; h is the vertical height. All must be positive
     * finite numbers. Returns surface area and volume in that order.
     */
    private double[] calculate(double a, double b, double h) {
        // The two pairs of side faces have different slant heights.
        double surface = a * b
                + a * Math.hypot(h, b / 2)
                + b * Math.hypot(h, a / 2);
        double volume = a * b * h / 3;
        if (!Double.isFinite(surface) || !Double.isFinite(volume)) {
            throw new NumberFormatException();
        }
        return new double[]{surface, volume};
    }
}
