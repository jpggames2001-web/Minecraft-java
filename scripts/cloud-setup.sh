#!/usr/bin/env bash
# Debian cloud bootstrap. No game artwork, font, audio, or model files are downloaded.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
tools_dir=$(dirname -- "$project_dir")/.minecraft-tools
mkdir -p "$tools_dir/downloads" "$tools_dir/root"
if [[ ! -x "$tools_dir/jdk/bin/javac" || ! -x "$tools_dir/root/usr/bin/Xvfb" ]]; then
  [[ -f /usr/share/keyrings/debian-archive-keyring.gpg ]] || { echo 'Use a Java 21 JDK and Maven on non-Debian hosts.' >&2; exit 1; }
  mkdir -p "$tools_dir/apt/etc/apt.conf.d" "$tools_dir/apt/etc/sources.list.d" "$tools_dir/apt/etc/preferences.d" "$tools_dir/apt/state/lists/partial" "$tools_dir/apt/cache/archives/partial"
  cat > "$tools_dir/apt/etc/sources.list" <<'SOURCES'
deb [signed-by=/usr/share/keyrings/debian-archive-keyring.gpg] https://deb.debian.org/debian trixie main
deb [signed-by=/usr/share/keyrings/debian-archive-keyring.gpg] https://security.debian.org/debian-security trixie-security main
SOURCES
  cat > "$tools_dir/apt/config" <<CONFIG
Dir::Etc "$tools_dir/apt/etc";
Dir::Etc::sourcelist "sources.list";
Dir::Etc::sourceparts "sources.list.d";
Dir::State "$tools_dir/apt/state";
Dir::State::status "/var/lib/dpkg/status";
Dir::Cache "$tools_dir/apt/cache";
Debug::NoLocking "true";
APT::Sandbox::User "$(id -un)";
CONFIG
  /usr/bin/apt-get -c "$tools_dir/apt/config" update
  /usr/bin/apt-get -c "$tools_dir/apt/config" --download-only --no-install-recommends install -y openjdk-21-jdk-headless xvfb
  for package_file in "$tools_dir"/apt/cache/archives/*.deb; do dpkg-deb -x "$package_file" "$tools_dir/root"; done
  if [[ ! -d "$tools_dir/jdk" ]]; then
    if [[ -d /usr/lib/jvm/java-21-openjdk-amd64 ]]; then cp -a /usr/lib/jvm/java-21-openjdk-amd64 "$tools_dir/jdk";
    else mkdir -p "$tools_dir/jdk"; fi
  fi
  cp -a "$tools_dir/root/usr/lib/jvm/java-21-openjdk-amd64/." "$tools_dir/jdk/"
fi
if [[ ! -x "$tools_dir/apache-maven-3.9.9/bin/mvn" ]]; then
  curl --fail --location --retry 2 https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.tar.gz -o "$tools_dir/downloads/maven.tar.gz"
  curl --fail --location --retry 2 https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.tar.gz.sha512 -o "$tools_dir/downloads/maven.sha512"
  python3 - "$tools_dir" <<'PY'
import hashlib, pathlib, sys
p = pathlib.Path(sys.argv[1]) / 'downloads'
if hashlib.sha512((p/'maven.tar.gz').read_bytes()).hexdigest() != (p/'maven.sha512').read_text().split()[0]:
    raise SystemExit('Maven checksum verification failed')
PY
  tar -xzf "$tools_dir/downloads/maven.tar.gz" -C "$tools_dir"
fi
# Maven does not automatically honor the platform's HTTPS_PROXY variable.
python3 - "$tools_dir" <<'PY'
import os, pathlib, sys, urllib.parse, xml.etree.ElementTree as E
p = pathlib.Path(sys.argv[1]) / 'maven-settings.xml'
proxy = os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy')
if proxy:
    u = urllib.parse.urlsplit(proxy)
    if u.username or u.password:
        raise SystemExit('Authenticated proxy needs supported credentials; no credentials are copied into settings')
    settings = E.Element('settings')
    item = E.SubElement(E.SubElement(settings, 'proxies'), 'proxy')
    for k, v in {'id':'cloud-egress','active':'true','protocol':'http','host':u.hostname,'port':str(u.port or 80),'nonProxyHosts':'localhost|127.0.0.1'}.items():
        E.SubElement(item, k).text = v
    E.ElementTree(settings).write(p, encoding='utf-8', xml_declaration=True)
else:
    p.write_text('<settings/>\n')
PY
cd "$project_dir"
bash scripts/maven.sh -B verify
