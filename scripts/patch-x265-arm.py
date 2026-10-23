"""Minimal build portability fixes for pinned x265; encoding algorithms unchanged."""
from pathlib import Path
root = Path("vendor/x265/source")
p = root / "CMakeLists.txt"
original = p.read_text()
s = original
old = "ARGS ${ARM_ARGS} -c ${ASM_SRC} -o ${ASM}.${SUFFIX}"
new = "ARGS ${PROBE_ASM_TARGET_FLAGS} ${ARM_ARGS} -c ${ASM_SRC} -o ${ASM}.${SUFFIX}"
if old in s:
    s = s.replace(old, new)
else:
    assert new in s, "Pinned ARM build rule changed"
if s != original:
    p.write_text(s)
# LLVM integrated assembler does not support GNU .func/.endfunc directives.
# ELF .type/.size metadata remains intact.
p = root / "common/aarch64/asm.S"
original = p.read_text()
s = original
s = "\n".join(line for line in s.split("\n")
              if not (line.startswith("FUNC") and (".func" in line or ".endfunc" in line)))
if s != original:
    p.write_text(s)
print("Applied pinned x265 ARM assembler portability fixes")
