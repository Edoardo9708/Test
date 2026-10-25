#!/usr/bin/env python3
"""Bounded, offline local JVM suites. Detailed output stays under .tools/test-results."""
import argparse
import fnmatch
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA = 'app/src/main/java/it/edo/codecprobe/'
TEST = 'app/src/test/java/it/edo/codecprobe/'
MANIFEST = json.loads((ROOT / 'scripts/local-test-suites.json').read_text())
RESULTS = ROOT / '.tools/test-results'


def snapshot():
    command = ['git', '-c', f'safe.directory={ROOT.as_posix()}', 'ls-files', '--cached', '--others', '--exclude-standard', '-z']
    names = subprocess.check_output(command, cwd=ROOT).decode('utf-8').split('\0')
    paths = {ROOT / name for name in names if name}
    paths.update(p for p in (ROOT / 'app/src').rglob('*') if p.is_file())
    paths.add(ROOT / 'local.properties')
    return {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted(paths) if p.is_file()}


def select_changes(paths):
    paths = [path.replace('\\', '/') for path in paths]
    groups = set()
    native = any(path.startswith(('app/src/main/cpp/', 'vendor/')) or
                 (path.startswith('scripts/') and any(token in Path(path).stem for token in ('native', 'prepare-', 'build-', 'image-fixtures')))
                 for path in paths)
    for path in paths:
        path = path.replace('\\', '/')
        name = Path(path).stem
        if path.startswith('docs/') or path in ('AGENTS.md', 'PROJECT_CONTEXT.md', 'README.md'):
            continue
        if path.startswith(('app/src/main/cpp/', 'vendor/')) or (path.startswith('scripts/') and any(token in name for token in ('native', 'prepare-', 'build-', 'image-fixtures'))):
            return None, True
        if name in {'MediaDestinationsController', 'DestinationPlan', 'AudioMetadataFlowTest', 'ImageWorkspaceTest', 'FileAnalysisChecks', 'SamsungSefMotionFixtures'}:
            return None, native
        if name in {'ImageOutput', 'ImageOutputTest', 'ImageOutputFailureTest', 'OutputDocuments', 'OutputCollisionTest'}:
            return None, native
        # Prefixes do not establish ownership: AudioCopy and JpegInventory also serve other sections.
        if path.startswith(JAVA):
            if name in {'PhotoWorkspace', 'PhotoComponents', 'PhotoFilesPane', 'PhotoCompressionPane', 'PhotoDetailsPane'}:
                groups.add('photo'); continue
            return None, native
        found = False
        for group, entries in MANIFEST['fast'].items():
            if name in entries['sources'] + entries['tests']:
                if group == 'control':
                    return None, native  # Lease/pause gate shared by all engines.
                groups.add(group); found = True
        if found:
            continue
        if path.startswith((JAVA, TEST)):
            for prefix, group in (('Audio', 'audio'), ('Image', 'photo'), ('Photo', 'photo'), ('Jpeg', 'photo'), ('Home', 'video'), ('Video', 'video'), ('Hdr', 'video'), ('Analysis', 'analysis')):
                if name.startswith(prefix):
                    groups.add(group); found = True; break
        if not found:
            return None, native
    return sorted(groups), native


def normalized_paths(values):
    result = []
    for value in values:
        path = (ROOT / value.replace('\\', '/')).resolve()
        if not path.is_relative_to(ROOT):
            raise ValueError('Changed paths must stay inside the repository')
        result.append(path.relative_to(ROOT).as_posix())
    return result


def changed_files(current):
    baseline = RESULTS / 'last-full.json'
    if baseline.is_file():
        previous = json.loads(baseline.read_text())
        return [p for p in current.keys() | previous.keys() if current.get(p) != previous.get(p)]
    command = ['git', '-c', f'safe.directory={ROOT.as_posix()}', 'diff', '--name-only', 'HEAD', '--']
    changed = subprocess.check_output(command, cwd=ROOT, text=True).splitlines()
    changed += subprocess.check_output(command[:3] + ['ls-files', '--others', '--exclude-standard'], cwd=ROOT, text=True).splitlines()
    return changed


