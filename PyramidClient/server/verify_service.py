"""Exercise the real HTTPS/PHP service with certificate verification and known answers."""
from pathlib import Path
import json
import math
import ssl
import urllib.request
import urllib.parse
import urllib.error

root = Path(__file__).resolve().parent
tls = ssl.create_default_context(cafile=str(root / '.local/ca.pem'))
url = 'https://localhost:8443/solve.php'
results = []

def request(data, expected=200, method='POST', mime='application/x-www-form-urlencoded'):
    body = urllib.parse.urlencode(data).encode()
    req = urllib.request.Request(url, body if method != 'GET' else None,
                                 {'Content-Type': mime}, method=method)
    try:
        with urllib.request.urlopen(req, context=tls, timeout=10) as response:
            status, value = response.status, json.load(response)
    except urllib.error.HTTPError as error:
        status, value = error.code, json.load(error)
    assert status == expected, (data, status, value)
    assert value['ok'] == (expected == 200), value
    return value

for a, b, h, surface, volume in [
    (6, 6, 4, 96, 48),
    (6, 8, 4, 121.94112549695429, 64),
    (1, 1, 1, 3.23606797749979, 1/3),
    (1e6, 1e6, 1e6, 3.23606797749979e12, 1e18/3),
]:
    value = request({'a': a, 'b': b, 'h': h})['res']
    assert math.isclose(value['surface'], surface, rel_tol=1e-12), value
    assert math.isclose(value['volume'], volume, rel_tol=1e-12), value
    results.append(f'known answer {a}, {b}, {h}')

for data in [{}, {'a': '', 'b': 1, 'h': 1}, {'a': 0, 'b': 1, 'h': 1},
             {'a': -1, 'b': 1, 'h': 1}, {'a': 'NaN', 'b': 1, 'h': 1},
             {'a': '1e309', 'b': 1, 'h': 1}, {'a': 1000001, 'b': 1, 'h': 1},
             {'a': 'abc', 'b': 1, 'h': 1}, {'a[]': '1', 'b': 1, 'h': 1},
             {'a': '1e-200', 'b': '1e-200', 'h': '1e-200'}]:
    request(data, 422)
    results.append(f'invalid input {data}')
request({}, 405, 'GET'); results.append('GET rejected, Allow POST')
request({'a': 1}, 415, mime='application/json'); results.append('wrong content type rejected')
out = root.parent / 'app/build/verification'
out.mkdir(parents=True, exist_ok=True)
(out / 'service-results.json').write_text(json.dumps({'status': 'PASS', 'checks': results}, indent=2), encoding='utf-8')
print(f'PASS: {len(results)} HTTPS/PHP checks')
