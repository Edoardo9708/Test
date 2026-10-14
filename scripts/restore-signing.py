#!/usr/bin/env python3
"""Restore a private PKCS12 from Actions secrets, never from repository files."""
import base64
import json
import os
from pathlib import Path
import subprocess

def main():
    secret = os.environ.get('CODECPROBE_SIGNING_JSON', '').strip()
    if not secret:
        print('::warning::CODECPROBE_SIGNING_JSON missing: tests will run, but no installable APK will be published with a temporary signature.')
        with open(os.environ['GITHUB_OUTPUT'], 'a') as out:
            out.write('ready=false\n')
        return
    try:
        value = json.loads(secret)
        password = value['password']
        if not isinstance(password, str) or not password.isalnum() or len(password)<16:
            raise ValueError()
        key = base64.b64decode(value['keystore_base64'], validate=True)
        if not 1000 <= len(key) <= 20000:
            raise ValueError()
    except (ValueError, TypeError, KeyError):
        raise SystemExit('Signing secret has invalid format; no APK will be published.') from None
    print('::add-mask::' + password)
    folder = Path(os.environ['RUNNER_TEMP']) / 'codecprobe-signing'
    folder.mkdir(mode=0o700, exist_ok=True)
    file = folder / 'permanent.p12'
    file.write_bytes(key)
    file.chmod(0o600)
    env = dict(os.environ, CODECPROBE_KEY_PASSWORD=password)
    result = subprocess.run(['keytool','-list','-keystore',str(file),'-storetype','PKCS12','-storepass:env','CODECPROBE_KEY_PASSWORD','-alias','codecprobe'], env=env, capture_output=True)
    if result.returncode:
        file.unlink(missing_ok=True)
        raise SystemExit('Permanent signing key cannot be opened; no APK will be published.')
    with open(os.environ['GITHUB_ENV'],'a') as out:
        out.write('CODECPROBE_STORE_FILE='+str(file)+'\nCODECPROBE_STORE_PASSWORD='+password+'\n')
    with open(os.environ['GITHUB_OUTPUT'],'a') as out:
        out.write('ready=true\n')
    print('Permanent signing key restored; APK updates will reuse this identity.')

if __name__ == '__main__':
    main()
