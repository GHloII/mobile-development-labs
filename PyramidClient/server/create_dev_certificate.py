"""Create a local-only CA/server certificate. Rebuild the debug APK afterwards.

Requires Python cryptography on the development computer; no Android dependency.
Private keys stay in ignored .local/ and are never packaged in the APK.
"""
from pathlib import Path
from datetime import datetime, timedelta, timezone
import ipaddress
from cryptography import x509
from cryptography.x509.oid import NameOID, ExtendedKeyUsageOID
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa

root = Path(__file__).resolve().parent
local = root / '.local'
local.mkdir(exist_ok=True)
key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, 'Mobile Lab Local Development CA')])
now = datetime.now(timezone.utc)
ca = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
      .serial_number(x509.random_serial_number()).not_valid_before(now - timedelta(days=1))
      .not_valid_after(now + timedelta(days=365))
      .add_extension(x509.BasicConstraints(ca=True, path_length=0), critical=True)
      .add_extension(x509.KeyUsage(False, False, False, False, False, True, True, False, False), critical=True)
      .sign(key, hashes.SHA256()))
server_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
server_name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, 'localhost')])
server = (x509.CertificateBuilder().subject_name(server_name).issuer_name(name)
          .public_key(server_key.public_key()).serial_number(x509.random_serial_number())
          .not_valid_before(now - timedelta(days=1)).not_valid_after(now + timedelta(days=365))
          .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
          .add_extension(x509.SubjectAlternativeName([x509.DNSName('localhost'),
              x509.IPAddress(ipaddress.ip_address('127.0.0.1'))]), critical=False)
          .add_extension(x509.ExtendedKeyUsage([ExtendedKeyUsageOID.SERVER_AUTH]), critical=False)
          .sign(key, hashes.SHA256()))
def write_key(path, value):
    path.write_bytes(value.private_bytes(serialization.Encoding.PEM,
        serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
write_key(local / 'server-key.pem', server_key)
(local / 'server.pem').write_bytes(server.public_bytes(serialization.Encoding.PEM))
ca_bytes = ca.public_bytes(serialization.Encoding.PEM)
(local / 'ca.pem').write_bytes(ca_bytes)
target = root.parent / 'app/src/debug/res/raw/lab_ca.pem'
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(ca_bytes)
# The CA private key is deliberately discarded; rerun to create a new local identity.
print('Created local certificate and debug trust anchor. Rebuild app:assembleDebug.')
