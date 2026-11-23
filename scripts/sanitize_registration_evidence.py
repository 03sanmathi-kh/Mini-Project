"""Redact transient credentials from diagnostic text without changing test outcomes."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]/'FINAL_GATE3_DELIVERABLES/evidence'
for p in root.glob('*.log'):
 original=p.read_text(encoding='utf-8',errors='replace');s=original
 s=re.sub(r'("(?:activationSecret|trackingCode|newPassword|confirmPassword|password)"\s*:\s*")[^"]*(")',r'\1[REDACTED]\2',s,flags=re.I)
 s=re.sub(r'PWREG-[A-Za-z0-9_-]{32}','PWREG-[REDACTED]',s)
 s=re.sub(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+','[JWT REDACTED]',s)
 s=re.sub(r'(XSRF-TOKEN[=:])[^;,\s\]]+',r'\1[REDACTED]',s)
 if s!=original:p.write_text(s,encoding='utf-8')
print('Diagnostic credential values redacted; assertion and result lines retained.')
