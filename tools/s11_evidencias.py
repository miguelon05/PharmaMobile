"""Capturas reales y estado SQLite del emulador de desarrollo. No exporta credenciales."""
import json
import sqlite3
import subprocess
import sys
import re
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/evidencias/s11"
TMP = ROOT / ".local/s11"
ADB = r"C:/Users/mygna/AppData/Local/Android/Sdk/platform-tools/adb.exe"
PACKAGE = "pe.edu.upeu.pharmamobil"
OUT.mkdir(parents=True, exist_ok=True)
TMP.mkdir(parents=True, exist_ok=True)

def adb(*args, binary=False):
    result = subprocess.run([ADB, "-s", "emulator-5554", *args], capture_output=True, timeout=45)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors="replace"))
    return result.stdout if binary else result.stdout.decode(errors="replace")

def pantalla(nombre):
    (OUT / (nombre + ".png")).write_bytes(adb("exec-out", "screencap", "-p", binary=True))
    estado = {"fecha_hora": datetime.now(ZoneInfo("America/Lima")).isoformat(), "plataforma": "Android / Pixel 7a", "modo_avion": adb("shell", "settings", "get", "global", "airplane_mode_on").strip()}
    for suffix in ["", "-wal", "-shm"]:
        target = TMP / ("pharma.db" + suffix)
        try:
            data = adb("exec-out", "run-as", PACKAGE, "cat", "databases/pharma.db" + suffix, binary=True)
            if data.startswith(b"cat:"): raise RuntimeError("Base no creada")
            target.write_bytes(data)
        except RuntimeError:
            target.unlink(missing_ok=True)
    if (TMP / "pharma.db").exists():
        with sqlite3.connect(TMP / "pharma.db") as db:
            db.row_factory = sqlite3.Row
            estado["productos"] = [dict(row) for row in db.execute("SELECT * FROM producto ORDER BY id")]
            if db.execute("SELECT name FROM sqlite_master WHERE name='sincronizacion'").fetchone():
                estado["sincronizacion"] = [dict(row) for row in db.execute("SELECT * FROM sincronizacion")]
            else:
                estado["sincronizacion"] = "Pendiente de abrir/migrar la base desde Productos"
    else:
        estado["base_local"] = "No creada: primera apertura sin sesión"
    (OUT / (nombre + ".json")).write_text(json.dumps(estado, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(estado, ensure_ascii=False))

def ui():
    result = adb("shell", "uiautomator", "dump", "/sdcard/s11-ui.xml")
    if "dumped" not in result: raise RuntimeError(result)
    return ET.fromstring(adb("shell", "cat", "/sdcard/s11-ui.xml"))

def tap_text(texto):
    candidatos = [n for n in ui().iter("node") if n.get("text") == texto or n.get("content-desc") == texto]
    if len(candidatos) != 1: raise RuntimeError(f"Se esperó un control {texto!r}; encontrados {len(candidatos)}")
    x1,y1,x2,y2 = map(int, re.findall(r"\d+", candidatos[0].get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))

if __name__ == "__main__":
    if sys.argv[1] == "captura": pantalla(sys.argv[2])
    elif sys.argv[1] == "ui":
        for n in ui().iter("node"):
            if n.get("text") or n.get("content-desc") or n.get("class") == "android.widget.EditText":
                print(n.get("class"), n.get("text"), n.get("content-desc"), n.get("bounds"))
    elif sys.argv[1] == "tap": tap_text(sys.argv[2])
    else: print(adb(*sys.argv[1:]))
