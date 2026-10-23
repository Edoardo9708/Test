import importlib.util
import contextlib
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('ci_scope', Path(__file__).resolve().parents[1] / 'scripts/ci-scope.py')
scope = importlib.util.module_from_spec(spec)
spec.loader.exec_module(scope)


class ScopeTest(unittest.TestCase):
    def test_ui_naming_audio_and_java_tests_skip_native(self):
        for path in ['app/src/main/res/values/styles.xml', scope.JAVA + 'AppUi.java',
                     scope.JAVA + 'OutputPlan.java', scope.JAVA + 'VideoConverter.java',
                     scope.JAVA + 'AudioCopy.java', 'app/src/test/java/NewTest.java',
                     'tests/OutputNameTest.java', 'scripts/emit-ui-previews.py']:
            with self.subTest(path=path):
                self.assertEqual((True, False), self.flags([path]))

    def flags(self, paths):
        result = scope.classify(paths)
        return result['build'], result['native']

    def test_native_build_jni_and_unknown_changes_run_full(self):
        for path in ['app/src/main/cpp/stream_bridge.cpp', 'vendor/x265-version.txt',
                     scope.JAVA + 'HdrTranscoder.java', scope.JAVA + 'EncoderPresetPreview.java',
                     'app/build.gradle', 'gradle.properties', 'app/src/main/AndroidManifest.xml',
                     'scripts/prepare-x265.sh', '.github/workflows/android.yml',
                     'scripts/ci-scope.py', 'tests/test_ci_scope.py', 'new-dependency.toml',
                     'tests/HdrNativeMetadataCheck.java', 'tests/HdrHeaderCheck.java']:
            with self.subTest(path=path):
                self.assertEqual((True, True), self.flags([path]))

    def test_changed_jni_lines_trigger_full_even_in_otherwise_fast_java(self):
        event={'before': 'a'*40, 'after': 'b'*40}
        for line in ['+private native void encode();', '+nativeOpen(width);', '-System.loadLibrary("x265");']:
            with patch.object(scope.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, stdout=line.encode())):
                self.assertTrue(scope.native_java_changes(event, [scope.JAVA+'VideoConverter.java']))
        with patch.object(scope.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, stdout=b'+while (sampleExists) {')):
            self.assertFalse(scope.native_java_changes(event, [scope.JAVA+'VideoConverter.java']))
        self.assertTrue(scope.classify([scope.JAVA+'VideoConverter.java'], native_boundary=True)['native'])

    def test_version_bump_is_quick_but_toolchain_changes_are_full(self):
        event={'before': 'a'*40, 'after': 'b'*40}
        version_diff="-        versionCode 41\n+        versionCode 42\n-        versionName '0.41'\n+        versionName '0.42'\n"
        with patch.object(scope.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, stdout=version_diff.encode())):
            self.assertTrue(scope.only_version_change(event))
        native_diff=version_diff+"-ndkVersion '27'\n+ndkVersion '28'"
        with patch.object(scope.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, stdout=native_diff.encode())):
            self.assertFalse(scope.only_version_change(event))
        with patch.object(scope.subprocess, 'run', side_effect=OSError('missing diff')):
            self.assertFalse(scope.only_version_change(event))
        self.assertFalse(scope.classify(['app/build.gradle',scope.JAVA+'AppUi.java'],version_only=True)['native'])
        self.assertTrue(scope.classify(['app/build.gradle',scope.JAVA+'AppUi.java'])['native'])

    def test_documentation_only_and_mixed_changes(self):
        self.assertEqual((False, False), self.flags(['README.md', 'docs/ui-architecture.md']))
        self.assertEqual((True, False), self.flags(['README.md', scope.JAVA + 'AppUi.java']))
        self.assertEqual((True, True), self.flags([scope.JAVA + 'AppUi.java', 'app/build.gradle']))
        self.assertEqual((True, True), self.flags([]))
        self.assertTrue(scope.classify(['README.md'], force_full=True)['native'])

    def test_initial_commit_invalid_diff_and_manual_run(self):
        self.assertEqual([], scope.changes({'before': '0'*40, 'after': 'a'*40}, 'push')[0])
        self.assertEqual([], scope.changes({}, 'push')[0])
        self.assertEqual([], scope.changes({}, 'workflow_dispatch')[0])
        with patch.object(scope.subprocess, 'run', side_effect=OSError('missing base')):
            self.assertEqual([], scope.changes({'before': 'a'*40, 'after': 'b'*40}, 'push')[0])

    def test_entry_point_writes_outputs_for_manual_quick_full_and_documentation(self):
        scenarios=[('workflow_dispatch', {'inputs': {'full_tests': False}}, [], (True, False)),
                   ('workflow_dispatch', {}, [], (True, True)),
                   ('push', {}, ['docs/readme.md'], (False, False)),
                   ('push', {}, [scope.JAVA+'AppUi.java'], (True, False))]
        for event_name, event, paths, expected in scenarios:
            with self.subTest(event_name=event_name, event=event, paths=paths), tempfile.TemporaryDirectory() as directory:
                payload=Path(directory, 'event.json');payload.write_text(json.dumps(event))
                outputs=Path(directory, 'outputs');summary=Path(directory, 'summary')
                env={'GITHUB_EVENT_PATH': str(payload), 'GITHUB_EVENT_NAME': event_name,
                     'GITHUB_OUTPUT': str(outputs), 'GITHUB_STEP_SUMMARY': str(summary)}
                with patch.dict(os.environ, env), patch.object(scope, 'changes', return_value=(paths,'test diff')), patch.object(scope, 'native_java_changes', return_value=False), contextlib.redirect_stdout(io.StringIO()):
                    scope.main()
                result=dict(line.split('=',1) for line in outputs.read_text().splitlines())
                self.assertEqual(str(expected[0]).lower(), result['build'])
                self.assertEqual(str(expected[1]).lower(), result['native'])
                self.assertIn('Test selection', summary.read_text())

    def test_cancelled_push_changes_are_compared_to_last_success_not_previous_push(self):
        with tempfile.TemporaryDirectory() as directory:
            payload=Path(directory,'event.json');payload.write_text(json.dumps({'before':'b'*40,'after':'c'*40}))
            outputs=Path(directory,'outputs')
            for baseline in ['a'*40,'']:
                with self.subTest(baseline=baseline):
                    env={'GITHUB_EVENT_PATH':str(payload),'GITHUB_EVENT_NAME':'push','GITHUB_OUTPUT':str(outputs),'CI_BASE_SHA':baseline}
                    def pending_changes(event,event_name):
                        self.assertEqual(baseline,event['before']);self.assertEqual('c'*40,event['after'])
                        return ([scope.JAVA+'AppUi.java','app/src/main/cpp/pending.cpp'] if baseline else []),'baseline'
                    with patch.dict(os.environ,env),patch.object(scope,'changes',side_effect=pending_changes),patch.object(scope,'native_java_changes',return_value=False),contextlib.redirect_stdout(io.StringIO()):
                        scope.main()
                    result=dict(line.split('=',1) for line in outputs.read_text().splitlines())
                    self.assertEqual('true',result['native'])

    def test_real_git_diff_covers_multiple_commits_and_both_sides_of_rename(self):
        with tempfile.TemporaryDirectory() as directory:
            def git(*args):
                return subprocess.check_output(['git', '-C', directory, *args], stderr=subprocess.DEVNULL).decode().strip()
            git('init');git('config', 'user.email', 'ci@example.test');git('config', 'user.name', 'CI test')
            file = Path(directory, 'native.cpp');file.write_text('initial');git('add', '.');git('commit', '-m', 'base')
            before=git('rev-parse', 'HEAD')
            file.write_text('changed native');git('add', '.');git('commit', '-m', 'native change')
            git('mv', 'native.cpp', 'README.md');git('commit', '-m', 'rename')
            after=git('rev-parse', 'HEAD')
            real_run=subprocess.run
            def in_repo(args, **kwargs):
                return real_run(args, cwd=directory, **kwargs)
            with patch.object(scope.subprocess, 'run', side_effect=in_repo):
                paths, _=scope.changes({'before': before, 'after': after}, 'push')
            self.assertEqual({'native.cpp', 'README.md'}, set(paths))
            self.assertTrue(scope.classify(paths)['native'])


if __name__ == '__main__':
    unittest.main()
