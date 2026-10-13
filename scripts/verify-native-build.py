"""Check actual Android compiler commands and the packaged ELF, not labels."""
import json
import shlex
import struct
import zipfile
from pathlib import Path

databases = list(Path("app/.cxx").rglob("compile_commands.json"))
assert databases, "Missing Android compiler commands"
count = 0
for database in databases:
    for entry in json.loads(database.read_text()):
        source = entry["file"]
        if not source.endswith((".cpp", ".c", ".cc")):
            continue
        args = entry.get("arguments") or shlex.split(entry["command"])
        options = [arg for arg in args if arg.startswith("-O")]
        assert options and options[-1] == "-O3", (source, options)
        assert "aarch64" in " ".join(args), source
        count += 1
assert count > 50, f"Expected x265 and JNI sources; only {count} checked"
apk = Path("app/build/outputs/apk/debug/app-debug.apk")
with zipfile.ZipFile(apk) as archive:
    data = archive.read("lib/arm64-v8a/libx265probe.so")
    assert data[:4] == b"\x7fELF" and data[4] == 2 and data[5] == 1, "Not ELF64 little endian"
    assert struct.unpack_from("<H", data, 18)[0] == 183, "Not AArch64"
print(f"OK: {count} native compiler commands use AArch64 and -O3; APK contains ELF64 AArch64")

# Verify the dispatcher was compiled and the actual assembly symbols were linked.
import os
import subprocess
native = list(Path("app/.cxx").rglob("libx265probe.so"))
assert native, "Missing unstripped native library"
sdk = Path(os.environ["ANDROID_HOME"])
nm = sdk / "ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-nm"
symbols = subprocess.check_output([str(nm), "--defined-only", str(native[0])], text=True)
assert "x265_sad_x" in symbols, "ARM64 SAD routine not linked"
assert "setupAssemblyPrimitives" in symbols, "Assembly dispatcher not linked"
assert any("aarch64/asm-primitives.cpp" in e["file"]
           for d in databases for e in json.loads(d.read_text())), "Missing ARM64 dispatcher compilation"
print("OK: ARM64 assembly dispatcher and SAD routines linked")
