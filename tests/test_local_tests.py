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

spec = importlib.util.spec_from_file_location('local_tests', Path(__file__).resolve().parents[1] / 'scripts/test-local.py')
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


class SelectionTest(unittest.TestCase):
    def test_documentation_needs_no_execution(self):
        self.assertEqual(([], False), runner.select_changes(['AGENTS.md', 'docs/test-strategy.md']))

    def test_output_changes_select_only_file_contracts(self):
        for name in ('OutputName.java', 'OutputPlan.java', 'ImageOutput.java', 'ImageOutputFailureTest.java'):
            self.assertEqual((None, False), runner.select_changes([runner.JAVA + name]))

    def test_shared_control_and_cross_section_fixtures_expand_to_full(self):
        for name in ('ConversionControl.java', 'MediaDestinationsController.java', 'AudioMetadataFlowTest.java'):
            self.assertEqual((None, False), runner.select_changes([runner.JAVA + name]))

    def test_multiple_domains_are_combined_without_duplicates(self):
        self.assertEqual((None, False), runner.select_changes([runner.JAVA + 'AudioCopy.java', runner.JAVA + 'PhotoWorkspace.kt', runner.JAVA + 'ImageActions.java']))

    def test_unknown_code_is_never_treated_as_documentation(self):
        self.assertEqual((None, False), runner.select_changes(['app/src/main/java/NewShared.java']))

    def test_native_changes_require_separate_native_verification(self):
        self.assertEqual((None, True), runner.select_changes(['app/src/main/cpp/images/image_verify.cpp']))
        self.assertEqual((None, True), runner.select_changes(['scripts/build-video-decoder.sh']))

    def test_native_requirement_survives_unknown_paths_in_either_order(self):
        paths = ['app/build.gradle', 'app/src/main/cpp/images/image_verify.cpp']
        self.assertEqual((None, True), runner.select_changes(paths))
        self.assertEqual((None, True), runner.select_changes(list(reversed(paths))))

    def test_paths_outside_repository_are_rejected(self):
        with self.assertRaises(ValueError):
            runner.normalized_paths(['../outside.java'])

    def test_explicit_files_cannot_hide_another_uncommitted_shared_change(self):
        output=io.StringIO()
        with patch.object(runner,'snapshot',return_value={}), patch.object(runner,'normalized_paths',return_value=[runner.TEST+'OutputNamingSafetyTest.java']), patch.object(runner,'changed_files',return_value=[runner.JAVA+'AudioCopy.java']), contextlib.redirect_stdout(output):
            self.assertEqual(0,runner.main(['plan','--files','ignored-by-fixture']))
        self.assertIn('Selection: full JVM',output.getvalue())

    def test_no_source_diff_still_consults_gradle_for_external_input_changes(self):
        with patch.object(runner,'snapshot',return_value={}), patch.object(runner,'changed_files',return_value=[]), patch.object(runner,'environment',return_value={}), patch.object(runner,'execute',side_effect=RuntimeError('Gradle consulted')), contextlib.redirect_stdout(io.StringIO()):
            with self.assertRaisesRegex(RuntimeError,'Gradle consulted'):
                runner.main(['changed'])


class ReportAndCacheTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.root_patch = patch.object(runner, 'ROOT', self.root)
        self.results_patch = patch.object(runner, 'RESULTS', self.root / '.tools/test-results')
        self.root_patch.start(); self.results_patch.start()
        self.addCleanup(self.root_patch.stop); self.addCleanup(self.results_patch.stop)

    def write_report(self, xml):
        folder = self.root / 'app/build/test-results/localFastTest'
        folder.mkdir(parents=True, exist_ok=True)
        path = folder / 'TEST-it.edo.codecprobe.ExampleTest.xml'
        path.write_text(xml)
        return path

    def test_counts_skips_and_failure_without_printing_stack_trace(self):
        path = self.write_report('<testsuite name="it.edo.codecprobe.ExampleTest" tests="3" failures="1" errors="0" skipped="1" time="0.12"><testcase name="fails"><failure message="missing output">full detailed stack</failure></testcase></testsuite>')
        report = runner.reports(path.parent)
        self.assertEqual((3, 1, 1), (report['tests'], report['failures'], report['skipped']))
        self.assertEqual('missing output', report['failed_cases'][0]['message'])
        self.assertEqual('full detailed stack', report['failed_cases'][0]['details'])

    def test_compilation_failure_cannot_reuse_stale_successful_xml(self):
        path = self.write_report('<testsuite name="it.edo.codecprobe.ExampleTest" tests="99" failures="0"/>')
        os.utime(path, (1, 1))
        with patch.object(runner.subprocess, 'run', return_value=subprocess.CompletedProcess([], 1)):
            result = runner.execute('fast', [], {}, {})
        self.assertTrue(result['build_failed']); self.assertEqual(0, result['tests']); self.assertEqual(1, result['exit_code'])

    def test_each_invocation_asks_gradle_to_validate_real_inputs(self):
        def gradle(command, **kwargs):
            self.assertIn('--offline', command)
            self.assertIn('--max-workers=2', command)
            self.assertNotIn('clean', command)
            self.write_report('<testsuite name="it.edo.codecprobe.ExampleTest" tests="1" failures="0" errors="0" skipped="0"/>')
            status = next(arg.split('=', 1)[1] for arg in command if arg.startswith('-Plocal.test.status='))
            Path(status).write_text(json.dumps({'task': ':app:localFastTest', 'upToDate': False, 'skipped': False, 'noSource': False, 'failed': False, 'executedCases': 1}))
            return subprocess.CompletedProcess(command, 0)
        with patch.object(runner.subprocess, 'run', side_effect=gradle) as run:
            first = runner.execute('fast', [], {}, {})
            second = runner.execute('fast', [], {}, {})
        self.assertEqual(2, run.call_count); self.assertEqual(1, second['executed_cases'])

    def test_success_with_no_reports_is_an_incomplete_verification(self):
        with patch.object(runner.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0)):
            result = runner.execute('fast', [], {}, {})
        self.assertNotEqual(0, result['exit_code'])

    def test_snapshot_tracks_uncommitted_sources_tests_catalogs_and_configuration(self):
        names = ['app/src/main/java/A.java', 'app/src/test/java/ATest.java', 'gradle/libs.versions.toml', 'buildSrc/src/main/kotlin/Plugin.kt', 'new-config.properties']
        for name in names:
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True); path.write_text('one')
        listing = ('\0'.join(names) + '\0').encode()
        with patch.object(runner.subprocess, 'check_output', return_value=listing):
            previous = runner.snapshot()
            for name in names:
                (self.root / name).write_text('uncommitted change')
                current = runner.snapshot()
                self.assertNotEqual(previous[name], current[name]); previous = current
            (self.root / names[0]).unlink()
            self.assertNotIn(names[0], runner.snapshot())

    def test_skipped_or_missing_task_cannot_validate_stale_reports(self):
        self.write_report('<testsuite name="it.edo.codecprobe.ExampleTest" tests="1" failures="0"><testcase name="old"/></testsuite>')
        for status in (None, {'task': ':app:localFastTest', 'skipped': True, 'upToDate': False, 'skipMessage': 'SKIPPED'}):
            def gradle(command, **kwargs):
                if status is not None:
                    path = next(arg.split('=', 1)[1] for arg in command if arg.startswith('-Plocal.test.status='))
                    Path(path).write_text(json.dumps(status))
                return subprocess.CompletedProcess(command, 0)
            with patch.object(runner.subprocess, 'run', side_effect=gradle):
                self.assertNotEqual(0, runner.execute('fast', [], {}, {})['exit_code'])

    def test_jpeg_and_audio_helpers_used_across_sections_expand_to_full(self):
        for name in ['JpegInventory.java', 'AudioCopy.java', 'CompressionSummary.java', 'ImageFingerprint.java']:
            self.assertEqual((None, False), runner.select_changes([runner.JAVA + name]))

    def test_ignored_junit_case_is_reported_as_skipped_not_executed(self):
        def gradle(command, **kwargs):
            self.write_report('<testsuite name="it.edo.codecprobe.ExampleTest" tests="1" failures="0" errors="0" skipped="1"><testcase name="ignored"><skipped/></testcase></testsuite>')
            status=next(arg.split('=',1)[1] for arg in command if arg.startswith('-Plocal.test.status='))
            Path(status).write_text(json.dumps({'task':':app:localFastTest','skipped':False,'noSource':False,'failed':False,'executedCases':1}))
            return subprocess.CompletedProcess(command,0)
        with patch.object(runner.subprocess,'run',side_effect=gradle):
            result=runner.execute('fast',[],{}, {})
        self.assertEqual(0,result['exit_code']);self.assertEqual(1,result['skipped']);self.assertEqual(0,result['executed_cases'])



if __name__ == '__main__':
    unittest.main()
