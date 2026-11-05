#!/usr/bin/env python3
"""Generate public update metadata from a built, signed APK. No secrets enter the manifest."""
import argparse, hashlib, json, pathlib, re
p=argparse.ArgumentParser();p.add_argument('--apk',required=True,type=pathlib.Path);p.add_argument('--version-code',required=True,type=int);p.add_argument('--version-name',required=True);p.add_argument('--repository',required=True);p.add_argument('--tag',required=True);p.add_argument('--output',required=True,type=pathlib.Path);a=p.parse_args()
if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+',a.repository) or not re.fullmatch(r'[A-Za-z0-9_.-]+',a.tag):p.error('Invalid repository or tag')
if a.version_code<1:p.error('version-code must be positive')
result={'packageName':'dev.farhan.freq','versionCode':a.version_code,'versionName':a.version_name,'sha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'apkUrl':f'https://github.com/{a.repository}/releases/download/{a.tag}/{a.apk.name}'}
a.output.write_text(json.dumps(result,indent=2)+'\n')
