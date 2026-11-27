package com.redux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(12, 12, 12);
    private static final int CARD = Color.rgb(23, 23, 23);
    private static final int RED = Color.rgb(242, 29, 53);
    private static final int WHITE = Color.rgb(244, 244, 244);
    private static final int MUTED = Color.rgb(166, 166, 166);
    private static final Pattern PACKAGE = Pattern.compile("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$");

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private ReduxAdbConnectionManager manager;
    private LinearLayout body;
    private TextView status;
    private TextView outputView;
    private EditText hostField, pairPortField, pairCodeField, connectPortField, packageField;
    private String lastOutput = "REDUX ready. Connect once; all panels will use the same ADB session.";
    private String selectedProfile = "ALL";
    private boolean adbConnected = false;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("redux_settings", MODE_PRIVATE);
        getWindow().setStatusBarColor(Color.rgb(8, 8, 8));
        getWindow().setNavigationBarColor(Color.rgb(8, 8, 8));
        renderShell();
        showPage("CONNECT");
    }

    @Override protected void onDestroy() {
        io.shutdownNow();
        if (manager != null) {
            try { manager.disconnect(); } catch (Exception ignored) { }
        }
        super.onDestroy();
    }

    private void renderShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(14), dp(6), dp(14), dp(10));

        TextView title = new TextView(this);
        title.setText("REDUX");
        title.setTextColor(RED);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(40)));

        TextView subtitle = new TextView(this);
        subtitle.setText("QUEST 3S  •  ONE ADB SESSION  •  UNIFIED TOOLKIT");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(10);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(23)));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(0, dp(6), 0, dp(6));
        nav.addView(navButton("CONNECT", "CONNECT"), weightParams());
        nav.addView(navButton("TOOLS", "TOOLS"), weightParams());
        nav.addView(navButton("APPS", "APPS"), weightParams());
        nav.addView(navButton("MODULES", "MODS"), weightParams());
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(50)));

        status = new TextView(this);
        status.setTextColor(MUTED);
        status.setTextSize(11);
        status.setPadding(dp(10), dp(5), dp(10), dp(5));
        status.setBackground(shape(CARD, Color.rgb(70, 25, 34), 1, 8));
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(29)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView footer = new TextView(this);
        footer.setText("ONE CONNECTION FOR ALL REDUX PANELS  •  GAME MODS NEED A COMPATIBLE BRIDGE");
        footer.setTextColor(MUTED);
        footer.setTextSize(8);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(6), 0, 0);
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(20)));
        setContentView(root);
    }

    private View navButton(String label, String target) {
        Button b = baseButton(label, false);
        b.setTextSize(9);
        b.setOnClickListener(v -> showPage(target));
        return b;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(37), 1f);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
    }

    private void showPage(String target) {
        body.removeAllViews();
        outputView = null;
        if ("CONNECT".equals(target)) renderConnectionPage();
        else if ("TOOLS".equals(target)) renderToolsPage();
        else if ("APPS".equals(target)) renderAppsPage();
        else renderModulesPage();
        addOutputBox();
        setStatus(adbConnected ? "●  CONNECTED / READY — SHARED SESSION" : "●  DISCONNECTED — CONNECT ONCE", adbConnected ? RED : MUTED);
    }

    private void renderConnectionPage() {
        addHeading("ONE ADB CONNECTION");
        addText("Pair once, connect once, then switch between Tools, Apps and the module profiles. REDUX keeps one ADB manager and reuses it; tab switching does not create more connections.", MUTED, 13);
        hostField = addField("Host / IP address", prefs.getString("host", "127.0.0.1"), InputType.TYPE_CLASS_TEXT);
        pairPortField = addField("Pairing port (temporary pairing dialog)", "", InputType.TYPE_CLASS_NUMBER);
        pairCodeField = addField("Six-digit pairing code", "", InputType.TYPE_CLASS_NUMBER);
        pairCodeField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        connectPortField = addField("Connection port (main Wireless Debugging page)", prefs.getString("connection_port", ""), InputType.TYPE_CLASS_NUMBER);
        addButton("OPEN WIRELESS DEBUGGING SETTINGS", true, this::openWirelessSettings);
        addButton("PAIR DEVICE (FIRST TIME / IF REVOKED)", true, this::doPair);
        addButton(adbConnected ? "ALREADY CONNECTED — SWITCH TABS" : "CONNECT ONCE / RECONNECT", false, this::doConnect);
        addButton("DISCONNECT REDUX SESSION", true, this::doDisconnect);
        addSpace(6);
        addText("For this headset, use 127.0.0.1. The pairing port and the connection port are different. Your pairing identity is kept in REDUX private storage; the connection port is saved for next time.", MUTED, 12);
    }

    private void renderToolsPage() {
        addHeading("DEVICE / DIAGNOSTIC TOOLS");
        addText("These are real preset ADB commands executed through the shared session. Results appear in OUTPUT below.", MUTED, 13);
        packageField = addField("Game / app package", prefs.getString("package", "com.AnotherAxiom.GorillaTag"), InputType.TYPE_CLASS_TEXT);
        addButton("CHECK ADB + DEVICE INFO", false, () -> runPreset("Reading device information…", "getprop ro.product.model; getprop ro.build.version.release; getprop ro.build.version.sdk; getprop ro.build.version.incremental"));
        addButton("LIST USER-INSTALLED PACKAGES", true, () -> runPreset("Listing packages…", "pm list packages -3"));
        addButton("CHECK SELECTED APP INSTALL", true, () -> withPackage(pkg -> runPreset("Checking package path…", "pm path " + pkg)));
        addButton("LAUNCH SELECTED APP", false, () -> withPackage(pkg -> runPreset("Launching app…", "monkey -p " + pkg + " -c android.intent.category.LAUNCHER 1")));
        addButton("FORCE-STOP SELECTED APP", true, this::confirmStopApp);
        addButton("CAPTURE LAST 250 LOG LINES", true, () -> runPreset("Collecting diagnostics…", "logcat -d -t 250"));
        addText("Device logs can contain app names or errors. Review them privately before sharing.", MUTED, 11);
    }

    private void renderAppsPage() {
        addHeading("APP LAUNCHER / MANAGER");
        addText("Inspect, launch, or open settings for apps on this Quest using the same ADB session. If an app does not launch, check its exact package name first.", MUTED, 13);
        packageField = addField("Android package name", prefs.getString("package", "com.AnotherAxiom.GorillaTag"), InputType.TYPE_CLASS_TEXT);
        addButton("OPEN SELECTED APP", false, () -> withPackage(pkg -> runPreset("Opening " + pkg + "…", "monkey -p " + pkg + " -c android.intent.category.LAUNCHER 1")));
        addButton("OPEN APP INFO / PERMISSIONS", true, () -> withPackage(pkg -> runPreset("Opening app details…", "am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:" + pkg)));
        addButton("CHECK PACKAGE INSTALL PATH", true, () -> withPackage(pkg -> runPreset("Checking package…", "pm path " + pkg)));
        addButton("LIST USER-INSTALLED APPS", true, () -> runPreset("Listing installed apps…", "pm list packages -3"));
        addButton("OPEN ANDROID SETTINGS", true, () -> runPreset("Opening settings…", "am start -a android.settings.SETTINGS"));
        addButton("FORCE-STOP SELECTED APP", true, this::confirmStopApp);
        addSpace(5);
        addText("ADB does not grant root or access to another app's private files. These actions target apps installed on the connected Quest.", MUTED, 11);
    }

    private void renderModulesPage() {
        addHeading("UNIFIED MODULE WORKSPACE");
        addText("Profile names are based on the labels found in the supplied Lunex/Ludex, Stripes ADB, Delta, and Moonlit V2 APKs. REDUX re-creates a clean workflow; it does not merge their private binaries. Every profile uses the same ADB session.", MUTED, 12);

        LinearLayout first = new LinearLayout(this);
        first.setOrientation(LinearLayout.HORIZONTAL);
        first.addView(profileButton("ALL", "ALL"), weightParams());
        first.addView(profileButton("LUNEX", "LUNEX"), weightParams());
        first.addView(profileButton("STRIPES ADB", "STRIPES"), weightParams());
        body.addView(first, new LinearLayout.LayoutParams(-1, dp(40)));

        LinearLayout second = new LinearLayout(this);
        second.setOrientation(LinearLayout.HORIZONTAL);
        second.addView(profileButton("DELTA", "DELTA"), weightParams());
        second.addView(profileButton("MOONLIT V2", "MOONLIT"), weightParams());
        second.addView(profileButton("DEVICE TOOLS", "DEVICE"), weightParams());
        body.addView(second, new LinearLayout.LayoutParams(-1, dp(40)));

        addHeading(selectedProfile.equals("ALL") ? "ALL PROFILE NOTES" : selectedProfile + " PROFILE");
        if ("LUNEX".equals(selectedProfile)) {
            addFeature("WIRELESS ADB", "Pairing code · pairing port · connection port · reconnect using REDUX's saved identity");
            addFeature("FEATURE LABELS FOUND", "Hover Fly · Joystick Fly · Velocity Fly · Long Arms · Teleports and Bursts");
        } else if ("STRIPES".equals(selectedProfile)) {
            addFeature("DEVICE WORKFLOWS", "Package list · launch / force-stop · device details · logcat diagnostics");
            addFeature("FEATURE LABELS FOUND", "Fly variants · Long Arms · Ghost Monke · Headbang");
        } else if ("DELTA".equals(selectedProfile)) {
            addFeature("QUICK ACTION LABELS", "Launch Gorilla Tag · force-stop package · connection and reconnect labels");
            addFeature("FEATURE LABELS FOUND", "Noclip Fly · Ghost Monke · Joystick Fly · Velocity Fly · Stick Fly");
        } else if ("MOONLIT".equals(selectedProfile)) {
            addFeature("DIFFERENT APP ARCHITECTURE", "Wireless-pairing vocabulary · movement-assist labels · larger auth/dependency surface");
            addFeature("INTEGRATION LIMIT", "Its native libraries and dependencies are not drop-in modules. A documented API or source project is needed to reuse its actual behavior.");
        } else if ("DEVICE".equals(selectedProfile)) {
            addFeature("LIVE ADB ACTIONS", "Device info · package list · launch app · app details · force-stop · logcat");
            addButton("OPEN APP LAUNCHER", false, () -> showPage("APPS"));
            addButton("RUN DEVICE CHECK", true, () -> runPreset("Checking device…", "getprop ro.product.model; getprop ro.build.version.release; pm list packages -3"));
        } else {
            addFeature("LIVE DEVICE ACTIONS", "One shared connection · app launch · package checks · device diagnostics");
            addFeature("GAME FEATURE INVENTORY ONLY", "Fly · Hover Fly · Noclip Fly · Long Arms · Ghost Monke · Wall Walk · Spider-Man · Headbang · visuals");
        }

        addText("Important: feature labels are not active in-game toggles. ADB can control the Android device, but it cannot change Unity movement or render a menu inside a game by itself. Real gameplay toggles need a compatible game-side plugin/bridge built for that specific game version. REDUX will not show fake ON states.", MUTED, 12);
        packageField = addField("Game / app package", prefs.getString("package", "com.AnotherAxiom.GorillaTag"), InputType.TYPE_CLASS_TEXT);
        addButton("CHECK GAME PACKAGE / POSSIBLE MOD RUNTIME", true, () -> withPackage(pkg -> runPreset("Checking app and installed runtimes…", "pm path " + pkg + "; pm list packages | grep -Ei 'questloader|modloader|modmenu|moonlit|lunex|stripes|bridge' || true")));
        addButton("OPEN SELECTED GAME", false, () -> withPackage(pkg -> runPreset("Launching selected game…", "monkey -p " + pkg + " -c android.intent.category.LAUNCHER 1")));
    }

    private View profileButton(String label, String profile) {
        Button b = baseButton(label, !selectedProfile.equals(profile));
        b.setTextSize(9);
        b.setOnClickListener(v -> {
            selectedProfile = profile;
            showPage("MODS");
        });
        return b;
    }

    private void addFeature(String title, String text) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(9), dp(12), dp(9));
        card.setBackground(shape(CARD, RED, 1, 10));
        TextView h = new TextView(this);
        h.setText(title);
        h.setTextColor(RED);
        h.setTextSize(12);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(h);
        TextView d = new TextView(this);
        d.setText(text);
        d.setTextColor(WHITE);
        d.setTextSize(11);
        d.setPadding(0, dp(4), 0, dp(3));
        card.addView(d);
        TextView foot = new TextView(this);
        foot.setText(title.startsWith("LIVE") ? "AVAILABLE THROUGH ADB" : "REFERENCE LABELS / NOT A LIVE GAME TOGGLE");
        foot.setTextColor(MUTED);
        foot.setTextSize(9);
        card.addView(foot);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(5));
        body.addView(card, lp);
    }

    private void openWirelessSettings() {
        Intent i = new Intent("android.settings.WIRELESS_DEBUGGING_SETTINGS");
        try { startActivity(i); }
        catch (Exception first) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception second) { toast("Open Settings > Developer options > Wireless debugging manually."); }
        }
    }

    private void doPair() {
        String host = hostField == null ? "127.0.0.1" : hostField.getText().toString().trim();
        String portText = pairPortField == null ? "" : pairPortField.getText().toString().trim();
        String code = pairCodeField == null ? "" : pairCodeField.getText().toString().trim();
        int port;
        try { port = validPort(portText); }
        catch (Exception e) { toast("Enter the temporary pairing port shown by Wireless Debugging."); return; }
        if (!validHost(host)) { toast("Enter a valid host/IP address."); return; }
        if (!code.matches("\\d{6}")) { toast("The pairing code must contain exactly six digits."); return; }
        prefs.edit().putString("host", host).apply();
        runTask("Pairing with Wireless Debugging…", () -> {
            boolean ok = getManager().pair(host, port, code);
            return ok ? "Pairing succeeded. Return to the main Wireless Debugging screen and enter its separate connection port." :
                    "Pairing failed. Check the current pairing code and temporary pairing port.";
        }, false);
    }

    private void doConnect() {
        String host = hostField == null ? prefs.getString("host", "127.0.0.1") : hostField.getText().toString().trim();
        String portText = connectPortField == null ? prefs.getString("connection_port", "") : connectPortField.getText().toString().trim();
        int port;
        try { port = validPort(portText); }
        catch (Exception e) { toast("Enter the connection port from the main Wireless Debugging page."); return; }
        if (!validHost(host)) { toast("Enter a valid host/IP address."); return; }
        if (adbConnected) { toast("Already connected. Switch tabs; all REDUX panels share this connection."); return; }
        prefs.edit().putString("host", host).putString("connection_port", portText).apply();
        runTask("Connecting to ADB…", () -> {
            ReduxAdbConnectionManager m = getManager();
            if (!m.connect(host, port)) return "Connection was not established. Check the current connection port and make sure REDUX is paired.";
            String model;
            try { model = new AdbCommandRunner(m).run("getprop ro.product.model"); }
            catch (Exception e) { model = "connected; device-info check unavailable (" + e.getClass().getSimpleName() + ")"; }
            return "CONNECTED to " + host + ":" + port + "\nDevice: " + model + "\n\nAll REDUX panels now share this session.";
        }, true);
    }

    private void doDisconnect() {
        adbConnected = false;
        runTask("Disconnecting…", () -> {
            if (manager != null) manager.disconnect();
            return "Disconnected. Wireless Debugging remains enabled until you turn it off in Android settings.";
        }, false);
    }

    private void confirmStopApp() {
        withPackage(pkg -> new AlertDialog.Builder(this)
                .setTitle("Force-stop selected app?")
                .setMessage("REDUX will stop this package on the connected device:\n\n" + pkg)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Force-stop", (dialog, which) -> runPreset("Stopping app…", "am force-stop " + pkg))
                .show());
    }

    private interface PackageAction { void run(String packageName); }

    private void withPackage(PackageAction action) {
        String pkg = packageField == null ? "" : packageField.getText().toString().trim();
        if (!PACKAGE.matcher(pkg).matches()) { toast("Enter a valid Android package name, e.g. com.example.game."); return; }
        prefs.edit().putString("package", pkg).apply();
        action.run(pkg);
    }

    private void runPreset(String message, String command) {
        if (!adbConnected) {
            lastOutput = "Not connected. Open CONNECT and tap CONNECT ONCE / RECONNECT first. Then return here; this panel shares that session.";
            if (outputView != null) outputView.setText(lastOutput);
            setStatus("●  CONNECT REQUIRED", MUTED);
            toast("Connect to ADB once first.");
            return;
        }
        runTask(message, () -> new AdbCommandRunner(getManager()).run(command), true);
    }

    private ReduxAdbConnectionManager getManager() throws Exception {
        if (manager == null) manager = new ReduxAdbConnectionManager(getApplicationContext());
        return manager;
    }

    private void runTask(String message, Task task, boolean connectedOnSuccess) {
        setStatus("●  " + message, MUTED);
        io.submit(() -> {
            String result;
            boolean ok = false;
            try {
                result = task.run();
                String low = result.trim().toLowerCase(Locale.ROOT);
                ok = !low.startsWith("error:") && !low.startsWith("pairing failed")
                        && !low.startsWith("connection was not established");
            } catch (Throwable e) {
                result = "ERROR: " + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage())
                        + "\n\nCheck the pairing/connection ports, keep Wireless Debugging enabled, and retry.";
            }
            final String shown = result;
            final boolean succeeded = ok;
            runOnUiThread(() -> {
                lastOutput = shown;
                if (connectedOnSuccess && succeeded) adbConnected = true;
                if (!succeeded && (shown.toLowerCase(Locale.ROOT).contains("connection was not established")
                        || shown.toLowerCase(Locale.ROOT).contains("not connected"))) adbConnected = false;
                setStatus(succeeded ? (adbConnected ? "●  CONNECTED / READY — SHARED SESSION" : "●  OPERATION COMPLETE")
                        : "●  CHECK OUTPUT", succeeded ? RED : MUTED);
                if (outputView != null) outputView.setText(lastOutput);
            });
        });
    }

    private interface Task { String run() throws Exception; }

    private void setStatus(String text, int color) {
        if (status != null) { status.setText(text); status.setTextColor(color); }
    }

    private void addOutputBox() {
        addSpace(10);
        addHeading("OUTPUT");
        outputView = new TextView(this);
        outputView.setText(lastOutput);
        outputView.setTextColor(WHITE);
        outputView.setTextSize(11);
        outputView.setTextIsSelectable(true);
        outputView.setPadding(dp(12), dp(12), dp(12), dp(12));
        outputView.setBackground(shape(Color.rgb(5, 5, 5), Color.rgb(70, 70, 70), 1, 8));
        body.addView(outputView, new LinearLayout.LayoutParams(-1, -2));
    }

    private EditText addField(String label, String value, int type) {
        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextColor(MUTED);
        caption.setTextSize(11);
        caption.setPadding(0, dp(6), 0, dp(4));
        body.addView(caption, new LinearLayout.LayoutParams(-1, -2));
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setText(value);
        field.setHint(label);
        field.setTextColor(WHITE);
        field.setHintTextColor(Color.rgb(100, 100, 100));
        field.setTextSize(14);
        field.setInputType(type | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setPadding(dp(10), dp(4), dp(10), dp(4));
        field.setBackground(shape(CARD, Color.rgb(90, 25, 34), 1, 8));
        if ("Game / app package".equals(label) || "Android package name".equals(label)) {
            field.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    prefs.edit().putString("package", s.toString()).apply();
                }
                @Override public void afterTextChanged(Editable e) { }
            });
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
        lp.setMargins(0, 0, 0, dp(3));
        body.addView(field, lp);
        return field;
    }

    private void addHeading(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(RED);
        v.setTextSize(16);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setPadding(0, dp(8), 0, dp(5));
        body.addView(v, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addText(String text, int color, int size) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(color);
        v.setTextSize(size);
        v.setLineSpacing(dp(2), 1f);
        body.addView(v, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addButton(String text, boolean secondary, Runnable action) {
        Button b = baseButton(text, secondary);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(44));
        lp.setMargins(0, dp(3), 0, dp(3));
        body.addView(b, lp);
    }

    private Button baseButton(String label, boolean secondary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(WHITE);
        b.setTextSize(11);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(shape(secondary ? CARD : RED, secondary ? Color.rgb(76, 27, 34) : RED, 1, 10));
        return b;
    }

    private GradientDrawable shape(int fill, int stroke, int width, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(width), stroke);
        return d;
    }

    private int validPort(String text) {
        int port = Integer.parseInt(text);
        if (port < 1 || port > 65535) throw new IllegalArgumentException("port out of range");
        return port;
    }

    private boolean validHost(String host) {
        return host != null && !host.isEmpty() && host.length() < 256
                && host.matches("[A-Za-z0-9_.:%-]+") && !host.contains("..") && !host.startsWith("-");
    }

    private void addSpace(int height) {
        View v = new View(this);
        body.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(float d) {
        return (int)(d * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
}
