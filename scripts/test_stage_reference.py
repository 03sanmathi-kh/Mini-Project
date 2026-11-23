import contextlib,csv,hashlib,io,json,sqlite3,tempfile,unittest,zipfile
from pathlib import Path
from stage_reference import SCHEMAS,validate

class StagingTests(unittest.TestCase):
 def archive(self,folder,change=None):
  ids={'User':'USR0001','Officer':'OFC0001','Property':'PROP0001','Application':'APP0001','Document':'DOC0001','Assignment':'ASG0001','Correction':'COR0001','Payment':'PAY0001','Inspection':'INS0001'}
  rows={}
  for name,fields in SCHEMAS.items():
   r={f:'' for f in fields.split()};r[name+'ID']=ids[name]
   for k in r:
    if k.endswith('ID') and k!='TransactionID':r[k]=ids.get(k[:-2],'OFC0001')
    if k.endswith('Date') or k=='AssignedAt':r[k]='2026-01-01'
   r.update({k:'1' for k in ['Amount','ReportedCost','TotalFee','ProcessingTime','VersionNo','Latitude','Longitude'] if k in r})
   for k,v in {'UploadedBy':ids['User'],'InspectorID':ids['Officer'],'OfficerRole':'Permit Reviewer','ApplicationStatus':'Submitted','PaymentStatus':'Pending','Result':'Pending','Stage':'Initial Review','IsOverride':'False','ReinspectionRequired':'False'}.items():
    if k in r:r[k]=v
   rows[name]=r
  if change:change(rows)
  archive=folder/'fixture.zip'
  with zipfile.ZipFile(archive,'w') as z:
   for name,row in rows.items():
    text=io.StringIO();w=csv.DictWriter(text,fieldnames=SCHEMAS[name].split());w.writeheader();w.writerow(row);z.writestr(name+'.csv',text.getvalue())
  return archive
 def run_stage(self,folder,change=None):
  archive=self.archive(folder,change);before=hashlib.sha256(archive.read_bytes()).hexdigest()
  with contextlib.redirect_stdout(io.StringIO()):summary=validate(archive,folder/'output')
  self.assertEqual(before,hashlib.sha256(archive.read_bytes()).hexdigest());return summary
 def test_valid_reference_data_excludes_credentials_and_is_read_only(self):
  with tempfile.TemporaryDirectory() as t:
   p=Path(t);s=self.run_stage(p);self.assertTrue(all(c['imported']==1 for c in s['counts'].values()))
   db=sqlite3.connect((p/'output/reference.sqlite').as_uri()+'?mode=ro',uri=True)
   self.assertNotIn('PasswordHash',' '.join(r[0] for r in db.execute('SELECT payload FROM reference_rows')))
   with self.assertRaises(sqlite3.OperationalError):db.execute('DELETE FROM reference_rows')
   db.close()
 def test_invalid_amount_and_orphan_are_rejected(self):
  with tempfile.TemporaryDirectory() as t:
   def invalid(rows):rows['Payment']['Amount']='NaN';rows['Document']['ApplicationID']='APP9999'
   s=self.run_stage(Path(t),invalid);self.assertEqual(1,s['counts']['Payment']['rejected']);self.assertEqual(1,s['counts']['Document']['rejected'])
 def test_ambiguous_officer_and_dependents_are_quarantined(self):
  with tempfile.TemporaryDirectory() as t:
   s=self.run_stage(Path(t),lambda rows:rows['Officer'].update(OfficerRole='Inspection Officer'))
   self.assertEqual(1,s['counts']['Officer']['quarantined']);self.assertEqual(1,s['counts']['Assignment']['quarantined']);self.assertEqual(1,s['counts']['Inspection']['quarantined'])
if __name__=='__main__':unittest.main()
