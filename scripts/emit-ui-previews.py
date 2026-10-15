"""Expose small synthetic UI renders in CI logs so they can be visually reviewed remotely."""
from pathlib import Path
import base64

files = sorted(Path('app/build/reports/ui').glob('home-*.png'))
assert len(files) == 10, files
for path in files:
    data = path.read_bytes()
    assert data.startswith(b'\x89PNG\r\n\x1a\n') and len(data) < 500_000, path
    print('UI_PREVIEW_START ' + path.name)
    print(base64.b64encode(data).decode('ascii'))
    print('UI_PREVIEW_END ' + path.name)
