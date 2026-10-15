"""Keep the Android entry point and section builders free of operational work."""
from pathlib import Path
import re

root = Path('app/src/main/java/it/edo/codecprobe')
entry = (root / 'MainActivity.java').read_text()
for forbidden in ['new Button(', 'new LinearLayout(', 'ExecutorService', 'MediaCodec', 'FileInputStream', 'nativePresetSettings']:
    assert forbidden not in entry, ('Operational code in Activity', forbidden)
for path in [*root.glob('Home*Section.java'),root / 'HomeDirectoryRow.java',root / 'QualityButtons.java']:
    source = path.read_text()
    for forbidden in ['getContentResolver(', 'startActivityForResult(', 'ConversionService.snapshot(', 'System.loadLibrary(', 'new VideoConverter(']:
        assert forbidden not in source, (path, forbidden)
state = (root / 'HomeState.java').read_text()
for forbidden in ['TextView', 'Button', 'Activity ', 'ExecutorService', 'AlertDialog']:
    assert forbidden not in state, ('View/worker leaked into selection state', forbidden)
for path in root.glob('Home*.java'):
    assert not re.search(r'v[0-9]+\.[0-9]+', path.read_text()), ('Hardcoded app version', path)
print('OK UI boundaries: Activity delegates; section builders own views; state contains no views or workers')