def reports(directory, excluded=(), patterns=('*',)):
    counts = dict(tests=0, failures=0, errors=0, skipped=0, test_seconds=0.0)
    failures = []
    for path in sorted(directory.glob('TEST-*.xml')):
        suite = ET.parse(path).getroot()
        name = suite.attrib.get('name', '').split('.')[-1]
        if name in excluded or not any(fnmatch.fnmatchcase(name, pattern) for pattern in patterns):
            continue
        for key in ('tests', 'failures', 'errors', 'skipped'):
            counts[key] += int(suite.attrib.get(key, 0))
        counts['test_seconds'] += float(suite.attrib.get('time', 0))
        for case in suite.findall('testcase'):
            for tag in ('failure', 'error'):
                failure = case.find(tag)
                if failure is not None:
                    failures.append({'test': name + '.' + case.attrib.get('name', '?'),
                                     'message': failure.attrib.get('message', '')[:700],
                                     'details': (failure.text or '')[:5000]})
    return counts | {'failed_cases': failures}


def environment():
    env = os.environ.copy()
    jdk = ROOT / 'vendor/jdk17'
    if (jdk / 'bin/java.exe').is_file() or (jdk / 'bin/java').is_file():
        env['JAVA_HOME'] = str(jdk)
    if not env.get('JAVA_HOME'):
        raise ValueError('Set JAVA_HOME to the existing JDK 17; no tools are installed by this script')
    env['PATH'] = str(Path(env['JAVA_HOME']) / 'bin') + os.pathsep + env.get('PATH', '')
    return env


