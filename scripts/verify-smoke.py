import json
from pathlib import Path

for crf in (24, 32):
    stream = json.loads(Path(f"smoke-{crf}.json").read_text())["streams"][0]
    assert (stream["width"], stream["height"], int(stream["nb_read_frames"])) == (1920, 1080, 30), stream
    print(f"CRF {crf}: decoded 30/30 frames at 1920x1080")


# Independently decode outputs of the stateful API used for real videos.
import subprocess
for crf in (0, 23, 51):
    path = f"stream-{crf}.hevc"
    subprocess.run(["ffmpeg", "-v", "error", "-xerror", "-i", path, "-f", "null", "-"], check=True)
    result = subprocess.check_output(["ffprobe", "-v", "error", "-count_frames", "-select_streams", "v:0",
        "-show_entries", "stream=width,height,nb_read_frames,color_primaries,color_transfer,color_space,color_range", "-of", "json", path], text=True)
    stream = json.loads(result)["streams"][0]
    assert (stream["width"], stream["height"], int(stream["nb_read_frames"])) == (320, 240, 12), stream
    expected = {0:("bt709","bt709","bt709","tv"),23:("smpte170m","smpte170m","smpte170m","pc"),51:("bt709","linear","bt709","tv")}[crf]
    actual = tuple(stream[k] for k in ("color_primaries","color_transfer","color_space","color_range"))
    assert actual == expected, (actual, expected)
    print(f"Streaming CRF {crf}: decoded 12/12 frames; exact color={actual}")

for b in (0, 1):
    for look in (0, 1):
        path = f"reorder-{b}-{look}.hevc"
        subprocess.run(["ffmpeg", "-v", "error", "-xerror", "-i", path, "-f", "null", "-"], check=True)
        data = json.loads(subprocess.check_output(["ffprobe", "-v", "error", "-count_frames", "-select_streams", "v:0", "-show_entries", "stream=width,height,nb_read_frames", "-show_frames", "-show_entries", "frame=pict_type", "-of", "json", path], text=True))
        stream = data["streams"][0]
        assert (stream["width"], stream["height"], int(stream["nb_read_frames"])) == (320, 240, 24), stream
        types = [frame["pict_type"] for frame in data["frames"]]
        assert ("B" in types) == bool(b), types
        print(f"B={b} lookahead={look}: decoded 24/24; actual B frames={types.count('B')}")
