#!/usr/bin/env python3
"""Check that the built APK certificate matches the permanent keystore."""
import hashlib
import os
from pathlib import Path
import re
import subprocess

key = os.environ['CODECPROBE_STORE_FILE']
cert = subprocess.run(['keytool','-exportcert','-keystore',key,'-storetype','PKCS12','-storepass:env','CODECPROBE_STORE_PASSWORD','-alias','codecprobe'],check=True,capture_output=True).stdout
expected = hashlib.sha256(cert).hexdigest()
sdk = os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT']
signer = Path(sdk)/'build-tools'/'35.0.0'/'apksigner'
apk = Path('app/build/outputs/apk/debug/app-debug.apk')
result = subprocess.run([str(signer),'verify','--print-certs',str(apk)],check=True,capture_output=True,text=True)
actual = re.findall(r'Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]+)',result.stdout)
if [value.lower() for value in actual] != [expected]:
    raise SystemExit('APK signature differs from permanent key; publication blocked.')
print('OK permanent APK signing certificate SHA-256: '+expected)
