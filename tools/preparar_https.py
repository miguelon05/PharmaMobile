"""Genera certificados locales para Spring Boot y la app Android debug.

Uso: python tools/preparar_https.py <carpeta-backend>
Requiere cryptography. No instala certificados en el sistema operativo.
Las claves privadas se guardan en .local/https (ignorado por Git).
"""
import ipaddress
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID, ExtendedKeyUsageOID

root = Path(__file__).resolve().parent.parent
backend = Path(sys.argv[1]).resolve()
assert (backend / "pom.xml").is_file(), "No se encontro el backend Maven"
folder = backend / ".local" / "https"
folder.mkdir(parents=True, exist_ok=True)
ignore = backend / ".gitignore"
if ".local/" not in ignore.read_text(encoding="utf-8"):
    with ignore.open("a", encoding="utf-8") as file:
        file.write("\n# Certificados privados de desarrollo\n.local/\n")
now = datetime.now(timezone.utc)
if not (folder / "server-key.pem").exists():
    ca_key = rsa.generate_private_key(public_exponent=65537, key_size=3072)
    ca_name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "PharmaMobil desarrollo local CA")])
    ca = (x509.CertificateBuilder().subject_name(ca_name).issuer_name(ca_name)
          .public_key(ca_key.public_key()).serial_number(x509.random_serial_number())
          .not_valid_before(now - timedelta(minutes=5)).not_valid_after(now + timedelta(days=365))
          .add_extension(x509.BasicConstraints(ca=True, path_length=0), critical=True)
          .add_extension(x509.KeyUsage(False, False, False, False, False, True, True, False, False), critical=True)
          .sign(ca_key, hashes.SHA256()))
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    cert = (x509.CertificateBuilder()
            .subject_name(x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "localhost")]))
            .issuer_name(ca_name).public_key(key.public_key()).serial_number(x509.random_serial_number())
            .not_valid_before(now - timedelta(minutes=5)).not_valid_after(now + timedelta(days=180))
            .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
            .add_extension(x509.SubjectAlternativeName([
                x509.DNSName("localhost"), x509.IPAddress(ipaddress.ip_address("127.0.0.1")),
                x509.IPAddress(ipaddress.ip_address("10.0.2.2"))]), critical=False)
            .add_extension(x509.ExtendedKeyUsage([ExtendedKeyUsageOID.SERVER_AUTH]), critical=False)
            .add_extension(x509.KeyUsage(True, False, True, False, False, False, False, False, False), critical=True)
            .sign(ca_key, hashes.SHA256()))
    (folder / "ca.pem").write_bytes(ca.public_bytes(serialization.Encoding.PEM))
    (folder / "server.pem").write_bytes(cert.public_bytes(serialization.Encoding.PEM) + ca.public_bytes(serialization.Encoding.PEM))
    (folder / "server-key.pem").write_bytes(key.private_bytes(
        serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
    # La clave de la CA no se conserva: no hace falta emitir otros certificados.
raw = root / "androidApp/src/debug/res/raw"
raw.mkdir(parents=True, exist_ok=True)
(raw / "pharma_dev_ca.pem").write_bytes((folder / "ca.pem").read_bytes())
profile = backend / "src/main/resources/application-https.yaml"
desired = '''# Activar junto con dev: --spring.profiles.active=dev,https
server:
  address: 127.0.0.1
  port: 8443
  ssl:
    enabled: true
    certificate: file:.local/https/server.pem
    certificate-private-key: file:.local/https/server-key.pem
    enabled-protocols: TLSv1.2,TLSv1.3
spring:
  jpa:
    hibernate:
      ddl-auto: validate
'''
if profile.exists() and profile.read_text(encoding="utf-8") != desired:
    raise RuntimeError("El perfil HTTPS ya existe y es diferente; revisarlo antes de cambiarlo")
profile.write_text(desired, encoding="utf-8")
print("Certificado local preparado. Perfil: dev,https. Puerto: 8443. Claves privadas ignoradas por Git.")
