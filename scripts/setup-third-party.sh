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

for file in \
    "$TARGET_DIR/terminal-emulator/build.gradle" \
    "$TARGET_DIR/terminal-view/build.gradle"
do
    if [ -f "$file" ]; then
        sed -i \
            "s/getDefaultProguardFile('proguard-android.txt')/getDefaultProguardFile('proguard-android-optimize.txt')/g" \
            "$file"
    fi
done

if grep -R "getDefaultProguardFile('proguard-android.txt')" \
    "$TARGET_DIR/terminal-emulator" \
    "$TARGET_DIR/terminal-view" 2>/dev/null
then
    echo "ERROR: obsolete proguard-android.txt configuration remains."
    exit 1
fi

echo "Fetched and patched terminal-emulator and terminal-view for AGP 9.4."