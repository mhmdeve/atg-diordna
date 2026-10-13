package ch.cclerc.muguet.android;

import android.app.NativeActivity;
import android.os.Bundle;
import android.view.WindowManager;

/** NativeActivity hosting the Rust/winit renderer. */
public final class GameActivity extends NativeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
    }
}
