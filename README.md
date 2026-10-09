# Shotgun Keys

A Windows desktop application that plays shotgun sound effects using keyboard input.

## Features

- Keyboard-triggered sound playback
- Sound selection from WAV files
- Volume and mute controls
- Playback presets and statistics

## Requirements

- Windows
- Python 3
- Dependencies listed in `requirements.txt`

## Run from source

Open PowerShell in the project directory and run:

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe main.py
```

Keep `shotgun.wav` in the project directory. Other WAV files can be placed there for sound selection.

## Build the Windows executable

```powershell
.\.venv\Scripts\python.exe -m PyInstaller --clean --noconfirm ShotgunKeys.spec
```

The executable is generated in the `dist` directory.
