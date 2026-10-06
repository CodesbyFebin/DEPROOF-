"""Executes the exact packaged Room migration against real SQLite and exported schemas.
Room-on-device/process lifecycle remains a separate NOT_RUN gate.
"""
import json,pathlib,sqlite3,tempfile,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
class MigrationTest(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.path=pathlib.Path(self.tmp.name)/'records.db'
  self.db=sqlite3.connect(self.path);self.db.execute('PRAGMA foreign_keys=ON')
  schema=json.loads((ROOT/'app/schemas/com.example.data.DeproofDatabase/1.json').read_text())['database']
  for entity in schema['entities']:
   self.db.execute(entity['createSql'].replace('${TABLE_NAME}',entity['tableName']))
   for index in entity.get('indices',[]):self.db.execute(index['createSql'].replace('${TABLE_NAME}',entity['tableName']))
  self.db.execute("INSERT INTO events VALUES ('receipt','2026-10-06T00:00:00Z','REJECTED','exact signed legacy bytes preserved')")
  self.db.execute("INSERT INTO tasks VALUES ('task','Existing task','Note\nUnicode 😀','[]','2026-10-06T00:00:00Z')")
  self.db.execute("INSERT INTO attachments VALUES ('file','task','digest','image/jpeg','99','capture','2026-10-06T00:00:00Z')")
  self.db.commit()
 def tearDown(self):self.db.close();self.tmp.cleanup()
 def migration(self):
  return [x.strip() for x in (ROOT/'app/src/main/resources/db/migration_1_2.sql').read_text().split(';') if x.strip()]
 def test_upgrade_preserves_bytes_and_matches_exported_room_schema(self):
  with self.db:
   for sql in self.migration():self.db.execute(sql)
  self.assertEqual(self.db.execute('SELECT payload FROM events').fetchone()[0],'exact signed legacy bytes preserved')
  self.assertEqual(self.db.execute('SELECT note FROM tasks').fetchone()[0],'Note\nUnicode 😀')
  self.assertEqual(self.db.execute('SELECT taskId FROM attachments').fetchone()[0],'task')
  schema=json.loads((ROOT/'app/schemas/com.example.data.DeproofDatabase/2.json').read_text())['database']
  for entity in schema['entities']:
   columns={row[1]:row for row in self.db.execute('PRAGMA table_info('+entity['tableName']+')')}
   self.assertEqual(set(columns),{f['columnName'] for f in entity['fields']})
   for field in entity['fields']:
    row=columns[field['columnName']];self.assertEqual(row[2],field['affinity']);self.assertEqual(bool(row[3]),field.get('notNull',False))
  self.assertEqual(list(self.db.execute('PRAGMA foreign_key_check')),[])
  self.db.execute("INSERT INTO operations VALUES ('op','NODE_transfer','peer','RUNNING','exact signed payload',NULL,'now')");self.db.commit();self.db.close()
  self.db=sqlite3.connect(self.path)
  self.assertEqual(self.db.execute("SELECT payload FROM operations WHERE id='op'").fetchone()[0],'exact signed payload')
  with self.assertRaises(sqlite3.IntegrityError):self.db.execute("INSERT INTO operations VALUES ('op','NODE_transfer','peer','RUNNING','duplicate',NULL,'now')")
 def test_interrupted_migration_rolls_back_without_destroying_old_records(self):
  self.db.execute('BEGIN')
  self.db.execute(self.migration()[0]);self.db.rollback()
  self.assertIsNone(self.db.execute("SELECT name FROM sqlite_master WHERE name='node_sessions'").fetchone())
  self.assertEqual(self.db.execute('SELECT COUNT(*) FROM events').fetchone()[0],1)
 def test_new_private_workflow_drafts_survive_reopen_and_replace_atomically(self):
  with self.db:
   for sql in self.migration():self.db.execute(sql)
  mapping='{"schema":"deproof-mapping-plan-v1","points":[{"label":"Private|😀","latitude":"10.5","longitude":"76.25"}]}'
  reminder='{"schema":"deproof-local-reminder-v1","generation":"old","state":"PENDING"}'
  self.db.execute('INSERT INTO drafts VALUES (?,?,?)',('mapping:task',mapping,'now'));self.db.execute('INSERT INTO drafts VALUES (?,?,?)',('reminder:task',reminder,'now'));self.db.commit()
  self.db.execute('BEGIN');self.db.execute('INSERT OR REPLACE INTO drafts VALUES (?,?,?)',('reminder:task','interrupted replacement','later'));self.db.rollback();self.db.close();self.db=sqlite3.connect(self.path)
  self.assertEqual(self.db.execute('SELECT payload FROM drafts WHERE id=?',('mapping:task',)).fetchone()[0],mapping)
  self.assertEqual(self.db.execute('SELECT payload FROM drafts WHERE id=?',('reminder:task',)).fetchone()[0],reminder)
  self.assertEqual(self.db.execute('SELECT payload FROM events').fetchone()[0],'exact signed legacy bytes preserved')
