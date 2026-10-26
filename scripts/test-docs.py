#!/usr/bin/env python3
"""Inventory and review bookkeeping only: no Gradle, network or semantic prose generation."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
DOCS = 'docs/testing'
STATE = DOCS + '/catalog.json'
PAGES = ('video.md', 'audio.md', 'foto.md', 'file-metadati.md', 'code.md', 'ui-condivisi.md')
FIELDS = ('Dati e preparazione', 'Sequenza delle operazioni', 'Metodi applicativi e percorso',
          'File, mock e simulazioni', 'Asserzioni e risultati attesi', 'Comportamento verificato', 'Limiti')
LEXICAL = re.compile(r'""".*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*.*?\*/', re.S)
TEST = re.compile(r'@(?:org\.junit\.)?Test\b(?:(?!@(?:org\.junit\.)?Test\b).)*?\b(?:void|fun)\s+(`[^`]+`|\w+)\s*\(', re.S)
CARD = re.compile(r'<!-- test: ([\w.$]+) -->\n(.*?)\n<!-- /test -->', re.S)
IGNORED = {'.git', '.gradle', '.tools', '.cxx', 'build', '__pycache__', 'vendor'}

def digest(value):
    if isinstance(value, str):
        value = value.encode('utf-8')
    return hashlib.sha256(value).hexdigest()

def file_digest(path):
    value = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(65536), b''):
            value.update(block)
    return value.hexdigest()

