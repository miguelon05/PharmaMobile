"""Capturas originales del emulador para la sesión 13."""
import sys, re, time
from pathlib import Path
import s11_evidencias as base

OUT = Path(__file__).resolve().parents[1] / 'output/evidencias_s13_android'
OUT.mkdir(parents=True, exist_ok=True)
base.OUT = OUT
base.TMP = OUT.parent / '.s13_estado'
base.TMP.mkdir(exist_ok=True)
_ui_original = base.ui
def ui_estable():
    for intento in range(3):
        try: return _ui_original()
        except RuntimeError:
            if intento == 2: raise
            time.sleep(2)
base.ui = ui_estable

def captura(nombre):
    time.sleep(2)
    (OUT / (nombre + '.png')).write_bytes(base.adb('exec-out', 'screencap', '-p', binary=True))
    print('Captura: ' + nombre)

def campo(indice, texto):
    campos = [n for n in base.ui().iter('node') if n.get('class') == 'android.widget.EditText']
    n = campos[int(indice)]
    x1,y1,x2,y2 = map(int, re.findall(r'\d+', n.get('bounds')))
    base.adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    base.adb('shell','input','keycombination','113','29')
    base.adb('shell','input','keyevent','67')
    base.adb('shell','input','text',texto)
    base.adb('shell','input','keyevent','4')

if __name__ == '__main__':
    if sys.argv[1] == 'login_admin':
        base.tap_text('Iniciar sesión')
        campo(0,'admin'); campo(1,'Admin123'); base.tap_text('Ingresar')
        time.sleep(3)
        base.tap_text('Abrir menú'); base.tap_text('Productos')
        time.sleep(31)
        base.tap_text('Sincronizar')
        time.sleep(3)
        captura('09a_sesion_vencida_retorno_login')
    elif sys.argv[1] == 'captura': captura(sys.argv[2])
    elif sys.argv[1] == 'campo': campo(sys.argv[2],sys.argv[3])
    elif sys.argv[1] == 'tap': base.tap_text(sys.argv[2])
    elif sys.argv[1] == 'ui':
        for n in base.ui().iter('node'):
            if n.get('text') or n.get('content-desc') or n.get('class') == 'android.widget.EditText':
                print(n.get('class'), n.get('text'), n.get('content-desc'), n.get('bounds'))
    else: print(base.adb(*sys.argv[1:]))
