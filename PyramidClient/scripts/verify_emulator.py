"""Real app -> HTTPS -> PHP checks on one disposable emulator.

Build debug APK, start server/serve_dev.py and use adb reverse tcp:8443 tcp:8443.
The script installs the APK and changes this app's fields, rotation and theme.
An additional localhost-only HTTPS fixture exercises error handling, not geometry.
"""
from pathlib import Path
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
import json, os, re, shlex, ssl, subprocess, threading, time
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
sdk = Path(os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME') or Path.home()/'AppData/Local/Android/Sdk')
adb = str(sdk/'platform-tools'/('adb.exe' if os.name == 'nt' else 'adb'))
out = root/'app/build/verification'; out.mkdir(parents=True, exist_ok=True)
package = 'g433_v5.lab4'
passed = []

def run(*args):
    return subprocess.check_output([adb, *args], stderr=subprocess.STDOUT, timeout=35).decode('utf-8', errors='replace')

def dump(name='working'):
    for _ in range(3):
        run('shell', 'rm', '-f', '/sdcard/lab4.xml')
        response = run('shell', 'uiautomator', 'dump', '/sdcard/lab4.xml')
        if 'dumped to:' in response: break
        time.sleep(.5)
    else: raise AssertionError('No fresh UI hierarchy')
    run('pull', '/sdcard/lab4.xml', str(out/(name+'.xml')))
    return ET.parse(out/(name+'.xml')).getroot()

def texts(tree): return '\n'.join(n.get('text','') for n in tree.iter('node'))

def tap(id):
    for attempt in range(9):
        tree = dump()
        for node in tree.iter('node'):
            if node.get('resource-id') == package+':id/'+id:
                x1,y1,x2,y2 = map(int, re.findall(r'\d+', node.get('bounds')))
                if y2-y1 >= 20:
                    run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
                    time.sleep(.3)
                    return
        # All coordinates fit both orientations of Pixel_8a.
        start,end = ('850','300') if attempt < 4 else ('300','850')
        run('shell','input','swipe','500',start,'500',end,'250')
    raise AssertionError('Control not found '+id)

def fill(id, value):
    tap(id)
    run('shell','input','keycombination','113','29');run('shell','input','keyevent','67')
    if value: run('shell','input','text',shlex.quote(value))
    run('shell','input','keyevent','4')

def shot(name): (out/(name+'.png')).write_bytes(subprocess.check_output([adb,'exec-out','screencap','-p']))
def check(condition, name):
    assert condition, name
    passed.append(name)
    (out/'ui-progress.json').write_text(json.dumps(passed,ensure_ascii=False),encoding='utf-8')
    print('PASS: '+name,flush=True)
def calculate():
    tap('calculate');time.sleep(.8);tap('result')
    return texts(dump())

class Fixture(BaseHTTPRequestHandler):
    def do_POST(self):
        self.rfile.read(int(self.headers.get('Content-Length',0)))
        if self.path == '/slow': time.sleep(11)
        self.send_response(503 if self.path == '/status' else 200)
        self.send_header('Content-Type','application/json');self.end_headers()
        try: self.wfile.write(b'{broken json')
        except OSError: pass
    def log_message(self,*args): pass

def main():
    tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    tls.load_cert_chain(root/'server/.local/server.pem',root/'server/.local/server-key.pem')
    fixture = ThreadingHTTPServer(('127.0.0.1',9444),Fixture)
    fixture.socket = tls.wrap_socket(fixture.socket,server_side=True)
    threading.Thread(target=fixture.serve_forever,daemon=True).start()
    run('reverse','tcp:9444','tcp:9444')
    try:
        run('install','-r',str(root/'app/build/outputs/apk/debug/app-debug.apk'))
        run('shell','am','force-stop',package)
        run('shell','cmd','window','user-rotation','lock','0');run('shell','cmd','uimode','night','no')
        run('shell','am','start','-W','-n',package+'/.MainActivity')
        check('Введите три числа' in calculate(),'Empty inputs rejected')
        fill('side_a','6');fill('side_b','6');fill('height','4')
        t=calculate();check('96.0000' in t and '48.0000' in t and 'PHP' in t,'Square-base answer 96 and 48 from PHP');shot('portrait-light')
        fill('side_b','8');t=calculate();check('121.941' in t and '64.0000' in t,'Rectangular-base answer from PHP');shot('rectangular')
        fill('side_a','0');check('Введите три числа' in calculate(),'Zero rejected')
        fill('side_a','1000001');check('Введите три числа' in calculate(),'Dimension above upper limit rejected')
        fill('side_a','6')
        fill('endpoint','http://localhost:8443/solve.php');check('Укажите HTTPS' in calculate(),'Cleartext URL rejected')
        fill('endpoint','https://localhost:8443/solve.php?x=1');check('Укажите HTTPS' in calculate(),'Unexpected URL query rejected')
        fill('endpoint','https://127.0.0.1:8443/solve.php');check('сертификат' in calculate(),'Untrusted TLS identity rejected')
        fill('endpoint','https://localhost:9444/status');check('503' in calculate(),'HTTP error shown')
        fill('endpoint','https://localhost:9444/json');check('некорректный ответ' in calculate(),'Malformed JSON rejected');shot('bad-response')
        fill('endpoint','https://localhost:9444/slow');tap('calculate');time.sleep(9);tap('result')
        check('Сервер недоступен' in texts(dump()),'Read timeout recovers UI');shot('timeout')
        fill('endpoint','https://localhost:9444/slow');tap('calculate')
        run('shell','cmd','window','user-rotation','lock','1');time.sleep(.8)
        tap('result');check('Запрос прерван' in texts(dump()),'Rotation cancels in-flight request')
        run('shell','cmd','window','user-rotation','lock','0');time.sleep(.5)
        fill('endpoint','https://localhost:8443/solve.php');fill('side_b','6')
        check('96.0000' in calculate(),'Retry after errors succeeds')
        for rotation,night,name in [('1','no','landscape-light'),('1','yes','landscape-dark'),('0','yes','portrait-dark')]:
            run('shell','cmd','window','user-rotation','lock',rotation);run('shell','cmd','uimode','night',night);time.sleep(.7)
            # Bring result into view after recreation in the portrait ScrollView.
            tap('calculate');time.sleep(.7);tap('result')
            check('96.0000' in texts(dump(name)),name+' preserves input and displays result');shot(name)
        run('shell','cmd','uimode','night','no');time.sleep(.5)
        tap('about');check('Г.И. Иванов' in texts(dump('authors')),'Author dialog');shot('authors')
        run('shell','input','keyevent','4')
        (out/'ui-results.json').write_text(json.dumps({'status':'PASS','checks':passed},ensure_ascii=False,indent=2),encoding='utf-8')
        print('PASS: '+str(len(passed))+' Android checks',flush=True)
    finally:
        fixture.shutdown();fixture.server_close()
        run('reverse','--remove','tcp:9444')

if __name__ == '__main__': main()
