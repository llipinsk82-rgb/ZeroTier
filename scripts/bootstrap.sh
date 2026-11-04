#!/usr/bin/env bash
set -euo pipefail
source_root=$(cd -- "$(dirname -- "$0")/.." && pwd)
dest=${1:?Usage: bootstrap.sh NEW_DESTINATION}
if [[ -e "$dest" ]]; then echo 'Destination exists; nothing overwritten' >&2; exit 1; fi
git clone --no-checkout https://github.com/kaaass/ZerotierFix.git "$dest"
git -C "$dest" checkout --detach 0373a58c03ffe053e92212a6fa2d98245db8aa9c
git -C "$dest" submodule update --init --depth 1
for patch in "$source_root"/patches/000{1,2,3,4,5,6}-*.patch; do
  git -C "$dest" apply --check "$patch"
  git -C "$dest" apply "$patch"
done
printf 'Source ready: %s\nBuild locally with ./gradlew :app:testDebugUnitTest :app:assembleDebug\n' "$dest"
