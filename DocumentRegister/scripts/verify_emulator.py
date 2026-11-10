"""Exercise lab 3 through the UI on a disposable emulator app installation.

WARNING: clears only com.g433_v5.lab3 data before running the test cases.
Uses Python's standard library, ADB and the built debug APK.
"""
from pathlib import Path
import json, os, re, shlex, sqlite3, subprocess, time
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
sdk = Path(os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME') or Path.home()/'AppData/Local/Android/Sdk')
adb = str(sdk/'platform-tools'/('adb.exe' if os.name == 'nt' else 'adb'))
out = root/'app/build/verification'
out.mkdir(parents=True, exist_ok=True)
package = 'com.g433_v5.lab3'
passed = []

def run(*args):
    return subprocess.check_output([adb, *args], stderr=subprocess.STDOUT, timeout=35).decode('utf-8', errors='replace')

def dump(name='working'):
    for attempt in range(3):
        run('shell', 'rm', '-f', '/sdcard/lab3.xml')
        result = run('shell', 'uiautomator', 'dump', '/sdcard/lab3.xml')
        if 'dumped to:' in result: break
        time.sleep(1)
    else: raise AssertionError('UI Automator did not produce a fresh hierarchy: '+result)
    run('pull', '/sdcard/lab3.xml', str(out/(name+'.xml')))
    return ET.parse(out/(name+'.xml')).getroot()

def texts(tree):
    return '\n'.join(n.get('text', '') for n in tree.iter('node'))

def tap(label=None, id=None):
    for _ in range(5):
        tree = dump()
        for node in tree.iter('node'):
            if (id and node.get('resource-id') == package+':id/'+id) or (label is not None and node.get('text','').casefold() == label.casefold()):
                x1,y1,x2,y2 = map(int,re.findall(r'\d+', node.get('bounds')))
                if y2-y1 < 15: continue
                run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
                time.sleep(.6)
                return
        run('shell','input','swipe','500','1800','500','750','300')
    raise AssertionError(f'Control not found: {label or id}')

def fill(id, value):
    tap(id=id)
    run('shell','input','keycombination','113','29')
    run('shell','input','keyevent','67')
    if value: run('shell','input','text',shlex.quote(value.replace(' ', '%s')))
    run('shell','input','keyevent','4')

def shot(name):
    (out/(name+'.png')).write_bytes(subprocess.check_output([adb,'exec-out','screencap','-p']))

def rows():
    data = subprocess.check_output([adb,'exec-out','run-as',package,'cat','databases/documents.db'])
    path = out/'database.db'; path.write_bytes(data)
    with sqlite3.connect(path) as db:
        return db.execute('SELECT _id,document_type,employee,document_date FROM documents ORDER BY document_date,_id').fetchall()

def create(name, date, kind=0):
    tap(id='add')
    fill('employee',name);fill('date',date)
    if kind: tap(id='type');tap('Заявление')
    tap(id='save');time.sleep(.4)

def check(condition, name):
    assert condition,name
    passed.append(name)
    (out/'progress.json').write_text(json.dumps(passed,ensure_ascii=False),encoding='utf-8')
    print('PASS: '+name, flush=True)

run('install','-r',str(root/'app/build/outputs/apk/debug/app-debug.apk'))
run('shell','pm','clear',package)
run('shell','cmd','window','user-rotation','lock','0')
run('shell','cmd','uimode','night','no')
run('shell','am','start','-W','-n',package+'/.MainActivity');time.sleep(.8)
check('Документов пока нет' in texts(dump('empty')), 'Empty database and placeholder')
tap(id='add');tap(id='save')
check(len(rows()) == 0 and 'Новый документ' in texts(dump('empty-name')), 'Reject empty employee')
fill('employee','   ');tap(id='save')
check(len(rows()) == 0, 'Reject whitespace employee')
fill('employee','Ivanov I.I.')
for value in ['','31.02.2026','29.02.2025','01.01.1899','01.01.2101','1.2.2026']:
    fill('date',value);tap(id='save')
    check(len(rows()) == 0 and 'Новый документ' in texts(dump()), 'Reject date '+repr(value))
shot('validation')
fill('date','29.02.2024');tap(id='save')
check(rows()[0][3] == '2024-02-29', 'Accept valid leap day')
create('Petrova A.A.','15.10.2026',1)
create('Sidorov P.P.','01.09.2025')
data=rows();check(len(data)==3 and [r[3] for r in data]==['2024-02-29','2025-09-01','2026-10-15'], 'Insert both types and sort dates ascending')
t=texts(dump('sorted'));check(t.index('Ivanov I.I.') < t.index('Sidorov P.P.') < t.index('Petrova A.A.'), 'RecyclerView order matches SQLite order')
tap('Petrova A.A.');fill('employee',"O'Neil A.A.");fill('date','01.01.2020');tap(id='save')
check(rows()[0][2:] == ("O'Neil A.A.",'2020-01-01') and len(rows())==3, 'Edit stable ID, quotes in name and reorder')
tap("O'Neil A.A.");fill('employee','Unsaved Draft')
run('shell','cmd','window','user-rotation','lock','1');time.sleep(.8)
check('Unsaved Draft' in texts(dump('draft-landscape')), 'Unsaved draft survives rotation');shot('edit-landscape')
run('shell','cmd','window','user-rotation','lock','0');time.sleep(.7)
tap(id='cancel');check(rows()[0][2]=="O'Neil A.A.",'Cancel discards draft')
tap(id='clear');tap('Отмена');check(len(rows())==3,'Cancel delete all')
tap(id='row_delete');tap('Отмена');check(len(rows())==3,'Cancel delete one')
tap(id='row_delete');tap('Удалить');check(len(rows())==2,'Delete one from list')
tap('Ivanov I.I.');tap(id='delete');tap('Удалить');check(len(rows())==1,'Delete from editor')
run('shell','am','force-stop',package);run('shell','am','start','-W','-n',package+'/.MainActivity');time.sleep(.6)
check(len(rows())==1 and 'Sidorov P.P.' in texts(dump('relaunch')),'Data survives process restart')
tap(id='clear');shot('clear-confirmation');tap('Удалить')
check(rows()==[] and 'Документов пока нет' in texts(dump('cleared')),'Delete all and empty state')
# Boundary years and deterministic tie order.
create('Boundary First','01.01.1900');create('Boundary Last','31.12.2100',1)
create('Boundary Same Date','31.12.2100')
check([r[2] for r in rows()]==['Boundary First','Boundary Last','Boundary Same Date'],'Accept year boundaries and order equal dates by ID')
tap(id='clear');tap('Удалить')
# Representative sample left on the emulator for manual review and report figures.
create('Ivanov I.I.','02.11.2026');create('Panina D.D.','05.11.2026',1);create('Divnogorsky A.A.','10.11.2026')
dump('portrait-light');shot('portrait-light')
tap('Panina D.D.');dump('edit-portrait');shot('edit-portrait')
tap(id='choose_date');dump('date-picker');shot('date-picker');run('shell','input','keyevent','4');tap(id='cancel')
for rotation,night,name in [('1','no','landscape-light'),('1','yes','landscape-dark'),('0','yes','portrait-dark')]:
    run('shell','cmd','window','user-rotation','lock',rotation)
    run('shell','cmd','uimode','night',night);time.sleep(.8)
    check('Panina D.D.' in texts(dump(name)),name);shot(name)
tap('Panina D.D.');dump('edit-dark');shot('edit-dark');tap(id='cancel')
tap(id='authors');check('Г.И. Иванов' in texts(dump('authors')),'Author information');shot('authors');tap('Закрыть')
run('shell','cmd','uimode','night','no');time.sleep(.5)
result={'status':'PASS','api':run('shell','getprop','ro.build.version.sdk').strip(),'tests':passed}
(out/'results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False))
