package g433_v5.lab4;

import android.content.Context;
import android.net.Uri;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;

/** HTTPS transport only. All geometry is calculated by the PHP service. */
final class PyramidService {
    private final Context context;
    private volatile HttpsURLConnection active;
    private volatile boolean cancelled;

    PyramidService(Context context) { this.context = context.getApplicationContext(); }

    boolean validEndpoint(String value) {
        try {
            Uri uri = Uri.parse(value);
            return context.getString(R.string.scheme_https).equals(uri.getScheme())
                    && uri.getHost() != null && !uri.getHost().isEmpty()
                    && uri.getUserInfo() == null && uri.getQuery() == null
                    && uri.getFragment() == null
                    && (uri.getPort() == -1 || uri.getPort() > 0 && uri.getPort() <= 65535)
                    && value.chars().noneMatch(Character::isWhitespace);
        } catch (IllegalArgumentException e) { return false; }
    }

    double[] calculate(String endpoint, double a, double b, double h)
            throws IOException, JSONException, HttpFailure {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(endpoint).openConnection();
        active = connection;
        try {
            if (cancelled) throw new IOException();
            // Normal certificate and hostname verification remain enabled, including in debug.
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(context.getString(R.string.method_post));
            connection.setRequestProperty(context.getString(R.string.header_content_type),
                    context.getString(R.string.mime_form));
            connection.setRequestProperty(context.getString(R.string.header_accept),
                    context.getString(R.string.mime_json));
            connection.setDoOutput(true);
            String form = new Uri.Builder().appendQueryParameter(context.getString(R.string.key_a), Double.toString(a))
                    .appendQueryParameter(context.getString(R.string.key_b), Double.toString(b))
                    .appendQueryParameter(context.getString(R.string.key_h), Double.toString(h))
                    .build().getEncodedQuery();
            byte[] body = form.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (java.io.OutputStream stream = connection.getOutputStream()) { stream.write(body); }
            int code = connection.getResponseCode();
            if (code != HttpsURLConnection.HTTP_OK) throw new HttpFailure(code);
            String type = connection.getContentType();
            if (type == null || !type.toLowerCase(java.util.Locale.ROOT)
                    .startsWith(context.getString(R.string.mime_json))) throw new JSONException(context.getString(R.string.response_error));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream stream = connection.getInputStream()) {
                byte[] buffer = new byte[1024];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    if (cancelled || bytes.size() + count > 16384) throw new IOException();
                    bytes.write(buffer, 0, count);
                }
            }
            JSONObject json = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
            if (!Boolean.TRUE.equals(json.get(context.getString(R.string.key_ok)))) throw new JSONException(context.getString(R.string.response_error));
            JSONObject result = json.getJSONObject(context.getString(R.string.key_res));
            Object surface = result.get(context.getString(R.string.key_surface));
            Object volume = result.get(context.getString(R.string.key_volume));
            if (!(surface instanceof Number) || !(volume instanceof Number)) throw new JSONException(context.getString(R.string.response_error));
            double s = ((Number) surface).doubleValue(), v = ((Number) volume).doubleValue();
            if (!Double.isFinite(s) || !Double.isFinite(v) || s <= 0 || v <= 0) throw new JSONException(context.getString(R.string.response_error));
            return new double[]{s, v};
        } finally { connection.disconnect(); active = null; }
    }

    void cancel() {
        cancelled = true;
        HttpsURLConnection connection = active;
        if (connection != null) connection.disconnect();
    }

    /** HTTP status is retained so transport errors are distinguishable from bad JSON. */
    static final class HttpFailure extends Exception {
        final int code;
        HttpFailure(int code) { this.code = code; }
    }
}
