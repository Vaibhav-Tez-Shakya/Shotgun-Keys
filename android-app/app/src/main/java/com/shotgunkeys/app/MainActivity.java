package com.shotgunkeys.app;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity {
    private float volume = 1f;
    private boolean muted = false;

    private void play(String file) {
        if (muted) return;
        try {
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(getAssets().openFd(file));
            player.setVolume(volume, volume);
            player.setOnCompletionListener(p -> {
                p.release();
            });
            player.setOnErrorListener((p, what, extra) -> {
                p.release();
                return true;
            });
            player.prepare();
            player.start();
        } catch (Exception e) {
            android.util.Log.e("ShotgunKeys", "Audio playback failed", e);
        }
    }

    private Button button(String label, LinearLayout parent) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            Color.rgb(255, 107, 53)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(6, 8, 6, 8);
        parent.addView(b, lp);
        return b;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(16, 17, 20));
        getWindow().setNavigationBarColor(Color.rgb(16, 17, 20));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 24, 20, 20);
        root.setBackgroundColor(Color.rgb(16, 17, 20));

        TextView title = new TextView(this);
        title.setText("SHOTGUN KEYS");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Soundboard  •  Keyboard sound effects");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 24);
        root.addView(subtitle);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(row1);

        Button fire = button("FIRE", row1);
        Button reload = button("RELOAD", row1);
        fire.setOnClickListener(v -> play("shotgun.wav"));
        reload.setOnClickListener(v -> play("dragon-studio-cinematic-shotgun-with-reload-467480.wav"));

        TextView volumeLabel = new TextView(this);
        volumeLabel.setText("VOLUME");
        volumeLabel.setTextColor(Color.WHITE);
        volumeLabel.setPadding(6, 24, 6, 4);
        root.addView(volumeLabel);

        SeekBar seek = new SeekBar(this);
        seek.setMax(100);
        seek.setProgress(100);
        root.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                volume = p / 100f;
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        Button mute = new Button(this);
        mute.setText("Mute audio");
        root.addView(mute);
        mute.setOnClickListener(v -> {
            muted = !muted;
            mute.setText(muted ? "Unmute audio" : "Mute audio");
        });

        TextView info = new TextView(this);
        info.setText("\nKeyboard setup\nSettings > System > Languages & input > On-screen keyboard\nEnable Shotgun Keys Keyboard, then select it when typing.");
        info.setTextColor(Color.LTGRAY);
        info.setPadding(6, 20, 6, 12);
        root.addView(info);

        Button test = new Button(this);
        test.setText("TEST SOUND");
        root.addView(test);
        test.setOnClickListener(v -> play("shotgun.wav"));

        setContentView(root);
    }
}
