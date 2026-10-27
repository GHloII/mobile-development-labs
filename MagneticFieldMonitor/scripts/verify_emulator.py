from pathlib import Path
import subprocess,time,xml.etree.ElementTree as ET,json,re,os
root=Path(__file__).resolve().parents[1]
sdk=Path(os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME') or Path.home()/'AppData/Local/Android/Sdk')
adb=str(sdk/'platform-tools'/('adb.exe' if os.name=='nt' else 'adb'))
out=root/'app/build/verification'; out.mkdir(parents=True,exist_ok=True)
def run(*args): return subprocess.check_output([adb,*args],stderr=subprocess.STDOUT).decode('utf-8',errors='replace')
def dump(name):
    run('shell','uiautomator','dump','/sdcard/lab2.xml'); run('pull','/sdcard/lab2.xml',str(out/(name+'.xml')))
    return ET.parse(out/(name+'.xml')).getroot()
def shot(name): (out/(name+'.png')).write_bytes(subprocess.check_output([adb,'exec-out','screencap','-p']))
def texts(r):return '\n'.join(n.get('text','') for n in r.iter('node'))
def tap(label):
    for _ in range(5):
        r=dump('working')
        for n in r.iter('node'):
            if n.get('text')==label:
                a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds')))
                if d-b<20:continue
                run('shell','input','tap',str((a+c)//2),str((b+d)//2));time.sleep(.5);return
        run('shell','input','swipe','500','1800','500','700','350')
    raise AssertionError('Not found '+label)
run('install','-r',str(out.parent/'outputs/apk/debug/app-debug.apk'))
run('shell','cmd','window','user-rotation','lock','0');run('shell','cmd','uimode','night','no')
run('shell','am','force-stop','g433_v5.lab2');run('shell','am','start','-n','g433_v5.lab2/.MainActivity')
run('emu','sensor','set','magnetic-field','3:4:12');time.sleep(1)
r=dump('field-3-4-12');t=texts(r);assert '13.00' in t and '3.00' in t and '4.00' in t and '12.00' in t,t;shot('portrait-light')
run('emu','sensor','set','magnetic-field','-6:8:0');time.sleep(.6);t=texts(dump('field-negative'));assert '-6.00' in t and '10.00' in t,t
run('emu','sensor','set','magnetic-field','0:0:0');time.sleep(.6);t=texts(dump('field-zero'));assert 'Модуль: 0.00' in t,t
run('emu','sensor','set','magnetic-field','3:4:12');time.sleep(.5)
tap('Остановить измерение');t=texts(dump('paused'));assert 'Измерение приостановлено' in t,t
run('emu','sensor','set','magnetic-field','0:0:50');time.sleep(.5);t=texts(dump('paused-changed'));assert '13.00' in t and '50.00' not in t,t;shot('paused')
# A stopped session must remain stopped after recreation.
run('shell','cmd','window','user-rotation','lock','1');time.sleep(.7);t=texts(dump('paused-rotated'));assert 'Измерение приостановлено' in t and 'Начать измерение' in t,t
run('shell','cmd','window','user-rotation','lock','0');time.sleep(.7)
tap('Начать измерение');time.sleep(.6);t=texts(dump('resumed'));assert '50.00' in t and 'Выполняется измерение' in t,t
tap('Список датчиков');t=texts(dump('sensors'));assert 'Найдено датчиков:' in t and 'Производитель:' in t,t;shot('sensors')
tap('Назад');time.sleep(.6);t=texts(dump('returned'));assert 'Выполняется измерение' in t,t
tap('Об авторах');t=texts(dump('authors'));assert 'Д.Д. Панина' in t and 'Г.И. Иванов' in t,t;shot('authors');run('shell','input','keyevent','4')
run('shell','am','force-stop','g433_v5.lab2');run('shell','am','start','-n','g433_v5.lab2/.MainActivity');run('emu','sensor','set','magnetic-field','3:4:12');time.sleep(.6)
run('shell','cmd','window','user-rotation','lock','1');time.sleep(.7);t=texts(dump('landscape-light'));assert '13.00' in t,t;shot('landscape-light')
run('shell','cmd','uimode','night','yes');time.sleep(.8);t=texts(dump('landscape-dark'));assert '13.00' in t,t;shot('landscape-dark')
run('shell','cmd','window','user-rotation','lock','0');time.sleep(.7);t=texts(dump('portrait-dark'));assert '13.00' in t,t;shot('portrait-dark')
run('shell','input','keyevent','3');time.sleep(.4);(out/'sensors-background.txt').write_text(run('shell','dumpsys','sensorservice'),encoding='utf-8')
run('shell','am','start','-n','g433_v5.lab2/.MainActivity');time.sleep(.6);t=texts(dump('foreground'));assert 'Выполняется измерение' in t,t
(out/'sensors-foreground.txt').write_text(run('shell','dumpsys','sensorservice'),encoding='utf-8')
run('shell','cmd','uimode','night','no');time.sleep(.5)
result={'device':run('shell','getprop','ro.product.model').strip(),'android':run('shell','getprop','ro.build.version.release').strip(),'api':run('shell','getprop','ro.build.version.sdk').strip(),'tests':['3,4,12 => 13','-6,8,0 => 10','0,0,0 => 0','pause freezes values','paused state survives rotation','resume receives latest values','sensor list opens and returns','author dialog','light portrait and landscape','dark portrait and landscape','return from background resumes'],'status':'PASS'}
(out/'results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(result,ensure_ascii=False))
