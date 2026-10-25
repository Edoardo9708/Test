param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$TestArguments
)
$ErrorActionPreference = 'Stop'
$bundled = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
$python = Get-Command python,python3 -ErrorAction SilentlyContinue | Select-Object -First 1
if (Test-Path -LiteralPath $bundled) { $executable = $bundled }
elseif ($python) { $executable = $python.Source }
else { throw 'Python 3 is required. Use the existing Codex runtime or configure Python; this script installs nothing.' }
& $executable -X utf8 (Join-Path $PSScriptRoot 'test-local.py') @TestArguments
exit $LASTEXITCODE
