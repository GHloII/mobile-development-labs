package g433_v5.lab4;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLException;
import org.json.JSONException;

/** View/controller: validates input, starts a worker, and presents its server response. */
public class MainActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView status, result;
    private Button calculate;
    private PyramidService service;
    private boolean busy;
    private volatile boolean disposed;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        findViewById(R.id.root).setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars()
                    | android.view.WindowInsets.Type.displayCutout() | android.view.WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        status = findViewById(R.id.status);
        result = findViewById(R.id.result);
        calculate = findViewById(R.id.calculate);
        service = new PyramidService(this);
        calculate.setOnClickListener(view -> request());
        findViewById(R.id.about).setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.about).setMessage(R.string.authors)
                .setPositiveButton(R.string.close, null).show());
        if (state != null) {
            result.setText(state.getCharSequence(getString(R.string.state_result), getString(R.string.empty)));
            status.setText(state.getBoolean(getString(R.string.state_busy)) ? getString(R.string.interrupted)
                    : state.getCharSequence(getString(R.string.state_status), getString(R.string.ready)));
        }
    }

    private double positive(int id) {
        String text = ((EditText) findViewById(id)).getText().toString().trim().replace(',', '.');
        double value = Double.parseDouble(text);
        if (!Double.isFinite(value) || value <= 0 || value > 1_000_000) throw new NumberFormatException();
        return value;
    }

    private void request() {
        if (busy) return;
        final double a, b, h;
        try { a = positive(R.id.side_a); b = positive(R.id.side_b); h = positive(R.id.height); }
        catch (NumberFormatException e) { showError(getString(R.string.input_error)); return; }
        String endpoint = ((EditText) findViewById(R.id.endpoint)).getText().toString().trim();
        if (!service.validEndpoint(endpoint)) { showError(getString(R.string.url_error)); return; }
        busy = true;
        calculate.setEnabled(false);
        result.setText(R.string.empty);
        status.setText(R.string.waiting);
        // The network and JSON parsing never block Android's UI thread.
        worker.execute(() -> {
            double[] answer = null;
            String error = null;
            try { answer = service.calculate(endpoint, a, b, h); }
            catch (SSLException e) { error = getString(R.string.tls_error); }
            catch (PyramidService.HttpFailure e) { error = getString(R.string.http_error, e.code); }
            catch (JSONException e) { error = getString(R.string.response_error); }
            catch (IOException | IllegalArgumentException e) { error = getString(R.string.network_error); }
            final double[] completed = answer;
            final String message = error;
            runOnUiThread(() -> {
                if (disposed) return;
                busy = false;
                calculate.setEnabled(true);
                if (message != null) showError(message);
                else {
                    result.setText(getString(R.string.result_format, completed[0], completed[1]));
                    status.setText(R.string.done);
                }
            });
        });
    }

    private void showError(String message) {
        result.setText(R.string.empty);
        status.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putCharSequence(getString(R.string.state_result), result.getText());
        state.putCharSequence(getString(R.string.state_status), status.getText());
        state.putBoolean(getString(R.string.state_busy), busy);
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        disposed = true;
        service.cancel();
        worker.shutdownNow();
        super.onDestroy();
    }
}
