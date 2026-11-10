"""Resultados reales del servicio, exportados como imágenes (sin tokens)."""
import json, urllib.request, urllib.error, textwrap, sys
from pathlib import Path
from datetime import datetime
from PIL import Image, ImageDraw, ImageFont
from s13_proxy_pruebas import token_corto

OUT=Path(__file__).resolve().parents[1]/'output/evidencias_s13_android'
def req(path, data=None, token=None):
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    q=urllib.request.Request('http://127.0.0.1:8080/api/v1/'+path,data=json.dumps(data).encode() if data is not None else None,headers=headers)
    try: r=urllib.request.urlopen(q,timeout=30)
    except urllib.error.HTTPError as e: r=e
    return r.status,json.loads(r.read())
def imagen(name,title,lines):
    font=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',24)
    big=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',34)
    text=[]
    for line in lines: text.extend(textwrap.wrap(line,84,replace_whitespace=False) or [''])
    im=Image.new('RGB',(1320,220+len(text)*36),'#f7f5fc'); d=ImageDraw.Draw(im)
    d.rectangle((0,0,1320,105),fill='#463378'); d.text((40,32),title,font=big,fill='white')
    d.text((40,125),'RESULTADO HTTP REAL | Servidor local | '+datetime.now().strftime('%Y-%m-%d %H:%M:%S'),font=font,fill='#463378')
    for i,line in enumerate(text):d.text((40,185+i*36),line,font=font,fill='#222222')
    im.save(OUT/(name+'.png'))
if __name__=='__main__':
    status,admin=req('auth/login',{'username':'admin','password':'Admin123'})
    assert status==200
    token=admin['token']
    codigo,res=req('productos',{'nombre':'ab','precio':12.5,'stock':3,'estado':True,'categoriaId':1},token)
    assert codigo==400,(codigo,res)
    imagen('07b_validacion_HTTP_400','Validación del servicio: HTTP 400',[
        'POST /api/v1/productos | Usuario de demostración: admin',
        'Entrada: nombre="ab", precio=12.5, stock=3, categoriaId=1',
        'Respuesta: HTTP '+str(codigo), *json.dumps(res,ensure_ascii=False,indent=2).splitlines(),
        'Esta imagen presenta la respuesta real del servicio; no es una captura de la app.'])
    _,vendedor=req('auth/login',{'username':'vendedor','password':'Vendedor123'})
    codigo,res=req('productos',{'nombre':'S13_NoDebeCrearse','precio':12.5,'stock':3,'estado':True,'categoriaId':1},vendedor['token'])
    assert codigo==403,(codigo,res)
    imagen('08b_operacion_sin_privilegio_HTTP_403','Operación sin privilegios: HTTP 403',[
        'POST /api/v1/productos | Usuario de demostración: vendedor',
        'Intento de creación sin permiso CATALOGO_ESCRIBIR.',
        'Respuesta: HTTP '+str(codigo),*json.dumps(res,ensure_ascii=False,indent=2).splitlines(),
        'Esta imagen presenta la respuesta real del servicio; no es una captura de la app.'])
    codigo,res=req('auth/me',token=token_corto(token,-30))
    assert codigo==401,(codigo,res)
    imagen('09b_token_prueba_vencido_HTTP_401','Token de prueba vencido: HTTP 401',[
        'GET /api/v1/auth/me | JWT de desarrollo firmado con expiración pasada.',
        'Respuesta: HTTP '+str(codigo),*json.dumps(res,ensure_ascii=False,indent=2).splitlines(),
        'Esta imagen comprueba el rechazo del servicio; el comportamiento de la app se captura aparte.'])
    print('Verificados: HTTP 400, 403 y 401. Imágenes guardadas sin credenciales ni tokens.')
