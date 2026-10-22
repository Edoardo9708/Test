#!/usr/bin/env bash
set -euo pipefail
# GitHub's Azure mirror can stall individual package downloads for many minutes.
# Use Ubuntu's standard mirror and bound each network attempt on the CI runner.
sudo python3 - <<'PY'
from pathlib import Path
import re
root = Path('/etc/apt')
paths = [root / 'sources.list', *root.glob('sources.list.d/*.sources'), *root.glob('sources.list.d/*.list')]
for path in paths:
    if path.is_file():
        text = path.read_text()
        changed = re.sub(r'https?://azure\.archive\.ubuntu\.com/ubuntu/?',
                         'https://archive.ubuntu.com/ubuntu/', text)
        if changed != text:
            path.write_text(changed)
(root / 'apt.conf.d/80codecprobe-network').write_text(
    'Acquire::Retries "2";\nAcquire::http::Timeout "30";\n'
    'Acquire::https::Timeout "30";\nAcquire::ForceIPv4 "true";\n')
PY
