"""Name the downloadable APK using the version embedded by Android Gradle."""
import json
import os
from pathlib import Path
import re
import shutil

folder = Path("app/build/outputs/apk/debug")
data = json.loads((folder / "output-metadata.json").read_text())
elements = data["elements"]
if len(elements) != 1:
    raise SystemExit("Expected one universal ARM64 APK")
version = elements[0]["versionName"]
if not re.fullmatch(r"[0-9]+(?:\.[0-9]+)*(?:[-+][A-Za-z0-9.-]+)?", version):
    raise SystemExit("Invalid version for download filename")
source = folder / elements[0]["outputFile"]
if source.resolve().parent != folder.resolve() or not source.is_file():
    raise SystemExit("Built APK not found in expected folder")
destination = Path("dist") / f"v{version}-CodecProbe.apk"
destination.parent.mkdir(exist_ok=True)
shutil.copyfile(source, destination)
with open(os.environ["GITHUB_OUTPUT"], "a") as output:
    output.write(f"version={version}\napk_path={destination.as_posix()}\n")
print(f"Download: {destination.name}")