def encoded(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'))

def masked(source):
    return LEXICAL.sub(lambda m: ''.join('\n' if c == '\n' else ' ' for c in m.group()), source)

def anchor(identity):
    return 'test-' + digest(identity)[:16]

def files(root, directory, application_resources=False):
    folder = root / directory
    if folder.is_dir():
        for base, directories, names in os.walk(folder, followlinks=False):
            directories[:] = sorted(d for d in directories if d not in IGNORED and not (Path(base) / d).is_symlink())
            for name in sorted(names):
                path = Path(base) / name
                suffix = path.suffix.lower()
                fixture = 'resources' in path.parts
                generated = suffix in {'.class', '.pyc'} or (suffix == '.log' and not fixture)
                report = suffix in {'.xml', '.png', '.html'} and not (fixture or application_resources)
                if not path.is_symlink() and not generated and not report:
                    yield path

def declarations(source, path):
    clean = masked(source)
    matches = list(TEST.finditer(clean))
    annotations = list(re.finditer(r'@(?:\w+\.)*Test\b', clean))
    if len(matches) != len(annotations):
        raise ValueError(f'{path}: annotazione @Test non riconosciuta; revisionare il parser prima di aggiornare l’indice')
    if re.search(r'\bextends\s+TestCase\b|:\s*TestCase\s*\(', clean):
        raise ValueError(f'{path}: test JUnit 3 non supportati dal catalogo @Test')
    owners = []
    for declaration in re.finditer(r'(?<![.\w])class\s+(\w+)', clean):
        begin = clean.find('{', declaration.end())
        if begin < 0:
            continue
        pos, depth = begin + 1, 1
        while depth and pos < len(clean):
            depth += (clean[pos] == '{') - (clean[pos] == '}')
            pos += 1
        owners.append((declaration.group(1), begin, pos))
    result = []
    for i, match in enumerate(matches):
        enclosing = [owner for owner in owners if owner[1] < match.start() < owner[2]]
        owner = max(enclosing, key=lambda item: item[1])[0] if enclosing else None
        if owner != Path(path).stem:
            raise ValueError(f'{path}: proprietario @Test {owner!r} non supportato; non assumere il nome del file come classe')
        limit = matches[i + 1].start() if i + 1 < len(matches) else len(clean)
        pos, depth = match.end(), 1
        while depth and pos < limit:
            depth += (clean[pos] == '(') - (clean[pos] == ')')
            pos += 1
        if depth:
            raise ValueError(f'{path}: firma non riconosciuta')
        block = clean.find('{', pos, limit)
        expression = clean.find('=', pos, limit)
        if expression >= 0 and (block < 0 or expression < block):
            # Kotlin expression body, possibly multiline and with lambda arguments.
            pos, depths = expression + 1, [0, 0, 0]
            started = False
            while pos < limit:
                c = clean[pos]
                if not c.isspace():
                    started = True
                if c in '([{':
                    depths['([{'.index(c)] += 1
                if c in ')]}':
                    depths[')]}'.index(c)] -= 1
                if c == '\n' and started and depths == [0, 0, 0]:
                    break
                pos += 1
        elif block >= 0:
            pos, depth = block + 1, 1
            while depth and pos < limit:
                depth += (clean[pos] == '{') - (clean[pos] == '}')
                pos += 1
            if depth:
                raise ValueError(f'{path}: corpo @Test non riconosciuto')
        else:
            raise ValueError(f'{path}: test privo di corpo riconoscibile')
        name = match.group(1).strip('`')
        if not re.fullmatch(r'\w+', name):
            raise ValueError(f'{path}: nome Kotlin non supportato; aggiungere supporto esplicito, non ignorare il test')
        result.append({'name': name, 'line': source.count('\n', 0, match.start()) + 1,
                       'start': match.start(), 'end': pos, 'body': source[match.start():pos].strip()})
    return result

def scan(root):
    tests, contexts, texts, source_paths, resources = {}, {}, {}, {}, {}
    for path in files(root, 'app/src/test'):
        relative = path.relative_to(root).as_posix()
        if path.suffix not in {'.java', '.kt'}:
            resources[relative] = file_digest(path)
            continue
        source = path.read_text(encoding='utf-8')
        stem = path.stem
        if stem in texts:
            raise ValueError('Classi omonime non supportate: ' + stem)
        texts[stem], source_paths[stem] = source, relative
        methods = declarations(source, relative)
        context = source
        for method in reversed(methods):
            context = context[:method['start']] + context[method['end']:]
        contexts[stem] = digest(context)
        for method in methods:
            identity = stem + '.' + method['name']
            tests[identity] = {'source': relative, 'line': method['line'], 'body': method['body'], 'class': stem}
    # Lexical helper links are conservative review hints, never semantic descriptions.
    references = {name: set(re.findall(r'\b[A-Za-z_]\w*\b', masked(text))) & (set(texts) - {name}) for name, text in texts.items()}
    for identity, test in tests.items():
        dependencies, pending = set(), list(references[test['class']])
        while pending:
            name = pending.pop()
            if name not in dependencies:
                dependencies.add(name)
                pending.extend(references[name] - dependencies)
        test['fingerprint'] = digest(encoded({'body': digest(test['body']), 'context': contexts[test['class']],
            'helpers': {source_paths[n]: digest(texts[n]) for n in sorted(dependencies)}, 'resources': resources}))
    return tests

def application_inputs(root):
    paths = set(files(root, 'app/src/main', application_resources=True))
    paths.update(files(root, 'buildSrc'))
    paths.update(files(root, 'build-logic'))
    candidates = [p for p in root.iterdir() if p.is_file() and not p.is_symlink()]
    for directory in ('app', 'gradle', 'buildSrc', 'build-logic'):
        candidates.extend(files(root, directory))
    for path in candidates:
        relative = path.relative_to(root).as_posix()
        if path.name in {'gradle.properties', 'gradle.lockfile'} or path.suffix in {'.gradle', '.kts', '.toml', '.lockfile'} or relative.startswith('gradle/wrapper/'):
            paths.add(path)
    return {p.relative_to(root).as_posix(): file_digest(p) for p in sorted(paths)}

def cards(root):
    result, errors = {}, []
    for page in PAGES:
        path = root / DOCS / page
        if not path.is_file():
            errors.append('Pagina mancante: ' + page)
            continue
        text = path.read_text(encoding='utf-8')
        matches = list(CARD.finditer(text))
        if len(matches) != text.count('<!-- test:') or len(matches) != text.count('<!-- /test -->'):
            errors.append('Marcatori scheda non bilanciati: ' + page)
        for match in matches:
            identity, content = match.groups()
            if identity in result:
                errors.append('Scheda duplicata: ' + identity)
            for field in FIELDS:
                if not re.search(r'\*\*' + re.escape(field) + r':\*\*[ \t]+\S[^\n]*', content):
                    errors.append(identity + ': campo mancante ' + field)
            if f'<a id="{anchor(identity)}"></a>' not in content:
                errors.append(identity + ': ancora stabile mancante')
            snippets = re.findall(r'```(?:java|kotlin)\n(.*?)\n```', content, re.S)
            category = re.search(r'\*\*Categoria:\*\* ([ABCDE])(?:[ .]|$)', content)
            if not category:
                errors.append(identity + ': categoria A–E mancante')
            if len(snippets) != 1:
                errors.append(identity + ': deve esserci un solo corpo sorgente Java/Kotlin')
            result[identity] = {'page': page, 'anchor': anchor(identity), 'content': content,
                                'category': category.group(1) if category else '?',
                                'body': snippets[0].strip() if len(snippets) == 1 else ''}
    return result, errors

def link_errors(root):
    errors = []
    cache = {}
    # Generated views are checked by exact comparison with rendered(), including links.
    # Do not report stale links from yesterday's index as errors in today's update.
    paths = [p for p in (root / DOCS).glob('*.md') if p.name not in {'index.md', 'review-needed.md'}]
    for path in paths:
        text = re.sub(r'```.*?```', '', path.read_text(encoding='utf-8'), flags=re.S)
        for link in re.findall(r'\]\(([^)]+)\)', text):
            if re.match(r'\w+://', link):
                continue
            target, _, fragment = link.partition('#')
            resolved = (path.parent / target).resolve() if target else path.resolve()
            if not resolved.is_relative_to(root.resolve()) or not resolved.is_file():
                errors.append(f'{path.name}: collegamento non valido {link}')
                continue
            if fragment and resolved not in cache:
                cache[resolved] = resolved.read_text(encoding='utf-8')
            if fragment.startswith(('test-', 'class-')) and f'id="{fragment}"' not in cache[resolved]:
                errors.append(f'{path.name}: ancora assente {link}')
            if re.fullmatch(r'L\d+', fragment) and int(fragment[1:]) > len(cache[resolved].splitlines()):
                errors.append(f'{path.name}: riga assente {link}')
    return errors

def read_state(root):
    data = json.loads((root / STATE).read_text(encoding='utf-8'))
    if data.get('schema') != 1:
        raise ValueError('Schema catalogo non supportato')
    return data

def assess(root):
    state, current = read_state(root), scan(root)
    documentation, errors = cards(root)
    inputs = application_inputs(root)
    app_changes = sorted(p for p in set(inputs) | set(state['application_inputs']) if inputs.get(p) != state['application_inputs'].get(p))
    statuses = {}
    for identity in sorted(set(current) | set(state['tests']) | set(documentation)):
        reasons = []
        test, old, card = current.get(identity), state['tests'].get(identity), documentation.get(identity)
        if not test:
            reasons.append('rimosso: eliminare/archiviare la scheda e registrare la revisione')
        else:
            if not old:
                reasons.append('aggiunto: documentare e revisionare')
            elif test['fingerprint'] != old['fingerprint']:
                reasons.append('modificato: metodo, setup, helper o fixture da revisionare')
            if not card:
                reasons.append('scheda assente')
            elif card['body'] != test['body']:
                reasons.append('trascrizione del corpo sorgente da aggiornare')
            elif old and digest(card['content']) != old.get('card_digest'):
                reasons.append('scheda modificata: registrare la revisione semantica')
            if app_changes:
                reasons.append('impatto applicativo da valutare da Codex')
        statuses[identity] = reasons
    errors.extend(link_errors(root))
    return state, current, documentation, inputs, app_changes, statuses, errors

def rendered(current, documentation, changes, statuses, errors):
    categories = ', '.join(f'{category}: {sum(c.get("category") == category for i, c in documentation.items() if i in current)}' for category in 'ABCDE')
    index = ['# Indice dei test', '', '[Guida e aree](README.md) · [Revisioni necessarie](review-needed.md)', '',
             '<!-- Generated by scripts/test-docs.py update. Descriptions remain hand-reviewed. -->', '',
             f'Metodi attualmente rilevati: **{len(current)}**. Categorie documentate: {categories}. Le categorie documentali non sono misure di esecuzione.', '',
             '| Test | Scheda | Sorgente attuale | Stato documentale |', '|---|---|---|---|']
    pending = ['# Revisioni documentali necessarie', '', '[Guida](README.md) · [Indice](index.md)', '',
               'Questo elenco è statico: non contiene risultati di test e non esegue alcuna suite.', '']
    for identity in sorted(current):
        test, card = current[identity], documentation.get(identity)
        link = f'[{identity}]({card["page"]}#{card["anchor"]})' if card else 'scheda mancante'
        status = '; '.join(statuses[identity]) or 'allineato alla revisione registrata'
        index.append(f'| `{identity}` | {link} | [sorgente](../../{test["source"]}) | {status} |')
    removed = [i for i in statuses if i not in current]
    if removed:
        index += ['', '## Metodi rimossi da riconciliare', ''] + ['- `' + i + '`' for i in removed]
    if changes:
        pending += ['## Modifiche applicative/configurazione', '',
            'Il controllo non deduce quali comportamenti siano cambiati. Codex deve individuare e revisionare le schede interessate; fino alla valutazione tutte hanno un avviso conservativo. Non vengono riscritte le pagine.', '']
        pending += ['- `' + p + '`' for p in changes]
    needs = {i: reasons for i, reasons in statuses.items() if reasons}
    pending += ['', '## Schede da valutare', '']
    pending += ['- `' + i + '`: ' + '; '.join(reasons) for i, reasons in needs.items()] or ['Nessuna revisione pendente.']
    if errors:
        pending += ['', '## Errori strutturali', ''] + ['- ' + e for e in errors]
    return '\n'.join(index) + '\n', '\n'.join(pending) + '\n'

def write_changed(path, text):
    if path.is_file() and path.read_text(encoding='utf-8') == text:
        return False
    path.write_text(text, encoding='utf-8', newline='\n')
    return True

def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('check', 'update', 'review'))
    parser.add_argument('--test', action='append', default=[], help='ClassName.methodName; repeatable; review attests human semantic inspection')
    parser.add_argument('--application', action='store_true', help='Record a human impact review of changed production/build inputs')
    parser.add_argument('--unaffected', action='store_true', help='Explicit conclusion that changed application inputs affect no documented contract')
    parser.add_argument('--reason', help='Required rationale for application impact review; do not put paths to private media or secrets here')
    args = parser.parse_args(argv)
    if args.command != 'review' and (args.test or args.application or args.unaffected or args.reason):
        parser.error('Le opzioni di revisione richiedono il comando review')
    try:
        state, current, documentation, inputs, changes, statuses, errors = assess(ROOT)
        if args.command == 'review':
            if not args.test and not args.application:
                parser.error('Specificare almeno --test oppure --application')
            if args.unaffected and (not args.application or args.test):
                parser.error('--unaffected richiede --application e nessun --test')
            if args.application and (not changes or not args.reason or not (args.test or args.unaffected)):
                parser.error('--application richiede cambi applicativi, --reason e --test oppure --unaffected')
            if changes and not args.application:
                parser.error('Valutare anche i cambi applicativi: usare --application e una motivazione esplicita')
            selected = set(args.test)
            if errors:
                raise ValueError('Correggere gli errori strutturali prima della revisione: ' + '; '.join(errors[:5]))
            for identity in selected:
                if identity not in statuses:
                    raise ValueError('Test sconosciuto: ' + identity)
                if identity in current:
                    if identity not in documentation or documentation[identity]['body'] != current[identity]['body']:
                        raise ValueError(identity + ': scheda/corpo sorgente non aggiornato')
                elif identity in documentation:
                    raise ValueError(identity + ': rimuovere prima la scheda obsoleta')
            for identity in selected:
                if identity in current:
                    state['tests'][identity] = {'fingerprint': current[identity]['fingerprint'], 'card_digest': digest(documentation[identity]['content'])}
                else:
                    state['tests'].pop(identity, None)
            if args.application:
                state['application_inputs'] = inputs
                state['last_application_review'] = {'reason': args.reason, 'reviewed_tests': sorted(selected), 'unaffected': args.unaffected,
                                                    'changed_paths': changes}
            write_changed(ROOT / STATE, json.dumps(state, ensure_ascii=False, indent=2, sort_keys=True) + '\n')
            state, current, documentation, inputs, changes, statuses, errors = assess(ROOT)
        index, pending = rendered(current, documentation, changes, statuses, errors)
        expected = {'index.md': index, 'review-needed.md': pending}
        stale = [name for name, content in expected.items() if not (ROOT / DOCS / name).is_file() or (ROOT / DOCS / name).read_text(encoding='utf-8') != content]
        if args.command in {'update', 'review'}:
            written = [name for name, content in expected.items() if write_changed(ROOT / DOCS / name, content)]
            print('Pagine indice aggiornate: ' + (', '.join(written) or 'nessuna'))
        elif stale:
            errors.append('Indice/segnalazioni non aggiornati: eseguire python scripts/test-docs.py update')
        dirty = {i: reasons for i, reasons in statuses.items() if reasons}
        counts = {'metodi': len(current), 'schede': len(documentation), 'revisioni_pendenti': len(dirty), 'input_applicativi_cambiati': len(changes), 'errori': len(errors)}
        print(json.dumps(counts, ensure_ascii=False))
        for error in errors[:10]:
            print('ERRORE: ' + error)
        if dirty:
            print('Consultare docs/testing/review-needed.md; nessuna descrizione semantica è stata generata o approvata automaticamente.')
        return 1 if dirty or errors else 0
    except (ValueError, OSError, json.JSONDecodeError) as error:
        print('ERRORE: ' + str(error), file=sys.stderr)
        return 2

if __name__ == '__main__':
    sys.exit(main())
