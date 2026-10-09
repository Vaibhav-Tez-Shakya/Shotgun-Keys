package com.shotgunkeys.app;

import android.inputmethodservice.InputMethodService;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;

public class ShotgunKeyboardService extends InputMethodService {
    private float volume = 0.7f;

    private void sound() {
        try {
            MediaPlayer p = new MediaPlayer();
            p.setDataSource(getAssets().openFd("shotgun.wav"));
            p.setVolume(volume, volume);
            p.setOnCompletionListener(MediaPlayer::release);
            p.setOnErrorListener((mp, what, extra) -> {
                mp.release();
                return true;
            });
            p.prepare();
            p.start();
        } catch (Exception e) {
            android.util.Log.e("ShotgunKeys", "Keyboard sound failed", e);
        }
    }

    private Button makeKey(String text, LinearLayout row, float weight) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(48, 50, 58));
        bg.setCornerRadius(12);
        b.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            0, 52, weight);
        lp.setMargins(3, 3, 3, 3);
        row.addView(b, lp);

        b.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            switch (text) {
                case "⌫":
                    if (ic != null) ic.deleteSurroundingText(1, 0);
                    break;
                case "Enter":
                    if (ic != null) ic.commitText("\n", 1);
                    break;
                case "Space":
                    if (ic != null) ic.commitText(" ", 1);
                    break;
                default:
                    if (ic != null) ic.commitText(text, 1);
            }
            sound();
        });
        return b;
    }

    private void addRow(LinearLayout root, String[] keys) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        root.addView(row);
        for (String key : keys) {
            float weight = key.equals("Space") ? 5f :
                (key.equals("Enter") || key.equals("⌫") ? 1.5f : 1f);
            makeKey(key, row, weight);
        }
    }

    @Override
    public View onCreateInputView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(4, 8, 4, 8);
        root.setBackgroundColor(Color.rgb(24, 25, 30));

        addRow(root, new String[]{"q","w","e","r","t","y","u","i","o","p"});
        addRow(root, new String[]{"a","s","d","f","g","h","j","k","l"});
        addRow(root, new String[]{"⌫","z","x","c","v","b","n","m","Enter"});
        addRow(root, new String[]{"Space"});

        Button soundToggle = new Button(this);
        soundToggle.setText("Keyboard sounds: ON");
        root.addView(soundToggle);
        soundToggle.setOnClickListener(v -> {
            volume = volume > 0 ? 0 : 0.7f;
            soundToggle.setText(volume > 0
                ? "Keyboard sounds: ON" : "Keyboard sounds: OFF");
        });
        return root;
    }
}
