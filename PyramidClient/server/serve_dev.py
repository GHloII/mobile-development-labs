"""Local HTTPS gateway -> real PHP built-in server on loopback, for lab demonstration.

Usage: python serve_dev.py --php C:/path/php.exe
Then: adb reverse tcp:8443 tcp:8443
Not a production web server. Use Apache/Nginx with valid TLS for deployment.
"""
from pathlib import Path
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
import argparse
import http.client
import ssl
import subprocess
import time

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--php', required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent
    # PHP's unencrypted development listener is accessible only on host loopback.
    php = subprocess.Popen([args.php, '-S', '127.0.0.1:8084', '-t', str(root)])
    class Gateway(BaseHTTPRequestHandler):
        def forward(self):
            if self.path != '/solve.php':
                self.send_error(404)
                return
            try:
                length = int(self.headers.get('Content-Length', '0'))
            except ValueError:
                self.send_error(400)
                return
            if length < 0 or length > 1024:
                self.send_error(413)
                return
            connection = http.client.HTTPConnection('127.0.0.1', 8084, timeout=8)
            try:
                body = self.rfile.read(length)
                connection.request(self.command, self.path, body, {
                    'Content-Type': self.headers.get('Content-Type', ''),
                    'Content-Length': str(length)})
                response = connection.getresponse()
                data = response.read(16385)
                self.send_response(response.status)
                for name in ['Content-Type', 'Cache-Control', 'Allow', 'X-Content-Type-Options']:
                    if response.getheader(name): self.send_header(name, response.getheader(name))
                self.send_header('Content-Length', str(len(data)))
                self.end_headers()
                self.wfile.write(data)
            except (OSError, http.client.HTTPException):
                self.send_error(502)
            finally:
                connection.close()
        do_POST = forward
        do_GET = forward
        do_PUT = forward
    try:
        time.sleep(.5)
        if php.poll() is not None: raise RuntimeError('PHP did not start')
        tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        tls.minimum_version = ssl.TLSVersion.TLSv1_2
        tls.load_cert_chain(root / '.local/server.pem', root / '.local/server-key.pem')
        with ThreadingHTTPServer(('127.0.0.1', 8443), Gateway) as server:
            server.socket = tls.wrap_socket(server.socket, server_side=True)
            print('HTTPS ready: https://localhost:8443/solve.php', flush=True)
            server.serve_forever()
    finally:
        php.terminate()
        php.wait(timeout=5)

if __name__ == '__main__': main()
