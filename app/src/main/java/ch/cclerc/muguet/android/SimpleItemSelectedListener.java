package ch.cclerc.muguet.android;

import android.view.View;
import android.widget.AdapterView;

/** Small helper so the launcher stays compatible with the Android framework widgets. */
abstract class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {
    private final Runnable changed;

    SimpleItemSelectedListener(Runnable changed) {
        this.changed = changed;
    }

    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        changed.run();
    }

    @Override public void onNothingSelected(AdapterView<?> parent) {}
}
