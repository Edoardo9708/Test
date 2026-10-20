"""Expose small synthetic UI renders in CI logs so they can be visually reviewed remotely."""
from pathlib import Path
import base64

directory = Path('app/build/reports/ui')
home = sorted(directory.glob('home-*.png'))
audio = sorted(directory.glob('audio-*.png'))
images = sorted(directory.glob('image-*.png'))
assert len(home) == 10, home
assert len(audio) == 4, audio
assert len(images) == 5, images
destinations = sorted(directory.glob('destination-*.png'))
assert len(destinations) == 6, destinations
settings = sorted(directory.glob('settings-*.png'))
assert len(settings) == 1, settings
analysis = sorted(directory.glob("analysis-*.png"))
assert len(analysis) == 4, analysis
files = home + audio + images + destinations + settings + analysis
for path in files:
    data = path.read_bytes()
    assert data.startswith(b'\x89PNG\r\n\x1a\n') and len(data) < 500_000, path
    print('UI_PREVIEW_START ' + path.name)
    print(base64.b64encode(data).decode('ascii'))
    print('UI_PREVIEW_END ' + path.name)
