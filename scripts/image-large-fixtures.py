"""Optional real 50/200 MP integration tests; run after building image_smoke.

These intentionally allocate large images. The normal suite checks header
boundaries without this cost. Optional arguments select individual cases.
"""
from pathlib import Path
import subprocess
import sys
from PIL import Image, ImageCms, ImageDraw

root = Path(__file__).resolve().parent.parent
engine = root / 'image-host-build/image_smoke'
directory = root / 'image-large-fixtures'
directory.mkdir(exist_ok=True)
cases = {'50mp': (8160, 6120, 1), '200mp': (16000, 12500, 6),
         'grid-odd': (16319, 8751, 8)}
colors = [(192, 48, 32), (48, 176, 80), (48, 80, 192), (192, 160, 48)]
for name in sys.argv[1:] or cases:
    width, height, orientation = cases[name]
    source, output, ppm = (directory / (name + ext) for ext in ('.jpg', '.heic', '.ppm'))
    image = Image.new('RGB', (width, height))
    draw = ImageDraw.Draw(image)
    for index, color in enumerate(colors):
        x, y = (index % 2) * (width // 2), (index // 2) * (height // 2)
        draw.rectangle((x, y, width - 1 if index % 2 else width // 2 - 1,
                        height - 1 if index // 2 else height // 2 - 1), fill=color)
    exif = Image.Exif()
    exif[274] = orientation
    image.save(source, quality=92, subsampling=2, exif=exif,
               icc_profile=ImageCms.ImageCmsProfile(ImageCms.createProfile('sRGB')).tobytes())
    del draw, image
    output.unlink(missing_ok=True)
    with (directory / (name + '.log')).open('w') as log:
        subprocess.run([str(engine), str(source), str(output), '60'],
                       stdout=log, stderr=subprocess.STDOUT, check=True)
    subprocess.run([str(engine), 'decode-oriented', str(output), str(ppm)], check=True)
    expected_width, expected_height = (height, width) if orientation >= 5 else (width, height)
    order = {1: [0, 1, 2, 3], 6: [2, 0, 3, 1], 8: [1, 3, 0, 2]}[orientation]
    try:
        with ppm.open('rb') as pixels:
            assert pixels.readline() == b'P6\n'
            assert list(map(int, pixels.readline().split())) == [expected_width, expected_height]
            assert pixels.readline() == b'255\n'
            start = pixels.tell()
            for index, color_index in enumerate(order):
                x = expected_width * (1 if index % 2 == 0 else 3) // 4
                y = expected_height * (1 if index // 2 == 0 else 3) // 4
                pixels.seek(start + (y * expected_width + x) * 3)
                actual = pixels.read(3)
                assert len(actual) == 3 and max(abs(a - b) for a, b in zip(actual, colors[color_index])) <= 8, (name, index, actual)
    finally:
        ppm.unlink(missing_ok=True)
    print('OK', name, width * height, 'pixels: full conversion, metadata, thumbnail and oriented RGB quadrants', flush=True)
