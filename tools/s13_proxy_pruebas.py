"""Proxy local de pruebas: acorta a 30 s la vigencia del JWT de desarrollo.
No cambia el servidor ni guarda contraseñas o tokens en las evidencias.
"""
import base64, hashlib, hmac, json, re, time, urllib.request, urllib.error
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
config = (ROOT.parent/'pharmasefutyity/src/main/resources/application-dev.yaml').read_text()
KEY = base64.b64decode(re.search(r'(?m)^\s+secret:\s*(\S+)',config)[1])

def enc(data): return base64.urlsafe_b64encode(data).rstrip(b'=')
def token_corto(token, segundos=30):
    header,payload,_ = token.split('.')
    claims=json.loads(base64.urlsafe_b64decode(payload+'==='))
    claims['iat']=int(time.time())
    claims['exp']=int(time.time())+segundos
    body=header.encode()+b'.'+enc(json.dumps(claims,separators=(',',':')).encode())
    alg=json.loads(base64.urlsafe_b64decode(header+'==='))['alg']
    firma=hmac.new(KEY,body,{'HS256':hashlib.sha256,'HS384':hashlib.sha384,'HS512':hashlib.sha512}[alg]).digest()
    return (body+b'.'+enc(firma)).decode()

class Proxy(BaseHTTPRequestHandler):
    def log_message(self,*args): pass
    def do_GET(self): self.enviar()
    def do_POST(self): self.enviar()
    def do_PUT(self): self.enviar()
    def do_DELETE(self): self.enviar()
    def enviar(self):
        path=urllib.parse.urlsplit(self.path).path
        if not path.startswith('/api/'):
            self.send_error(404); return
        datos=self.rfile.read(int(self.headers.get('Content-Length','0'))) or None
        headers={k:v for k,v in self.headers.items() if k.lower() not in ['host','connection','proxy-connection','content-length','accept-encoding']}
        req=urllib.request.Request('http://127.0.0.1:8080'+path,data=datos,headers=headers,method=self.command)
        try: response=urllib.request.urlopen(req,timeout=25)
        except urllib.error.HTTPError as e: response=e
        raw=response.read()
        if path.endswith('/auth/login') and response.status==200:
            obj=json.loads(raw); obj['token']=token_corto(obj['token']); obj['expiraEnSegundos']=30
            raw=json.dumps(obj).encode()
        self.send_response(response.status)
        self.send_header('Content-Type','application/json; charset=utf-8')
        self.send_header('Content-Length',str(len(raw))); self.end_headers(); self.wfile.write(raw)
        print(self.command,path,response.status,flush=True)

if __name__=='__main__': ThreadingHTTPServer(('127.0.0.1',18080),Proxy).serve_forever()
