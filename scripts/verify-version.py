"""Guard against manually duplicated runtime version labels."""
from pathlib import Path
import re
import json
for path in ["app/src/main/java/it/edo/codecprobe/MainActivity.java", "app/src/main/java/it/edo/codecprobe/VideoConverter.java", "app/src/main/java/it/edo/codecprobe/X265Probe.java"]:
    source=Path(path).read_text()
    assert "BuildConfig.VERSION_NAME" in source, path
    assert not re.search(r'v[0-9]+\.[0-9]+', source), f"Hardcoded runtime version in {path}"
data=json.loads(Path("app/build/outputs/apk/debug/output-metadata.json").read_text())
version=data["elements"][0]["versionName"]
assert Path(f"dist/v{version}-CodecProbe.apk").is_file()
print(f"Runtime, APK and artifact version source: {version}")
