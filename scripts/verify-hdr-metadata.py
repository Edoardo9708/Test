"""Decoder interoperability check after Java inserts the original Samsung HDR10+ payloads."""
import json
import subprocess

for source in ['hdr10-preserved.hevc', 'hdr10-preserved.mp4']:
    data = json.loads(subprocess.check_output([
        'ffprobe', '-v', 'error', '-select_streams', 'v:0', '-show_frames', '-show_streams', '-of', 'json', source]))
    stream = data['streams'][0]
    assert stream['profile'] == 'Main 10' and stream['pix_fmt'] == 'yuv420p10le', stream
    assert stream['color_transfer'] == 'smpte2084' and stream['color_primaries'] == 'bt2020', stream
    frames = data['frames']
    assert len(frames) == 72, len(frames)
    dynamic = sum(any('HDR10+' in side.get('side_data_type', '') for side in frame.get('side_data_list', [])) for frame in frames)
    assert dynamic == 72, (source, dynamic)
    assert any('Mastering display' in side.get('side_data_type', '') for side in frames[0].get('side_data_list', [])), frames[0]
    print('OK decoder HDR10+:', source, dynamic, 'frames; Main10/PQ/BT2020; static mastering metadata present')

subprocess.run(['ffmpeg', '-v', 'error', '-i', 'hdr10-preserved.mp4', '-c', 'copy', '-bsf:v', 'hevc_mp4toannexb', '-f', 'hevc', 'hdr10-remuxed.hevc'], check=True)
subprocess.run(['java', '-cp', 'yuv-test', 'it.edo.codecprobe.HdrNativeMetadataCheck', 'verify-remux'], check=True)
