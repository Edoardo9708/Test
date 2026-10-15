import json
import subprocess
from pathlib import Path
for transfer, name in [(16,'smpte2084'),(18,'arib-std-b67')]:
    source=f'hdr10-{transfer}.hevc'
    subprocess.run(['ffmpeg','-v','error','-xerror','-i',source,'-f','null','-'],check=True)
    data=json.loads(subprocess.check_output(['ffprobe','-v','error','-count_frames','-select_streams','v:0','-show_entries','stream=profile,pix_fmt,width,height,nb_read_frames,color_primaries,color_transfer,color_space,color_range','-of','json',source]))['streams'][0]
    assert data['profile']=='Main 10' and data['pix_fmt']=='yuv420p10le',data
    assert data['width']==320 and data['height']==180 and int(data['nb_read_frames'])==6,data
    assert data['color_primaries']=='bt2020' and data['color_transfer']==name and data['color_space']=='bt2020nc' and data['color_range']=='tv',data
    raw=subprocess.check_output(['ffmpeg','-v','error','-i',source,'-frames:v','1','-pix_fmt','yuv420p10le','-f','rawvideo','-'])
    words=[raw[i]|(raw[i+1]<<8) for i in range(0,len(raw),2)]
    assert max(words)<=1023 and any(v&3 for v in words),'Output reduced to eight bits'
    print('OK HDR10 x265:',data)
