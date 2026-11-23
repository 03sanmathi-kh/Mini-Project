"""Validate the supplied ZIP into an isolated SQLite reference store; never connect to operational DB.

Usage: python scripts/stage_reference.py --archive <zip> [--output reference_data]
No credentials, password hashes, names, emails, addresses or document paths are imported.
Ambiguous roles/statuses and inconsistent parent relationships are quarantined.
"""
import argparse, collections, csv, hashlib, io, json, sqlite3, zipfile, re
from datetime import date
from decimal import Decimal, InvalidOperation
from pathlib import Path

SCHEMAS = {
 'User':'UserID Name PhoneNo Address Email PasswordHash',
 'Officer':'OfficerID Name OfficerRole PhoneNo Email PasswordHash',
 'Property':'PropertyID StreetNumber StreetDirection StreetName CommunityArea Ward ZipCode Latitude Longitude',
 'Application':'ApplicationID UserID PropertyID PermitNo ApplicationType ApplicationStatus ApplicationDate IssueDate ProcessingTime ReportedCost TotalFee ReviewType WorkType WorkDescription PermitMilestone ResultDescription DecisionReason',
 'Document':'DocumentID ApplicationID DocumentName FilePath VersionNo UploadDate UploadedBy',
 'Assignment':'AssignmentID ApplicationID OfficerID Stage AssignedAt IsOverride',
 'Correction':'CorrectionID ApplicationID OfficerID CorrectionStatus CorrectionDescription RequestedDate ResolvedDate',
 'Payment':'PaymentID ApplicationID UserID TransactionID Amount PaymentDate PaymentStatus',
 'Inspection':'InspectionID ApplicationID InspectorID InspectionDate Result Remarks ReinspectionRequired',
}
LINKS={'Application':[('UserID','User'),('PropertyID','Property')], 'Document':[('ApplicationID','Application'),('UploadedBy','User')], 'Assignment':[('ApplicationID','Application'),('OfficerID','Officer')], 'Correction':[('ApplicationID','Application'),('OfficerID','Officer')], 'Payment':[('ApplicationID','Application'),('UserID','User')], 'Inspection':[('ApplicationID','Application'),('InspectorID','Officer')]}
MAPS={
 'OfficerRole':{'Permit Reviewer':'PERMIT_REVIEWER','Senior Officer':'SENIOR_DEPARTMENT_OFFICER','Building Inspector':'BUILDING_INSPECTOR','Zoning Officer':'ZONING_OFFICER'},
 'ApplicationStatus':{'Rejected':'REJECTED','Approved':'APPROVED','Submitted':'SUBMITTED','Correction Required':'CORRECTION_REQUIRED','Under Review':'REFERENCE_UNDER_REVIEW','Inspection Pending':'REFERENCE_INSPECTION_PENDING'},
 'PaymentStatus':{'Failed':'REFERENCE_FAILED','Pending':'PENDING','Paid':'PAID'},
 'Result':{'Pending':'REFERENCE_PENDING','Pass':'PASSED','Fail':'FAILED','Could Not Complete':'COULD_NOT_COMPLETE'},
 'Stage':{'Initial Review':'REFERENCE_INITIAL_REVIEW','Zoning Review':'ZONING_OFFICER','Building Review':'REFERENCE_BUILDING_REVIEW','Final Review':'REFERENCE_FINAL_REVIEW','Inspection':'BUILDING_INSPECTOR'},
}
# REFERENCE_* categories deliberately do not assert a precise operational state/stage.
SAFE={'ApplicationType','ApplicationStatus','ApplicationDate','IssueDate','ProcessingTime','ReportedCost','TotalFee','PaymentStatus','Amount','Result','OfficerRole','Stage','IsOverride','Ward','CommunityArea','VersionNo'}

