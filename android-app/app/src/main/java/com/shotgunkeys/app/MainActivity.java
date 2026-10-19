package com.shotgunkeys.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private float volume = 1.0f;
    private boolean muted = false;

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()
        );
    }

    private GradientDrawable cardBg(String hexBg, String hexStroke, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(hexBg));
        gd.setCornerRadius(dp(radiusDp));
        if (hexStroke != null) gd.setStroke(dp(1), Color.parseColor(hexStroke));
        return gd;
    }

    private void play(String file) {
        if (muted) return;
        try {
            MediaPlayer player = new MediaPlayer();
            var afd = getAssets().openFd(file);
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setVolume(volume, volume);
            player.setOnCompletionListener(MediaPlayer::release);
            player.setOnErrorListener((p, w, e) -> { p.release(); return true; });
            player.prepare();
            player.start();
        } catch (Exception e) {
            android.util.Log.e("ShotgunKeys", "Audio failed", e);
        }
    }

    private Button makeButton(String label, String bgHex, String strokeHex, LinearLayout parent) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        b.setBackground(cardBg(bgHex, strokeHex, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        parent.addView(b, lp);
        return b;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.parseColor("#0B0C10"));
        getWindow().setNavigationBarColor(Color.parseColor("#0B0C10"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(16));
        root.setBackgroundColor(Color.parseColor("#0B0C10"));

        TextView title = new TextView(this);
        title.setText("SHOTGUN KEYS");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#FF5722"));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("MECHANICAL TACTICAL KEYBOARD & SOUNDBOARD");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        subtitle.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        subtitle.setTextColor(Color.parseColor("#66FCF1"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        root.addView(subtitle);

        // Privacy Assurance Notice (Addressing Android's standard IME warning)
        TextView privacyCard = new TextView(this);
        privacyCard.setText("🔒 100% PRIVATE & OFFLINE\nAndroid shows a standard warning for all custom keyboards. Shotgun Keys contains ZERO internet permissions—your typed text never leaves your device.");
        privacyCard.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        privacyCard.setTextColor(Color.parseColor("#CFD8DC"));
        privacyCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        privacyCard.setBackground(cardBg("#141A23", "#1E88E5", 8));
        LinearLayout.LayoutParams pcLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pcLp.setMargins(dp(4), 0, dp(4), dp(12));
        root.addView(privacyCard, pcLp);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(row1);

        Button fire = makeButton("FIRE BLAST", "#FF5722", "#FF8A65", row1);
        Button reload = makeButton("RELOAD", "#1F2833", "#455A64", row1);
        fire.setOnClickListener(v -> play("shotgun.wav"));
        reload.setOnClickListener(v -> play("dragon-studio-cinematic-shotgun-with-reload-467480.wav"));

        TextView volLabel = new TextView(this);
        volLabel.setText("MASTER VOLUME: 100%");
        volLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        volLabel.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        volLabel.setTextColor(Color.parseColor("#66FCF1"));
        volLabel.setPadding(dp(4), dp(12), dp(4), dp(4));
        root.addView(volLabel);

        SeekBar seek = new SeekBar(this);
        seek.setMax(100);
        seek.setProgress(100);
        root.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                volume = p / 100f;
                volLabel.setText("MASTER VOLUME: " + p + "%");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(10), 0, dp(6));
        root.addView(row2);

        Button enableBtn = makeButton("1. Enable Keyboard", "#1F2833", "#37474F", row2);
        Button selectBtn = makeButton("2. Select Keyboard", "#1F2833", "#FF5722", row2);
        enableBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        selectBtn.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        });

        EditText testInput = new EditText(this);
        testInput.setHint("Test typing with shotgun sound effects here...");
        testInput.setHintTextColor(Color.parseColor("#546E7A"));
        testInput.setTextColor(Color.WHITE);
        testInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        testInput.setBackground(cardBg("#141A23", "#263238", 10));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        etLp.setMargins(dp(4), dp(8), dp(4), dp(8));
        root.addView(testInput, etLp);

        Button mute = makeButton("AUDIO FEEDBACK: ACTIVE", "#141A23", "#37474F", root);
        LinearLayout.LayoutParams muteLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        muteLp.setMargins(dp(4), dp(4), dp(4), dp(4));
        mute.setLayoutParams(muteLp);
        mute.setOnClickListener(v -> {
            muted = !muted;
            mute.setText(muted ? "AUDIO FEEDBACK: MUTED" : "AUDIO FEEDBACK: ACTIVE");
            mute.setTextColor(muted ? Color.parseColor("#FF5252") : Color.WHITE);
        });

        setContentView(root);
    }
}