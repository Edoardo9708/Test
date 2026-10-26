"""Isolated probes of documentation bookkeeping; never executes app tests or Gradle."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import time

project = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('td', project / 'scripts/test-docs.py')
td = importlib.util.module_from_spec(spec)
spec.loader.exec_module(td)
checks = []

def passed(name):
    checks.append(name)

def write(root, path, text):
    target = root / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding='utf-8')
    return target

def source(root, name='SampleTest'):
    return root / ('app/src/test/java/' + name + '.java')

def call(root, *args):
    td.ROOT = root
    output = io.StringIO()
    with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
        try:
            result = td.main(list(args))
        except SystemExit as error:
            result = error.code
    return result, output.getvalue()

def card(identity, body):
    content = [f'<!-- test: {identity} -->', f'<a id="{td.anchor(identity)}"></a>', f'### {identity}', '', '**Categoria:** A.']
    content += ['**' + field + ':** Isolated verifier fixture; no app test execution.' for field in td.FIELDS]
    content += ['```java', body, '```', '<!-- /test -->']
    return '\n'.join(content) + '\n'

def reset(root):
    # Each probe uses a fresh child directory; no deletion of project files.
    root.mkdir(parents=True, exist_ok=True)
    write(root, 'app/src/test/java/SampleTest.java', '''class SampleTest {
  @Before public void setup() { int setupValue = 1; }
  @Test public void one() { Shared.value(); assertEquals(1, 1); }
  @Test public void two() { assertTrue(true); }
}
''')
    write(root, 'app/src/test/java/OtherTest.java', 'class OtherTest { @Test public void independent() { assertTrue(true); } }')
    write(root, 'app/src/test/java/Shared.java', 'class Shared { static int value() { return 1; } }')
    write(root, 'app/src/test/resources/input.txt', 'fixture one')
    write(root, 'app/src/main/java/Logic.java', 'class Logic { int value() { return 1; } }')
    current = td.scan(root)
    for page in td.PAGES:
        write(root, td.DOCS + '/' + page, '# Isolated fixture\n')
    write(root, td.DOCS + '/README.md', '# Isolated verifier fixture\n')
    write(root, td.DOCS + '/video.md', '\n'.join(card(i, test['body']) for i, test in current.items()))
    documentation, errors = td.cards(root)
    assert not errors
    state = {'schema': 1, 'tests': {i: {'fingerprint': t['fingerprint'], 'card_digest': td.digest(documentation[i]['content'])} for i, t in current.items()},
             'application_inputs': td.application_inputs(root)}
    write(root, td.STATE, json.dumps(state, ensure_ascii=False, indent=2, sort_keys=True) + '\n')
    assert call(root, 'update')[0] == 0
    return root

def status(root):
    return td.assess(root)[5]

def modify(root, old, new, name='SampleTest'):
    path = source(root, name)
    text = path.read_text(encoding='utf-8')
    assert old in text
    path.write_text(text.replace(old, new), encoding='utf-8')

def refresh_card(root, identity):
    path = root / td.DOCS / 'video.md'
    text = path.read_text(encoding='utf-8')
    replacement = card(identity, td.scan(root)[identity]['body']).rstrip()
    text = td.CARD.sub(lambda match: replacement if match.group(1) == identity else match.group(0), text)
    path.write_text(text, encoding='utf-8')

start = time.perf_counter()
(project / ".tools").mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(prefix='testing-docs-probes-', dir=project / '.tools') as directory:
    temporary = Path(directory).resolve()
    assert temporary.is_relative_to((project / '.tools').resolve())
    r = reset(temporary / 'baseline')
    assert call(r, 'check')[0] == 0
    passed('baseline inventory/cards coherent')
    paths = list((r / td.DOCS).glob('*'))
    before = {p: (p.read_bytes(), p.stat().st_mtime_ns) for p in paths}
    assert call(r, 'check')[0] == 0
    assert before == {p: (p.read_bytes(), p.stat().st_mtime_ns) for p in paths}
    passed('check is read-only')
    assert call(r, 'update')[0] == 0
    assert before == {p: (p.read_bytes(), p.stat().st_mtime_ns) for p in paths}
    passed('unchanged update preserves bytes and timestamps')

    r = reset(temporary / 'one-method')
    modify(r, 'assertEquals(1, 1)', 'assertEquals(2, 2)')
    dirty = {i for i, reasons in status(r).items() if reasons}
    assert dirty == {'SampleTest.one'}, dirty
    passed('uncommitted method change selects exactly involved method')
    preserved = {p: p.read_bytes() for p in (r / td.DOCS).glob('*') if p.name not in {'index.md', 'review-needed.md'}}
    assert call(r, 'update')[0] == 1
    assert preserved == {p: p.read_bytes() for p in preserved}
    passed('pending update writes views only, not prose/catalog')
    assert call(r, 'review', '--test', 'SampleTest.one')[0] == 2
    passed('review refuses stale body transcription')
    refresh_card(r, 'SampleTest.one')
    assert call(r, 'review', '--test', 'SampleTest.one')[0] == 0
    assert call(r, 'check')[0] == 0
    passed('explicit selected review clears only matching source/card revision')

    r = reset(temporary / 'add-remove')
    write(r, 'app/src/test/java/NewTest.java', 'class NewTest { @Test public void newCase() { assertTrue(true); } }')
    assert len(td.scan(r)) == 4 and 'aggiunto' in ';'.join(status(r)['NewTest.newCase'])
    assert call(r, 'update')[0] == 1 and 'NewTest.newCase' in (r / td.DOCS / 'index.md').read_text()
    passed('new file/method appears before a card exists')
    modify(r, '@Test public void one() { Shared.value(); assertEquals(1, 1); }', '')
    assert 'rimosso' in ';'.join(status(r)['SampleTest.one'])
    assert call(r, 'review', '--test', 'SampleTest.one')[0] == 2
    path = r / td.DOCS / 'video.md'
    path.write_text(td.CARD.sub(lambda m: '' if m.group(1) == 'SampleTest.one' else m.group(0), path.read_text()), encoding='utf-8')
    assert call(r, 'review', '--test', 'SampleTest.one')[0] == 1  # unrelated new test still pending
    assert 'SampleTest.one' not in td.read_state(r)['tests']
    passed('removal detected; requires removing card; unrelated addition remains pending')

    r = reset(temporary / 'setup')
    modify(r, 'setupValue = 1', 'setupValue = 2')
    assert {i for i, reasons in status(r).items() if reasons} == {'SampleTest.one', 'SampleTest.two'}
    passed('setup change invalidates whole class, not unrelated class')
    r = reset(temporary / 'helper')
    path = source(r, 'Shared'); path.write_text(path.read_text().replace('return 1', 'return 2'))
    assert {i for i, reasons in status(r).items() if reasons} == {'SampleTest.one', 'SampleTest.two'}
    passed('shared helper invalidates dependent class')
    r = reset(temporary / 'resource')
    (r / 'app/src/test/resources/input.txt').write_text('fixture changed')
    assert all(status(r).values())
    (r / 'app/src/test/resources/input.txt').unlink()
    assert all(status(r).values())
    passed('resource modification and removal conservatively detected')

    r = reset(temporary / 'application')
    (r / 'app/src/main/java/Logic.java').write_text('class Logic { int value() { return 2; } }')
    assert all('impatto applicativo' in ';'.join(reasons) for reasons in status(r).values())
    assert call(r, 'review', '--application', '--unaffected')[0] == 2
    assert call(r, 'review', '--application', '--unaffected', '--reason', 'Isolated fixture: no referenced app contract')[0] == 0
    passed('application change requires explicit impact review and reason')
    r = reset(temporary / 'configuration')
    write(r, 'app/build.gradle', 'changed configuration')
    assert td.assess(r)[4] == ['app/build.gradle']
    passed('new build configuration input detected')
    r = reset(temporary / 'android-resources')
    write(r, 'app/src/main/AndroidManifest.xml', '<manifest/>')
    write(r, 'app/src/main/res/values/strings.xml', '<resources/>')
    assert set(td.assess(r)[4]) == {'app/src/main/AndroidManifest.xml', 'app/src/main/res/values/strings.xml'}
    passed('Android manifest and XML resources are application inputs, not generated reports')
    r = reset(temporary / 'transitive-helper')
    write(r, 'app/src/test/java/Shared.java', 'class Shared { static int value() { return Deeper.value(); } }')
    write(r, 'app/src/test/java/Deeper.java', 'class Deeper { static int value() { return 1; } }')
    state=td.read_state(r)
    for identity,test in td.scan(r).items():
        state['tests'][identity]['fingerprint']=test['fingerprint']
    write(r,td.STATE,json.dumps(state))
    (r/'app/src/test/java/Deeper.java').write_text('class Deeper { static int value() { return 2; } }')
    assert {i for i,reasons in status(r).items() if reasons} == {'SampleTest.one','SampleTest.two'}
    passed('helper invalidation follows transitive references')
    r = reset(temporary / 'app-plus-test')
    modify(r, 'assertEquals(1, 1)', 'assertEquals(2, 2)')
    (r / 'app/src/main/java/Logic.java').write_text('changed application')
    assert call(r, 'review', '--application', '--unaffected', '--reason', 'Isolated app change unrelated')[0] == 1
    assert status(r)['SampleTest.one']
    passed('application impact acknowledgement cannot approve a changed test')

    r = reset(temporary / 'ignored')
    scan_before = {i: t['fingerprint'] for i, t in td.scan(r).items()}
    inputs_before = td.application_inputs(r)
    for directory in ('.tools', '.gradle', '.git', 'vendor', 'build', '.cxx', '__pycache__', 'app/build', 'app/src/test/build', 'app/src/test/.tools', 'app/src/main/build'):
        write(r, directory + '/FakeTest.java', '@Test invalid syntax')
        write(r, directory + '/bad.gradle', 'generated configuration')
        write(r, directory + '/result.log', 'generated log')
    write(r, 'app/src/test/java/result.log', '@Test ignored log')
    assert scan_before == {i: t['fingerprint'] for i, t in td.scan(r).items()}
    assert inputs_before == td.application_inputs(r)
    passed('cache/build/vendor/log/result directories do not affect inventory or app inputs')
    write(r, 'app/src/test/resources/input.xml', '<intentional-fixture/>')
    assert all(status(r).values())
    passed('intentional XML fixture is monitored, unlike generated XML reports')

    r = reset(temporary / 'lexical')
    write(r, 'app/src/test/java/Comment.java', 'class Comment { String s="@Test public void fake() {}"; /* @Test invalid */ }')
    assert len(td.scan(r)) == 3
    passed('annotations in strings/comments do not create fake tests')
    write(r, 'app/src/test/java/KotlinTest.kt', '''class KotlinTest {
      @Test fun expression() = capture(
        "text with } and @Test", { 1 })
      @org.junit.Test fun block() { assertEquals("{", "{") }
    }''')
    assert len(td.scan(r)) == 5
    passed('Kotlin multiline expression/lambda and qualified annotation recognized')
    write(r, 'app/src/test/java/Unsupported.kt', 'class Unsupported { @Test fun `words with spaces`() {} }')
    try:
        td.scan(r)
        raise AssertionError('unsupported form silently skipped')
    except ValueError:
        pass
    passed('unsupported annotation/method shape fails closed')
    for invalid in ('class Renamed { @Test public void one() {} }', 'class OwnerTest { class Nested { @Test public void one() {} } }', 'class OwnerTest extends TestCase { public void testLegacy() {} }'):
        try:
            td.declarations(invalid,'OwnerTest.java')
            raise AssertionError('unsupported owner/JUnit3 silently accepted')
        except ValueError:
            pass
    passed('renamed/nested owner and JUnit3 cannot silently corrupt inventory identities')

    r = reset(temporary / 'documentation')
    path = r / td.DOCS / 'video.md'
    path.write_text(path.read_text().replace('**Limiti:** Isolated', '**Limiti:** Revised isolated', 1))
    assert any('scheda modificata' in reason for reasons in status(r).values() for reason in reasons)
    passed('documentation edit requires recorded review')
    write(r, td.DOCS + '/README.md', '[broken](missing.md)\n[anchor](video.md#test-does-not-exist)')
    assert len(td.link_errors(r)) == 2
    passed('missing relative file and missing stable anchor rejected')
    write(r, td.DOCS + '/README.md', '# Fixture')
    path.write_text(path.read_text().replace('**Dati e preparazione:** Isolated verifier fixture; no app test execution.', '**Dati e preparazione:**', 1))
    assert td.cards(r)[1]
    passed('empty required field rejected')

td.ROOT = project
duration = time.perf_counter() - start
summary = {'probes_passed': len(checks), 'failed': 0, 'wall_seconds': round(duration, 3), 'checks': checks,
           'scope': 'isolated documentation-tool probes, not the 332 app tests'}
(project / '.tools/testing-docs-verification.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
print(json.dumps({k: v for k, v in summary.items() if k != 'checks'}))
