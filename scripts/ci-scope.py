"""Select expensive native smoke tests from the complete push diff; unknown paths run all."""
import json
import os
from pathlib import Path
import re
import subprocess

JAVA = 'app/src/main/java/it/edo/codecprobe/'
# These classes load native libraries or define the JNI/encoder boundary.
NATIVE_JAVA = {'EncoderPresetPreview.java', 'X265Probe.java', 'HdrProbe.java', 'HdrTranscoder.java'}


def classify(paths, force_full=False, native_boundary=False, version_only=False):
    paths = sorted(set(paths))
    if force_full or native_boundary or not paths:
        return {'build': True, 'native': True, 'reason': 'JNI boundary changed' if native_boundary else 'Full run requested or no reliable change list'}
    build = False
    for path in paths:
        if path.startswith('docs/') or path in {'README.md', 'LICENSE', 'LICENSE.md'}:
            continue
        build = True
        if path == 'app/build.gradle' and version_only:
            continue
        if path.startswith(JAVA) and path.endswith('.java'):
            if path[len(JAVA):] not in NATIVE_JAVA:
                continue
        elif path.startswith(('app/src/main/res/', 'app/src/test/')):
            continue
        elif path == 'scripts/emit-ui-previews.py':
            continue
        elif path.startswith('tests/') and path.endswith('.java') and path not in {
            'tests/HdrHeaderCheck.java', 'tests/HdrNativeMetadataCheck.java'
        }:
            continue
        return {'build': True, 'native': True, 'reason': 'Native/build/unknown dependency changed: ' + path}
    return {'build': build, 'native': False,
            'reason': 'Application/Java checks; native encoder unchanged' if build else 'Documentation only'}


def changes(event, event_name):
    if event_name == 'workflow_dispatch':
        return [], 'Manual run'
    before, after = event.get('before', ''), event.get('after', '')
    if not all(re.fullmatch(r'[0-9a-fA-F]{40}', sha or '') and set(sha) != {'0'} for sha in (before, after)):
        return [], 'Missing or initial commit: full run'
    try:
        result = subprocess.run(['git', 'diff', '--name-only', '--no-renames', '-z', before, after, '--'],
                                check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        return [path for path in result.stdout.decode('utf-8').split('\0') if path], 'Complete push diff'
    except (subprocess.CalledProcessError, UnicodeError, OSError):
        return [], 'Cannot read push diff: full run'




def only_version_change(event):
    try:
        diff = subprocess.run(['git', 'diff', '--unified=0', '--no-renames',
                               event['before'], event['after'], '--', 'app/build.gradle'],
                              check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout.decode('utf-8')
        lines = [line[1:].strip() for line in diff.splitlines()
                 if line.startswith(('+', '-')) and not line.startswith(('+++', '---'))]
        return bool(lines) and all(re.fullmatch(r"versionCode\s+[0-9]+|versionName\s+['\"][A-Za-z0-9.+-]+['\"]", line) for line in lines)
    except (KeyError, subprocess.CalledProcessError, UnicodeError, OSError):
        return False


def native_java_changes(event, paths):
    java = [path for path in paths if path.startswith(JAVA) and path.endswith('.java')]
    if not java:
        return False
    try:
        diff = subprocess.run(['git', 'diff', '--unified=0', '--no-renames',
                               event['before'], event['after'], '--', *java],
                              check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout.decode('utf-8')
        # Catch added/removed JNI declarations, native calls and library loads, including in new classes.
        return any(re.search(r'\bnative\b|\bnative[A-Z]\w*\s*\(|System\s*\.\s*load(?:Library)?\s*\(', line[1:])
                   for line in diff.splitlines() if line.startswith(('+', '-')) and not line.startswith(('+++', '---')))
    except (KeyError, subprocess.CalledProcessError, UnicodeError, OSError):
        return True


def main():
    try:
        event = json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())
        event_name = os.environ.get('GITHUB_EVENT_NAME', '')
        paths, source = changes(event, event_name)
        # Manual runs default to full, including if the input is absent on an older workflow.
        manual = event_name == 'workflow_dispatch'
        requested = str(event.get('inputs', {}).get('full_tests', 'true')).lower() == 'true'
        if manual and not requested:
            # An explicit quick manual build has no diff and intentionally skips native smoke tests.
            scope = {'build': True, 'native': False, 'reason': 'Manual quick application build'}
        else:
            scope = classify(paths, force_full=manual and requested, native_boundary=native_java_changes(event, paths),
                             version_only='app/build.gradle' in paths and only_version_change(event))
    except (KeyError, OSError, ValueError, TypeError, AttributeError):
        paths, source = [], 'Invalid event: full run'
        scope = classify([], True)
    print(json.dumps({'source': source, 'changed_files': paths, **scope}, indent=2))
    with open(os.environ['GITHUB_OUTPUT'], 'a') as out:
        for key in ('build', 'native'):
            out.write(f'{key}={str(scope[key]).lower()}\n')
    summary = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary:
        with open(summary, 'a') as out:
            out.write('## Test selection\n\n' + scope['reason'] + '\n\n')
            out.write(f'- Build, Java tests and APK verification: {scope["build"]}\n')
            out.write(f'- Native SDR / ARM64 / HDR smoke tests: {scope["native"]}\n')
            out.write(f'- Changed paths: {len(paths)} ({source})\n')


if __name__ == '__main__':
    main()
