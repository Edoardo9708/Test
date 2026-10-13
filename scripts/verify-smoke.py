import json
from pathlib import Path

for crf in (24, 32):
    stream = json.loads(Path(f"smoke-{crf}.json").read_text())["streams"][0]
    assert (stream["width"], stream["height"], int(stream["nb_read_frames"])) == (1920, 1080, 30), stream
    print(f"CRF {crf}: decoded 30/30 frames at 1920x1080")