def execute(kind, groups, files, env):
    stamp = time.strftime('%Y%m%d-%H%M%S') + '-' + str(time.time_ns())[-6:]
    run = RESULTS / 'runs' / (stamp + '-' + kind)
    run.mkdir(parents=True)
    task = ':app:localFastTest' if kind == 'fast' else ':app:testDebugUnitTest'
    command = [str(ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew')), '--offline', '--console=plain',
               '--no-configuration-cache', '--no-parallel', '--max-workers=2',
               '-Dorg.gradle.jvmargs=-Xmx1024m -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8',
               '-Pkotlin.compiler.execution.strategy=in-process', '-I', str(ROOT / 'scripts/local-tests.gradle'), task,
               '-Plocal.test.status=' + str(run / 'task-status.json')]
    if groups:
        if kind == 'fast':
            command += ['-Plocal.fast.groups=' + ','.join(groups)]
        else:
            patterns = sorted({pattern for group in groups for pattern in MANIFEST['integration'][group]})
            command += ['-Plocal.integration.patterns=' + ','.join(patterns)]
    started = time.perf_counter()
    epoch = time.time()
    log = run / 'gradle.log'
    with log.open('w', encoding='utf-8') as stream:
        process = subprocess.run(command, cwd=ROOT, env=env, stdout=stream, stderr=subprocess.STDOUT)
    wall = time.perf_counter() - started
    original = ROOT / 'app/build/test-results' / ('localFastTest' if kind == 'fast' else 'testDebugUnitTest')
    archive = run / 'xml'
    archive.mkdir()
    for path in original.glob('TEST-*.xml'):
        if not process.returncode or path.stat().st_mtime >= epoch - 1:
            shutil.copy2(path, archive / path.name)
    excluded = {name for values in MANIFEST['fast'].values() for name in values['tests']} if kind != 'fast' else set()
    patterns = sorted({p for g in groups for p in MANIFEST['integration'][g]}) if kind != 'fast' and groups else ['*']
    result = reports(archive, excluded, patterns)
    status_file = run / 'task-status.json'
    task_status = json.loads(status_file.read_text()) if status_file.is_file() else {}
    # A compilation failure must never be represented by stale passing XML from an older run.
    reused = task_status.get('upToDate', False) or task_status.get('skipMessage') == 'FROM-CACHE'
    observed_cases = task_status.get('executedCases', 0)
    result.update(kind=kind, groups=groups, exit_code=process.returncode, wall_seconds=round(wall, 3),
                  gradle_reused=reused, executed_cases=0 if reused else max(0, observed_cases-result['skipped']), log=str(log), xml=str(archive))
    valid_status = task_status.get('task') == task and not task_status.get('noSource') and not task_status.get('failed')
    if not process.returncode and (not valid_status or (task_status.get('skipped') and not reused) or
                                  (not reused and observed_cases != result['tests'])):
        result.update(exit_code=2, build_errors=['Test task status/count does not confirm these reports; verification incomplete'])
    if process.returncode and (not (run / 'task-status.json').is_file() or not result['failed_cases']):
        result.update(tests=0, failures=0, errors=0, skipped=0, test_seconds=0.0, build_failed=True)
        lines = log.read_text(encoding='utf-8', errors='replace').splitlines()
        result['build_errors'] = [line[:500] for line in lines if any(token in line for token in ('error:', 'What went wrong', 'Execution failed', '> ', 'Error:', 'Caused by:'))][-12:]
    if not process.returncode and not result['tests']:
        result['exit_code'] = 2
        result['build_errors'] = ['No matching JUnit reports: verification is incomplete']
    (run / 'summary.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
    return result


def main(argv=None):
    started = time.perf_counter()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('suite', choices=['fast', 'integration', 'full', 'changed', 'plan'])
    parser.add_argument('--group', action='append', choices=sorted(MANIFEST['integration']))
    parser.add_argument('--files', nargs='+', help='Repository-relative changed paths; no reliance on a clean Git tree')
    args = parser.parse_args(argv)
    files = snapshot()
    groups = args.group or []
    native = False
    if args.suite in ('changed', 'plan'):
        paths = normalized_paths(args.files) if args.files else changed_files(files)
        groups, native = select_changes(paths)
        # An explicit list must not conceal other unverified edits in the working tree.
        pending = changed_files(files)
        if args.files and pending:
            groups, native_pending = select_changes(sorted(set(paths + pending)))
            native = native or native_pending
        print('Selection:', 'full JVM' if groups is None else ', '.join(groups) or 'no executable changes')
        if native:
            print('Native boundary changed: native/hardware verification remains a separate explicit operation.')
        if args.suite == 'plan':
            return 0
        if groups == []:
            print('Rechecking Gradle inputs; unchanged test tasks may remain UP-TO-DATE.')
            groups = None  # Let Gradle recheck dependency/configuration inputs even without source edits.
    if args.suite == 'full':
        groups = None
    elif args.suite == 'fast' and any(group not in MANIFEST['fast'] for group in groups):
        parser.error('fast supports only files, savings, control, errors')
    env = environment()
    results = []
    if args.suite != 'integration':
        fast_groups = [g for g in (groups or []) if g in MANIFEST['fast']]
        results.append(execute('fast', fast_groups, files, env))
    if args.suite != 'fast' and not any(r['exit_code'] for r in results):
        results.append(execute('integration', groups, files, env))
    for result in results:
        status = 'Gradle UP-TO-DATE/CACHED' if result.get('gradle_reused') else 'executed'
        passed = result['tests'] - result['failures'] - result['errors'] - result['skipped']
        print(f"{result['kind']}: {status}; passed={passed}, failed={result['failures'] + result['errors']}, skipped={result['skipped']}; actually_executed={result['executed_cases']}; wall={result['wall_seconds']:.3f}s; JUnit={result['test_seconds']:.3f}s")
        print('Reports:', str(Path(result['log']).parent.relative_to(ROOT)))
        for failure in result['failed_cases'][:5]:
            print('FAIL:', failure['test'], failure['message'])
        for error in result.get('build_errors', []):
            print(error)
    success = all(not r['exit_code'] and not r['failures'] and not r['errors'] for r in results)
    if success and args.suite == 'full':
        RESULTS.mkdir(parents=True, exist_ok=True)
        (RESULTS / 'last-full.json').write_text(json.dumps(files, sort_keys=True), encoding='utf-8')
    RESULTS.mkdir(parents=True, exist_ok=True)
    total = round(time.perf_counter() - started, 3)
    (RESULTS / 'latest.json').write_text(json.dumps({'suite': args.suite, 'groups': groups, 'wall_seconds': total,
        'native_verification_required': native, 'results': results}, indent=2), encoding='utf-8')
    print(f'Selection total: {total:.3f}s. No native build or device operation.')
    return 0 if success else 1


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (OSError, ValueError, subprocess.CalledProcessError, ET.ParseError) as error:
        print('Local tests could not complete:', str(error), file=sys.stderr)
        sys.exit(2)
