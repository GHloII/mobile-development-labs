package com.g433_v5.lab3;

import android.app.Activity;
import android.graphics.Insets;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;

/** Shared handling of system bars and short database error messages. */
abstract class BaseActivity extends Activity {
    protected void applyInsets() {
        View root = findViewById(R.id.root);
        int left = root.getPaddingLeft(), top = root.getPaddingTop();
        int right = root.getPaddingRight(), bottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsets.Type.ime());
            view.setPadding(left + bars.left, top + bars.top, right + bars.right,
                    bottom + Math.max(bars.bottom, ime.bottom));
            return windowInsets;
        });
        root.requestApplyInsets();
    }

    protected void message(int resource) { Toast.makeText(this, resource, Toast.LENGTH_LONG).show(); }
}
