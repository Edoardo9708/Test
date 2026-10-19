"""Expose explicit x265 standard tune; upstream defaults to SSIM and has no 'none' option."""
from pathlib import Path

path = Path('vendor/libheif-1.23.6/libheif/plugins/encoder_x265.cc')
source = path.read_text()
old = '"psnr", "ssim", "grain", "fastdecode", nullptr'
new = '"none", "psnr", "ssim", "grain", "fastdecode", nullptr'
call = 'api->param_default_preset(param, encoder->preset.c_str(), encoder->tune.c_str());'
patched = 'api->param_default_preset(param, encoder->preset.c_str(), encoder->tune == "none" ? nullptr : encoder->tune.c_str());'
if new not in source:
    assert source.count(old) == 1 and source.count(call) == 1
    source = source.replace(old, new).replace(call, patched)
    path.write_text(source)
assert new in source and patched in source
