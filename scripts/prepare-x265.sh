#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
revision=419182243fb2e2dfbe91dfc45a51778cf704f849
mkdir -p vendor
if [ ! -d vendor/x265/.git ]; then
  git clone --no-checkout https://github.com/videolan/x265.git vendor/x265
fi
git -C vendor/x265 checkout --detach "$revision"
test "$(git -C vendor/x265 rev-parse HEAD)" = "$revision"
mkdir -p app/src/main/assets
cp vendor/x265/COPYING app/src/main/assets/x265-COPYING.txt
printf '%s\n' "$revision" > app/src/main/assets/x265-revision.txt

python3 scripts/patch-x265-arm.py
