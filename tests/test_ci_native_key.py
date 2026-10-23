import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('native_key', Path(__file__).resolve().parents[1] / 'scripts/ci-native-key.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class NativeKeyTest(unittest.TestCase):
    def test_ui_and_version_reuse_native_but_sources_flags_dependencies_and_paths_invalidate(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            values = {'app/build.gradle': "android {\n versionCode 103\n versionName '1.03'\n ndkVersion '27'\n}",
                      'app/src/main/cpp/codec.cpp': 'native code', 'scripts/prepare-images.sh': 'pinned source',
                      'scripts/patch-x265-arm.py': 'patch', 'scripts/build-video-decoder.sh': 'flags',
                      'app/src/main/java/Screen.java': 'UI', '.github/workflows/android.yml': 'workflow',
                      'gradle.properties': 'configuration', 'build.gradle': 'plugin version'}
            for path, content in values.items():
                target = root / path; target.parent.mkdir(parents=True, exist_ok=True); target.write_text(content)
            environment = {'workspace': '/build', 'ANDROID_HOME': '/sdk'}
            def key():
                return module.fingerprint(root, values, environment)
            original = key()
            (root / 'app/src/main/java/Screen.java').write_text('new UI')
            (root / 'app/build.gradle').write_text(values['app/build.gradle'].replace('103', '104').replace('1.03', '1.04'))
            self.assertEqual(original, key())
            for path in values:
                if path == 'app/src/main/java/Screen.java':
                    continue
                with self.subTest(path=path):
                    old = (root / path).read_text();(root / path).write_text(old + '\nchanged build input')
                    self.assertNotEqual(original, key());(root / path).write_text(old)
            environment['ANDROID_HOME'] = '/new-sdk';self.assertNotEqual(original, key())
            environment['ANDROID_HOME'] = '/sdk';environment['workspace'] = '/other-build';self.assertNotEqual(original, key())

    def test_added_removed_and_reordered_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory);(root / 'app/src/main/cpp').mkdir(parents=True)
            paths = ['app/src/main/cpp/a.cpp', 'app/src/main/cpp/b.cpp']
            for path in paths:(root / path).write_text(path)
            original = module.fingerprint(root, paths, {})
            self.assertEqual(original, module.fingerprint(root, list(reversed(paths)), {}))
            self.assertNotEqual(original, module.fingerprint(root, paths[:1], {}))
            (root / paths[0]).unlink();self.assertNotEqual(original, module.fingerprint(root, paths, {}))


if __name__ == '__main__':
    unittest.main()