def validate(archive, output):
 output=Path(output);output.mkdir(parents=True,exist_ok=True)
 tables={};schema_errors=[]
 with zipfile.ZipFile(archive) as z:
  if sum(i.file_size for i in z.infolist())>100_000_000: raise ValueError('Archive exceeds 100 MB uncompressed limit')
  for name,fields in SCHEMAS.items():
   filename=name+'.csv'
   if filename not in z.namelist(): schema_errors.append(filename+' missing');continue
   reader=csv.DictReader(io.TextIOWrapper(z.open(filename),encoding='utf-8-sig'))
   if reader.fieldnames!=fields.split(): schema_errors.append(filename+' header mismatch');continue
   tables[name]=list(reader)
 if schema_errors: raise ValueError('; '.join(schema_errors))
 indexes={n:{r[n+'ID'] for r in rows} for n,rows in tables.items()}
 counts={};issues=[];accepted=[];status_by_key={}
 for name,rows in tables.items():
  c=collections.Counter();seen=set()
  for row in rows:
   key=row[name+'ID'];bad=[];quarantine=[]
   if key in seen: bad.append('duplicate primary key')
   seen.add(key)
   for field,value in row.items():
    try:
     if field.endswith('ID') and field!='TransactionID':
      if not re.fullmatch(r'[A-Z]{2,5}[0-9]{4,12}',value): raise ValueError()
     if field.endswith('Date') or field=='AssignedAt':
      if value: date.fromisoformat(value[:10])
     if field in {'Amount','ReportedCost','TotalFee','ProcessingTime','VersionNo'}:
      v=Decimal(value)
      if not v.is_finite() or v<0 or field=='VersionNo' and v<1: raise ValueError()
      if field in {'ProcessingTime','VersionNo'} and v!=v.to_integral_value(): raise ValueError()
     if field in {'Latitude','Longitude'}:
      v=Decimal(value);limit=90 if field=='Latitude' else 180
      if not v.is_finite() or abs(v)>limit: raise ValueError()
     if field in {'IsOverride','ReinspectionRequired'} and value not in {'True','False'}: raise ValueError()
    except (ValueError,InvalidOperation): bad.append('invalid '+field)
    if field in MAPS and value not in MAPS[field]: quarantine.append('unresolved '+field)
   for field,parent in LINKS.get(name,[]):
    if row[field] not in indexes[parent]:bad.append('missing parent '+field)
   for later,earlier in [('IssueDate','ApplicationDate'),('ResolvedDate','RequestedDate')]:
    if row.get(later) and row.get(earlier) and row[later]<row[earlier]:quarantine.append(later+' precedes '+earlier)
   result='rejected' if bad else 'quarantined' if quarantine else 'imported'
   c[result]+=1;status_by_key[(name,key)]=result
   if result!='imported':issues.append({'table':name,'id':key,'status':result,'reasons':bad+quarantine})
   payload={k:MAPS.get(k,{}).get(v,v) for k,v in row.items() if k in SAFE}
   accepted.append([name,key,result,payload])
  counts[name]={'total':len(rows),**{k:c[k] for k in ['imported','rejected','quarantined']}}
 # Do not let apparently valid child records silently depend on rejected/quarantined parents.
 lookup={n:{r[n+'ID']:r for r in rows} for n,rows in tables.items()}
 changed=True
 while changed:
  changed=False
  for entry in accepted:
   name,key,result,payload=entry
   if status_by_key[(name,key)]!='imported':continue
   reasons=['parent not imported: '+field for field,parent in LINKS.get(name,[]) if status_by_key.get((parent,lookup[name][key][field]))!='imported']
   if reasons:
    status_by_key[(name,key)]='quarantined';entry[2]='quarantined';counts[name]['imported']-=1;counts[name]['quarantined']+=1;issues.append({'table':name,'id':key,'status':'quarantined','reasons':reasons});changed=True
 db=sqlite3.connect(output/'reference.sqlite')
 with db:
  db.execute('CREATE TABLE IF NOT EXISTS reference_rows (source_table TEXT, source_id TEXT, status TEXT, payload TEXT, PRIMARY KEY(source_table,source_id))')
  db.execute('DELETE FROM reference_rows')  # Only this isolated, reproducible reference cache.
  db.executemany('INSERT INTO reference_rows VALUES (?,?,?,?)',[(n,k,s,json.dumps(p)) for n,k,s,p in accepted if s!='rejected'])
 # Analytics queries use SQLite read-only mode after the import transaction closes.
 db.close();read=sqlite3.connect((output/'reference.sqlite').resolve().as_uri()+'?mode=ro',uri=True)
 grouped={}
 for n,field in [('Application','ApplicationStatus'),('Application','ApplicationType'),('Payment','PaymentStatus'),('Inspection','Result')]:
  grouped[n+'.'+field]=dict(read.execute('SELECT json_extract(payload,?),count(*) FROM reference_rows WHERE source_table=? AND status=? GROUP BY json_extract(payload,?)',('$.'+field,n,'imported','$.'+field)).fetchall())
 read.close()
 summary={'archiveSha256':hashlib.sha256(Path(archive).read_bytes()).hexdigest(),'provenance':'Unverified supplied reference archive; not certified official Chicago data','operationalImport':False,'credentialImport':False,'counts':counts,'analytics':grouped,'mappingRules':MAPS,'limitations':['REFERENCE_* categories retain ambiguity rather than guessing workflow state.','Inspection Officer is unresolved and quarantined.','No referenced document binaries are supplied or fabricated.','Counts describe reference rows, not operational users or permits.']}
 (output/'summary.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
 (output/'invalid_records.json').write_text(json.dumps(issues,indent=2),encoding='utf-8')
 print(json.dumps({'counts':counts,'issues':len(issues),'archiveSha256':summary['archiveSha256']},indent=2))
 return summary

if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--archive',required=True,type=Path);parser.add_argument('--output',default='reference_data',type=Path);args=parser.parse_args();validate(args.archive,args.output)
