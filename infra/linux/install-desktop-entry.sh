#!/usr/bin/env bash
#
# Registers this MatrosDMS folder with your desktop: menu entry, icon, and a launcher you can pin.
# Nothing is copied and nothing needs root — the entry points straight at the files you unpacked,
# so moving or deleting this folder is all the uninstall there is (plus --uninstall below).
#
#   ./install-desktop-entry.sh              # add the menu entry
#   ./install-desktop-entry.sh --uninstall  # remove it again
#
# Prefer a system package? Use the .deb from the same release — it does all of this for you.
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_BIN="$APP_DIR/bin/MatrosDMS"
DESKTOP_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/applications"
DESKTOP_FILE="$DESKTOP_DIR/matrosdms.desktop"

if [ "${1:-}" = "--uninstall" ]; then
  rm -f "$DESKTOP_FILE"
  command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database "$DESKTOP_DIR" || true
  echo "Removed $DESKTOP_FILE"
  exit 0
fi

if [ ! -x "$APP_BIN" ]; then
  echo "error: $APP_BIN not found or not executable." >&2
  echo "Run this script from inside the unpacked MatrosDMS folder." >&2
  exit 1
fi

# jpackage puts the icon in lib/ next to the launcher; fall back to the app dir if that ever moves.
ICON="$APP_DIR/lib/MatrosDMS.png"
[ -f "$ICON" ] || ICON="$APP_DIR/MatrosDMS.png"

mkdir -p "$DESKTOP_DIR"
cat > "$DESKTOP_FILE" <<EOF
[Desktop Entry]
Type=Application
Version=1.0
Name=MatrosDMS
GenericName=Document Management
Comment=Local-first document management — file by context, search the full text, encrypted at rest
Exec="$APP_BIN"
Icon=$ICON
Terminal=false
Categories=Office;Utility;
Keywords=documents;dms;archive;scan;ocr;search;
StartupNotify=true
EOF
chmod +x "$DESKTOP_FILE"

command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database "$DESKTOP_DIR" || true

echo "Installed $DESKTOP_FILE"
echo "MatrosDMS should now appear in your application menu under Office."
