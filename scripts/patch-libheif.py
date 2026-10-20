"""Pinned libheif fixes: explicit x265 standard tune and thumbnail C API argument order."""
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

# 1.23.6 declares (master, thumbnail), but the C wrapper forwards the two handles
# backwards. Keep application calls consistent with the public API instead of
# compensating at each caller. The C++ convenience wrapper has the matching typo.
path = Path('vendor/libheif-1.23.6/libheif/api/libheif/heif_aux_images.cc')
source = path.read_text()
old = 'ctx->context->assign_thumbnail(thumbnail_image->image, master_image->image)'
new = 'ctx->context->assign_thumbnail(master_image->image, thumbnail_image->image)'
if new not in source:
    assert source.count(old) == 1
    path.write_text(source.replace(old, new))
assert new in path.read_text()

path = Path('vendor/libheif-1.23.6/libheif/api/libheif/heif_cxx.h')
source = path.read_text()
old = '''heif_context_assign_thumbnail(m_context.get(),
                                                    thumbnail_image.get_raw_image_handle(),
                                                    master_image.get_raw_image_handle())'''
new = '''heif_context_assign_thumbnail(m_context.get(),
                                                    master_image.get_raw_image_handle(),
                                                    thumbnail_image.get_raw_image_handle())'''
if new not in source:
    assert source.count(old) == 1
    path.write_text(source.replace(old, new))
assert new in path.read_text()
