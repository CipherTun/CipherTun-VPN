#!/data/data/com.termux/files/usr/bin/bash
set -e
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="$ROOT_DIR/third_party/termux-app"
TERMUX_REF="master"

rm -rf "$TARGET_DIR"
mkdir -p "$TARGET_DIR"
cd "$TARGET_DIR"

git init -q
git remote add origin https://github.com/termux/termux-app.git
git config core.sparseCheckout true
echo "terminal-emulator/*" >> .git/info/sparse-checkout
echo "terminal-view/*" >> .git/info/sparse-checkout
git fetch --depth 1 origin "$TERMUX_REF"
git checkout FETCH_HEAD

echo "Fetched terminal-emulator and terminal-view into $TARGET_DIR"
