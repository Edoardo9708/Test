"""Real MP4 HEVC packets, B-frames, VFR, PQ/HLG; bit-exact comparison with system FFmpeg."""
import json, re, struct, subprocess
from pathlib import Path
root=Path(__file__).resolve().parent.parent
out=root/'video-fixtures'; out.mkdir(exist_ok=True)
def run(*args): subprocess.run(args,check=True)
def data(s):
    return bytes.fromhex(''.join(line.split(':',1)[1].split('  ')[0].replace(' ','') for line in s.strip().splitlines()))
for transfer in (16,18):
    base=out/str(transfer)
    run('ffmpeg','-v','error','-y','-f','lavfi','-i','testsrc2=s=64x48:r=40:d=0.9','-vf',"select='not(mod(n,2))',format=yuv420p10le,geq=lum='p(X,Y)+mod(X+Y,4)':cb='p(X,Y)+mod(X+Y,4)':cr='p(X,Y)+mod(X+Y,4)',settb=1/1000000,setpts=PTS+floor(N/3)*7000",'-fps_mode','vfr','-enc_time_base','1:1000000','-c:v','libx265','-x265-params',f'pools=1:frame-threads=1:bframes=3:lossless=1:colorprim=9:transfer={transfer}:colormatrix=9:range=limited:log-level=error',str(base)+'.mp4')
    probe=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_packets','-show_streams','-show_data','-of','json',str(base)+'.mp4']))
    hvcc=data(probe['streams'][0]['extradata']); offset=23; headers=bytearray()
    for _ in range(hvcc[22]):
        count=int.from_bytes(hvcc[offset+1:offset+3],'big');offset+=3
        for _ in range(count):
            n=int.from_bytes(hvcc[offset:offset+2],'big');offset+=2;headers+=b'\0\0\0\1'+hvcc[offset:offset+n];offset+=n
    with open(str(base)+'.bin','wb') as f:
        f.write(struct.pack('<I',len(headers))+headers)
        packets=probe['packets'];pts=[round(float(p['pts_time'])*1e6) for p in packets]
        assert pts!=sorted(pts),'Fixture must contain reordered B-frames'
        for i,p in enumerate(packets):
            packet=data(p['data'])
            # Exercise both Android packet layouts.
            if i%2:
                annex=bytearray();pos=0
                while pos<len(packet):
                    n=int.from_bytes(packet[pos:pos+4],'big');pos+=4;annex+=b'\0\0\0\1'+packet[pos:pos+n];pos+=n
                packet=annex
            f.write(struct.pack('<qI',pts[i],len(packet))+packet)
    run(str(root/'video-host-build/smoke'),str(base)+'.bin',str(base)+'.yuv',str(transfer))
    reference=subprocess.check_output(['ffmpeg','-v','error','-i',str(base)+'.mp4','-fps_mode','passthrough','-f','rawvideo','-pix_fmt','yuv420p10le','-'])
    assert reference==Path(str(base)+'.yuv').read_bytes(),'Decoded pixels differ from independent FFmpeg'
    print(f'OK transfer={transfer}: all decoded samples bit-exact with independent FFmpeg')
