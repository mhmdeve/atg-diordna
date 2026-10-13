package ch.cclerc.muguet.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.text.InputType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Android counterpart of ios/Launcher.swift.
 * Keeps the same setting keys and presets; the game itself runs in the Rust NativeActivity.
 */
public final class MainActivity extends Activity {
    private static final int PICK_ZIP = 1001;
    private static final int MODE_IMPORT = 1;
    private File root, gameDir, configFile, cacheDir;
    private TextView status, memoryLabel;
    private ProgressBar progress;
    private Spinner mode, graphics, resolution, textures, fps;
    private SeekBar sensitivity;
    private TextView sensitivityValue;
    private CheckBox mute, showFps, multiplayer;
    private EditText playerName, serverAddress;
    private Spinner session;
    private LinearLayout multiplayerDetails;
    private JSONObject config = new JSONObject();

    private final String[] presets = {"Battery", "Balanced", "Quality", "Max", "Custom"};

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        root = new File(getFilesDir(), "Muguet");
        gameDir = new File(root, "game");
        configFile = new File(root, "config.json");
        cacheDir = getCacheDir();
        root.mkdirs();
        gameDir.mkdirs();
        loadConfig();
        buildUi();
        refreshReady();
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(16), dp(20), dp(18));
        page.setBackgroundColor(0xff101114);

        TextView title = text("Muguet", 30, true);
        title.setGravity(Gravity.CENTER);
        page.addView(title, matchWrap());

        status = text("Import game data to get started.", 14, false);
        status.setGravity(Gravity.CENTER);
        page.addView(status, matchWrap());

        memoryLabel = text("", 12, false);
        memoryLabel.setGravity(Gravity.CENTER);
        page.addView(memoryLabel, matchWrap());
        memoryLabel.setText(String.format(Locale.US, "Android memory limit: about %.1f GB", Runtime.getRuntime().maxMemory() / 1073741824.0));

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(0, dp(8), 0, dp(8));

        mode = spinner(new String[]{"Story Mode", "Sandbox"});
        addField(form, "Mode", mode);
        graphics = spinner(presets);
        addField(form, "Graphics preset", graphics);
        resolution = spinner(new String[]{"35 % (fastest)", "50 %", "75 %", "100 % (sharpest)"});
        addField(form, "Resolution (Custom preset)", resolution);
        textures = spinner(new String[]{"Normal (less memory)", "High", "Very High"});
        addField(form, "Textures (Custom preset)", textures);
        fps = spinner(new String[]{"30 fps (cooler)", "60 fps", "Uncapped (hotter)"});
        addField(form, "Frame limit (Custom preset)", fps);

        TextView sensitivityTitle = text("Look sensitivity", 15, false);
        form.addView(sensitivityTitle, matchWrap());
        LinearLayout sensRow = new LinearLayout(this);
        sensRow.setGravity(Gravity.CENTER_VERTICAL);
        sensitivity = new SeekBar(this);
        sensitivity.setMax(11);
        sensRow.addView(sensitivity, new LinearLayout.LayoutParams(0, dp(40), 1));
        sensitivityValue = text("1.00×", 13, false);
        sensRow.addView(sensitivityValue, wrapWrap());
        sensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                double multiplier = 0.25 + value * 0.25;
                sensitivityValue.setText(String.format(Locale.US, "%.2f×", multiplier));
            }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
        });
        form.addView(sensRow, matchWrap());

        mute = new CheckBox(this); mute.setText("Mute audio"); style(mute);
        showFps = new CheckBox(this); showFps.setText("Show frame rate"); style(showFps);
        multiplayer = new CheckBox(this); multiplayer.setText("Play with friends"); style(multiplayer);
        form.addView(mute, matchWrap());
        form.addView(showFps, matchWrap());
        form.addView(multiplayer, matchWrap());

        TextView multiplayerHeading = text("Multiplayer", 18, true);
        form.addView(multiplayerHeading, matchWrap());
        multiplayerDetails = new LinearLayout(this);
        multiplayerDetails.setOrientation(LinearLayout.VERTICAL);
        playerName = new EditText(this); playerName.setSingleLine(true); playerName.setHint("Your name"); style(playerName);
        multiplayerDetails.addView(playerName, matchWrap());
        session = spinner(new String[]{"Join", "Host"});
        addField(multiplayerDetails, "Session", session);
        serverAddress = new EditText(this); serverAddress.setSingleLine(true); serverAddress.setHint("Host address, e.g. 192.168.1.20");
        serverAddress.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI); style(serverAddress);
        multiplayerDetails.addView(serverAddress, matchWrap());
        form.addView(multiplayerDetails, matchWrap());
        multiplayer.setOnCheckedChangeListener((button, checked) -> multiplayerDetails.setVisibility(checked ? View.VISIBLE : View.GONE));
        session.setOnItemSelectedListener(new SimpleItemSelectedListener(() -> serverAddress.setVisibility(session.getSelectedItemPosition() == 0 ? View.VISIBLE : View.GONE)));

        scroll.addView(form);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setVisibility(View.GONE);
        page.addView(progress, matchWrap());

        Button importButton = button("Import game data .zip");
        importButton.setOnClickListener(v -> pickZip());
        page.addView(importButton, matchWrap());

        Button playButton = button("Play");
        playButton.setTextSize(20);
        playButton.setOnClickListener(v -> play());
        page.addView(playButton, matchWrap());

        TextView footer = text("proudly made by c22dev", 12, false);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, matchWrap());
        setContentView(page);
        applyConfigToUi();
    }

    private void refreshReady() {
        boolean ready = new File(gameDir, "data/manifest.json").isFile();
        status.setText(ready ? "Game data found. Configure options and tap Play." : "No game data found. Choose a compatible game .zip to import.");
    }

    private void pickZip() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_ZIP);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK_ZIP || res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        importZip(uri);
    }

    private void importZip(Uri uri) {
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
        status.setText("Importing game data…");
        new Thread(() -> {
            String error = null;
            try {
                File stage = new File(root, "game.importing");
                deleteTree(stage);
                stage.mkdirs();
                int copied = 0;
                try (InputStream raw = new BufferedInputStream(getContentResolver().openInputStream(uri));
                     ZipInputStream zip = new ZipInputStream(raw)) {
                    if (raw == null) throw new IllegalArgumentException("Unable to open selected zip.");
                    ZipEntry entry;
                    boolean manifest = false, shaders = false;
                    while ((entry = zip.getNextEntry()) != null) {
                        if (entry.isDirectory()) continue;
                        String name = entry.getName().replace('\\', '/');
                        if (name.startsWith("/") || name.contains("../")) continue;
                        String rel = trimPrefix(name);
                        if (rel == null || skipped(rel)) continue;
                        if (rel.equals("data/manifest.json")) manifest = true;
                        if (rel.startsWith("b/") && rel.endsWith("/shaders/index.json")) shaders = true;
                        File out = new File(stage, rel);
                        if (!out.getCanonicalPath().startsWith(stage.getCanonicalPath() + File.separator)) continue;
                        File parent = out.getParentFile();
                        if (parent != null) parent.mkdirs();
                        try (OutputStream stream = new FileOutputStream(out)) {
                            byte[] buffer = new byte[64 * 1024]; int n;
                            while ((n = zip.read(buffer)) > 0) stream.write(buffer, 0, n);
                        }
                        copied++;
                    }
                    if (!manifest) throw new IllegalArgumentException("This zip has no data/manifest.json.");
                    if (!shaders) throw new IllegalArgumentException("This zip has no b/<build>/shaders/index.json.");
                }
                File old = new File(root, "game");
                deleteTree(old);
                if (!stage.renameTo(old)) throw new IllegalStateException("Could not finalize imported game data.");
            } catch (Throwable e) { error = e.getMessage() == null ? e.toString() : e.getMessage(); }
            final String err = error;
            runOnUiThread(() -> {
                progress.setIndeterminate(false); progress.setVisibility(View.GONE);
                status.setText(err == null ? "Import finished. You can play now." : "Import failed: " + err);
                refreshReady();
            });
        }).start();
    }

    // Accept zip files rooted directly at data/ or with one containing folder.
    private String trimPrefix(String name) {
        int at = name.indexOf("data/manifest.json");
        String prefix = at < 0 ? null : name.substring(0, at);
        if (prefix == null && !name.startsWith("b/")) return null;
        if (prefix == null) return name;
        return name.startsWith(prefix) ? name.substring(prefix.length()) : null;
    }

    private boolean skipped(String rel) {
        String low = rel.toLowerCase(Locale.ROOT);
        if (!low.startsWith("data/") && !low.startsWith("b/")) return true;
        return low.contains("/env_test/") || low.contains("/verityradio/");
    }

    private void play() {
        if (!new File(gameDir, "data/manifest.json").isFile()) {
            new AlertDialog.Builder(this).setTitle("Muguet").setMessage("Import game data first.").setPositiveButton("OK", null).show();
            return;
        }
        if (multiplayer.isChecked() && session.getSelectedItemPosition() == 0 && serverAddress.getText().toString().trim().isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Muguet").setMessage("Enter the host address to join.").setPositiveButton("OK", null).show();
            return;
        }
        try {
            saveConfig();
            Intent intent = new Intent(this, GameActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            new AlertDialog.Builder(this).setTitle("Muguet").setMessage("Could not save settings: " + e.getMessage()).setPositiveButton("OK", null).show();
        }
    }

    private void loadConfig() {
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(configFile.toPath());
            config = new JSONObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) { config = new JSONObject(); }
    }

    private void applyConfigToUi() {
        mode.setSelection(config.optInt("start_mode", 2) == 1 ? 0 : 1);
        String preset = config.optString("graphics_preset", "balanced");
        int p = 1;
        for (int i = 0; i < presets.length; i++) if (presets[i].equalsIgnoreCase(preset)) p = i;
        graphics.setSelection(p);
        double scale = config.optDouble("scale", 0.5);
        resolution.setSelection(scale <= .35 ? 0 : scale <= .5 ? 1 : scale <= .75 ? 2 : 3);
        textures.setSelection(Math.max(0, Math.min(2, config.optInt("texture_quality", 0))));
        int f = config.optInt("fps_limit", 60);
        fps.setSelection(f <= 30 ? 0 : f == 60 ? 1 : 2);
        sensitivity.setProgress(Math.max(0, Math.min(11, (int)Math.round((config.optDouble("touch_sensitivity", 1.0)-0.25)/0.25))));
        mute.setChecked(config.optBoolean("mute", false));
        showFps.setChecked(config.optBoolean("show_fps", false));
        multiplayer.setChecked(config.optBoolean("multiplayer", false));
        playerName.setText(config.optString("player_name", ""));
        serverAddress.setText(config.optString("server", ""));
        session.setSelection(config.optBoolean("host", false) ? 1 : 0);
    }

    private void saveConfig() throws Exception {
        config.put("start_mode", mode.getSelectedItemPosition() == 0 ? 1 : 2);
        String preset = ((String)graphics.getSelectedItem()).toLowerCase(Locale.ROOT);
        config.put("graphics_preset", preset);
        double scale = new double[]{0.35,0.5,0.75,1.0}[resolution.getSelectedItemPosition()];
        int texture = textures.getSelectedItemPosition();
        int frame = new int[]{30,60,0}[fps.getSelectedItemPosition()];
        if (!"custom".equals(preset)) {
            double[] scales = {0.35,0.5,0.75,1.0,0.5};
            int[] tex = {0,0,1,2,texture};
            int[] frames = {30,60,60,60,frame};
            int idx = graphics.getSelectedItemPosition();
            scale = scales[idx]; texture = tex[idx]; frame = frames[idx];
        }
        config.put("scale", scale);
        config.put("resolution", String.format(Locale.US, "%dx%d", (int)(1280*scale), (int)(720*scale)));
        config.put("texture_quality", texture);
        config.put("fps_limit", frame);
        config.put("touch_sensitivity", 0.25 + sensitivity.getProgress() * 0.25);
        config.put("mouse_sensitivity", 2.0);
        config.put("mute", mute.isChecked());
        config.put("show_fps", showFps.isChecked());
        config.put("multiplayer", multiplayer.isChecked());
        config.put("host", session.getSelectedItemPosition() == 1);
        config.put("server", serverAddress.getText().toString().trim());
        config.put("player_name", playerName.getText().toString().trim());
        if (!config.has("extra_args")) config.put("extra_args", new JSONArray());
        root.mkdirs();
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            out.write(config.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private Spinner spinner(String[] entries) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, entries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private void addField(LinearLayout parent, String label, View child) {
        parent.addView(text(label, 14, true), matchWrap());
        parent.addView(child, matchWrap());
    }

    private Button button(String caption) {
        Button b = new Button(this); b.setText(caption); b.setAllCaps(false); return b;
    }
    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size);
        t.setTextColor(bold ? 0xfff2f2f2 : 0xffb9bbc2);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }
    private void style(View v) {
        if (v instanceof TextView) { ((TextView)v).setTextColor(0xffe2e2e8); }
        v.setPadding(dp(4), dp(6), dp(4), dp(6));
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams wrapWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private void deleteTree(File f) {
        if (!f.exists()) return;
        File[] children = f.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        f.delete();
    }

    private abstract static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable changed;
        SimpleItemSelectedListener(Runnable r) { changed = r; }
        public void onItemSelected(android.widget.AdapterView<?> p, View v, int position, long id) { changed.run(); }
        public void onNothingSelected(android.widget.AdapterView<?> p) {}
    }
}
