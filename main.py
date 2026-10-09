import os
import sys
import time
import random
import threading
import tkinter as tk
from tkinter import ttk

import pygame
from pynput import keyboard

BASE_DIR = os.path.dirname(os.path.abspath(
    sys.executable if getattr(sys, "frozen", False) else __file__
))
RESOURCE_DIR = getattr(sys, "_MEIPASS", BASE_DIR)
SOUND_DIR = os.path.join(BASE_DIR, "sounds")
RESOURCE_SOUND_DIR = os.path.join(RESOURCE_DIR, "sounds")
DEFAULT_SOUND = os.path.join(RESOURCE_DIR, "shotgun.wav")

pygame.mixer.init(frequency=44100, size=-16, channels=2, buffer=512)
pygame.mixer.set_num_channels(32)

sounds = {}
active_channels = []
lock = threading.Lock()
listener = None
selected_sound = "Random"
current_preset = "Normal"
cooldown_seconds = 0.12
volume_level = 0.70
muted = False
last_play_time = 0.0
session_start = None

stats = {"key_presses": 0, "sounds_played": 0, "skipped": 0}

PRESETS = {
    "Normal": 0.12,
    "Heavy Shotgun": 0.35,
    "Rapid Fire": 0.0,
}


def load_sounds():
    candidates = []
    seen = set()

    for folder in (SOUND_DIR, RESOURCE_SOUND_DIR):
        if os.path.isdir(folder):
            for name in sorted(os.listdir(folder)):
                if name.lower().endswith(".wav"):
                    candidates.append(os.path.join(folder, name))

    candidates.append(DEFAULT_SOUND)

    for path in candidates:
        if not os.path.isfile(path):
            continue

        key = os.path.normcase(os.path.abspath(path))
        if key in seen:
            continue
        seen.add(key)

        try:
            label = os.path.splitext(os.path.basename(path))[0]
            unique = label
            number = 2
            while unique in sounds:
                unique = f"{label} ({number})"
                number += 1
            sounds[unique] = pygame.mixer.Sound(path)
        except Exception as exc:
            print(f"Could not load {path}: {exc}")


def apply_volume():
    volume = 0.0 if muted else volume_level

    for sound in sounds.values():
        sound.set_volume(volume)

    for channel in active_channels[:]:
        try:
            if channel.get_busy():
                channel.set_volume(volume)
            else:
                active_channels.remove(channel)
        except Exception:
            pass


def start_listening():
    global listener, last_play_time, session_start

    if not sounds:
        status.config(text="Error: No playable WAV sound found")
        return

    if listener is not None and listener.is_alive():
        return

    with lock:
        stats.update(key_presses=0, sounds_played=0, skipped=0)
        last_play_time = 0.0
        session_start = time.monotonic()

    def on_press(key):
        global last_play_time

        now = time.monotonic()

        with lock:
            stats["key_presses"] += 1

            if now - last_play_time < cooldown_seconds:
                stats["skipped"] += 1
                return

            if selected_sound == "Random":
                sound = random.choice(list(sounds.values()))
            else:
                sound = sounds.get(selected_sound)

            if sound is None:
                return

            sound.set_volume(0.0 if muted else volume_level)
            channel = sound.play()

            if channel is not None:
                channel.set_volume(0.0 if muted else volume_level)
                active_channels.append(channel)
                stats["sounds_played"] += 1
                last_play_time = now

    try:
        listener = keyboard.Listener(on_press=on_press)
        listener.start()
        status.config(text="Status: Listening")
    except Exception as exc:
        listener = None
        status.config(text=f"Listener error: {exc}")


def stop_listening():
    global listener, session_start

    old_listener = listener
    listener = None

    if old_listener is not None:
        old_listener.stop()
        if old_listener is not threading.current_thread():
            old_listener.join(timeout=1.0)

    pygame.mixer.stop()
    active_channels.clear()
    session_start = None
    status.config(text="Status: Stopped")


def update_preset(event=None):
    global current_preset, cooldown_seconds

    current_preset = preset_var.get()
    cooldown_seconds = PRESETS[current_preset]

    labels = {
        "Normal": "120 ms cooldown",
        "Heavy Shotgun": "350 ms cooldown",
        "Rapid Fire": "No cooldown",
    }
    status.config(text=f"Preset: {current_preset} | {labels[current_preset]}")


def change_sound(event=None):
    global selected_sound
    selected_sound = sound_var.get()


def change_volume(value):
    global volume_level
    with lock:
        volume_level = float(value) / 100.0
        apply_volume()


def toggle_mute():
    global muted
    with lock:
        muted = not muted
        apply_volume()
    mute_button.config(text="Unmute" if muted else "Mute")


def refresh_stats():
    with lock:
        presses = stats["key_presses"]
        played = stats["sounds_played"]
        skipped = stats["skipped"]
        started = session_start

    elapsed = int(time.monotonic() - started) if started else 0
    minutes, seconds = divmod(elapsed, 60)

    stats_label.config(
        text=f"Key presses: {presses}   Sounds played: {played}\n"
             f"Cooldown skips: {skipped}   Session: {minutes:02}:{seconds:02}"
    )
    root.after(250, refresh_stats)


def close_app():
    stop_listening()
    pygame.mixer.quit()
    root.destroy()


load_sounds()

root = tk.Tk()
root.title("ShotgunKeys")
root.geometry("440x440")
root.resizable(False, False)
root.protocol("WM_DELETE_WINDOW", close_app)

tk.Label(root, text="ShotgunKeys", font=("Segoe UI", 19, "bold")).pack(pady=(12, 2))
tk.Label(root, text="System-wide keyboard sound effects").pack(pady=(0, 8))

controls = tk.Frame(root)
controls.pack(pady=3)
tk.Button(controls, text="Start Listening", width=17, command=start_listening).grid(row=0, column=0, padx=4)
tk.Button(controls, text="Stop Listening", width=17, command=stop_listening).grid(row=0, column=1, padx=4)

status = tk.Label(root, text="Status: Stopped")
status.pack(pady=6)

tk.Label(root, text="Sound selection").pack()
sound_var = tk.StringVar(value="Random")
sound_combo = ttk.Combobox(
    root, textvariable=sound_var,
    values=["Random"] + list(sounds.keys()),
    state="readonly", width=30
)
sound_combo.pack(pady=3)
sound_combo.bind("<<ComboboxSelected>>", change_sound)

tk.Label(root, text="Preset").pack(pady=(5, 0))
preset_var = tk.StringVar(value="Normal")
preset_combo = ttk.Combobox(
    root, textvariable=preset_var,
    values=list(PRESETS.keys()), state="readonly", width=30
)
preset_combo.pack(pady=3)
preset_combo.bind("<<ComboboxSelected>>", update_preset)

tk.Label(root, text="Volume").pack(pady=(5, 0))
volume_slider = tk.Scale(
    root, from_=0, to=100, orient="horizontal",
    length=250, command=change_volume
)
volume_slider.set(70)
volume_slider.pack()

mute_button = tk.Button(root, text="Mute", width=12, command=toggle_mute)
mute_button.pack(pady=3)

stats_label = tk.Label(root, text="Key presses: 0   Sounds played: 0\nCooldown skips: 0   Session: 00:00")
stats_label.pack(pady=7)

if not sounds:
    status.config(text="Error: shotgun.wav not found")

refresh_stats()
root.mainloop()
