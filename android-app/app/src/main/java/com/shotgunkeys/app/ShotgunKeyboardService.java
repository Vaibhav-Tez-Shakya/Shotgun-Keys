package com.shotgunkeys.app;

import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.inputmethodservice.InputMethodService;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

public class ShotgunKeyboardService extends InputMethodService {
    private static final int POOL_SIZE = 4;
    private final MediaPlayer[] players = new MediaPlayer[POOL_SIZE];
    private int poolIndex = 0;
    private boolean soundEnabled = true;
    private boolean isShifted = false;
    private final List<Button> letterKeys = new ArrayList<>();

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }

    private void initAudioPool() {
        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();

        for (int i = 0; i < POOL_SIZE; i++) {
            try {
                MediaPlayer mp = new MediaPlayer();
                mp.setAudioAttributes(attrs);
                AssetFileDescriptor afd = getAssets().openFd("shotgun.wav");
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mp.setVolume(1.0f, 1.0f);
                mp.prepare();
                players[i] = mp;
            } catch (Exception e) {
                android.util.Log.e("ShotgunKeys", "Pool init failed for slot " + i, e);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initAudioPool();
    }

    private void playShotgun() {
        if (!soundEnabled) return;
        try {
            MediaPlayer mp = players[poolIndex];
            poolIndex = (poolIndex + 1) % POOL_SIZE;
            if (mp != null) {
                if (mp.isPlaying()) {
                    mp.seekTo(0);
                } else {
                    mp.start();
                }
            } else {
                // Fallback instant player if slot wasn't initialized
                MediaPlayer fallback = new MediaPlayer();
                AssetFileDescriptor afd = getAssets().openFd("shotgun.wav");
                fallback.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                fallback.setOnCompletionListener(MediaPlayer::release);
                fallback.prepare();
                fallback.start();
            }
        } catch (Exception e) {
            android.util.Log.e("ShotgunKeys", "Key sound playback error", e);
        }
    }

    private StateListDrawable createKeyBackground(int normalColor, int pressedColor, int strokeColor) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(dp(8));
        if (strokeColor != 0) {
            normal.setStroke(dp(1), strokeColor);
        }

        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(pressedColor);
        pressed.setCornerRadius(dp(8));

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{}, normal);
        return states;
    }

    private Button makeKey(String label, LinearLayout row, float weight, boolean isAction, boolean isAccent) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, isAction ? 14 : 17);
        btn.setTextColor(Color.parseColor("#F5F6F8"));
        btn.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        btn.setAllCaps(false);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, 0, 0, 0);

        int bgNormal = isAccent ? Color.parseColor("#FF6B35") : (isAction ? Color.parseColor("#262930") : Color.parseColor("#32363F"));
        int bgPressed = isAccent ? Color.parseColor("#E85A24") : Color.parseColor("#4B515E");
        int border = isAccent ? Color.parseColor("#FFA573") : Color.parseColor("#3F4450");

        btn.setBackground(createKeyBackground(bgNormal, bgPressed, border));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), weight);
        int marginH = dp(3);
        int marginV = dp(4);
        lp.setMargins(marginH, marginV, marginH, marginV);
        row.addView(btn, lp);

        if (!isAction && label.length() == 1 && Character.isLetter(label.charAt(0))) {
            letterKeys.add(btn);
        }

        // Fire sound & haptics immediately on finger touch-down (ACTION_DOWN)
        btn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                playShotgun();
            }
            return false;
        });

        btn.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            switch (label) {
                case "DEL":
                    if (ic != null) ic.deleteSurroundingText(1, 0);
                    break;
                case "ENTER":
                    if (ic != null) ic.commitText("\n", 1);
                    break;
                case "SPACE":
                    if (ic != null) ic.commitText(" ", 1);
                    break;
                case "SHIFT":
                    isShifted = !isShifted;
                    for (Button b : letterKeys) {
                        String txt = b.getText().toString();
                        b.setText(isShifted ? txt.toUpperCase() : txt.toLowerCase());
                    }
                    break;
                default:
                    if (ic != null) {
                        ic.commitText(btn.getText().toString(), 1);
                    }
            }
        });
        return btn;
    }

    private void addRow(LinearLayout root, String[] keys) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        root.addView(row);

        for (String key : keys) {
            float weight = 1.0f;
            boolean isAction = false;
            boolean isAccent = false;

            if (key.equals("SPACE")) {
                weight = 4.2f;
                isAction = true;
            } else if (key.equals("ENTER")) {
                weight = 1.6f;
                isAction = true;
                isAccent = true;
            } else if (key.equals("DEL") || key.equals("SHIFT")) {
                weight = 1.4f;
                isAction = true;
            }
            makeKey(key, row, weight, isAction, isAccent);
        }
    }

    @Override
    public View onCreateInputView() {
        letterKeys.clear();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(6), dp(10), dp(6), dp(12));
        root.setBackgroundColor(Color.parseColor("#15171C"));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.END);
        toolbar.setPadding(dp(4), 0, dp(4), dp(4));

        Button soundToggle = new Button(this);
        soundToggle.setText(soundEnabled ? "SFX: ON" : "SFX: OFF");
        soundToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        soundToggle.setTextColor(soundEnabled ? Color.parseColor("#FFD166") : Color.parseColor("#8E929B"));
        soundToggle.setBackground(createKeyBackground(Color.parseColor("#20232A"), Color.parseColor("#2C313B"), Color.parseColor("#383E4A")));
        LinearLayout.LayoutParams tbParams = new LinearLayout.LayoutParams(dp(110), dp(32));
        toolbar.addView(soundToggle, tbParams);
        root.addView(toolbar);

        soundToggle.setOnClickListener(v -> {
            soundEnabled = !soundEnabled;
            soundToggle.setText(soundEnabled ? "SFX: ON" : "SFX: OFF");
            soundToggle.setTextColor(soundEnabled ? Color.parseColor("#FFD166") : Color.parseColor("#8E929B"));
            if (soundEnabled) playShotgun();
        });

        addRow(root, new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"});
        addRow(root, new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"});
        addRow(root, new String[]{"SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"});
        addRow(root, new String[]{"?123", ",", "SPACE", ".", "ENTER"});

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < POOL_SIZE; i++) {
            if (players[i] != null) {
                players[i].release();
                players[i] = null;
            }
        }
    }
}