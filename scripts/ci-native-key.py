"""Fingerprint every tracked native/build input; app version and Java UI changes may reuse CMake outputs."""
import hashlib
import os
from pathlib import Path
import re
import subprocess

VERSION_LINE = re.compile(rb"\s*(?:versionCode\s+[0-9]+|versionName\s+['\"][A-Za-z0-9.+-]+['\"])\s*")
BUILD_FILES = {'app/build.gradle', 'build.gradle', 'settings.gradle', 'gradle.properties',
               '.github/workflows/android.yml', 'scripts/ci-native-key.py'}


def native_input(path):
    return (path in BUILD_FILES or path.startswith(('app/src/main/cpp/', 'vendor/'))
            or path.startswith('scripts/') and Path(path).name.startswith(('prepare-', 'patch-', 'build-')))


def fingerprint(root, paths, environment):
    digest = hashlib.sha256()
    # CMake outputs contain absolute paths. Do not restore them into a different toolchain location.
    digest.update(repr(sorted(environment.items())).encode())
    for path in sorted(set(paths)):
        if not native_input(path):
            continue
        file = root / path
        data = file.read_bytes() if file.is_file() else b'<missing>'
        if path == 'app/build.gradle':
            data = b'\n'.join(line for line in data.splitlines() if not VERSION_LINE.fullmatch(line))
        digest.update(path.encode() + b'\0' + str(len(data)).encode() + b'\0' + data)
    return digest.hexdigest()


def main():
    root = Path.cwd()
    paths = subprocess.check_output(['git', 'ls-files', '-z']).decode().split('\0')
    environment = {key: os.environ.get(key, '') for key in ('ANDROID_HOME', 'RUNNER_OS', 'RUNNER_ARCH', 'ImageOS')}
    environment['workspace'] = str(root.resolve())
    key = fingerprint(root, paths, environment)
    print('Native cache fingerprint: ' + key)
    with open(os.environ['GITHUB_OUTPUT'], 'a') as out:
        out.write('key=' + key + '\n')


if __name__ == '__main__':
    main()
