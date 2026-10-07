#!/usr/bin/env python3
import json,pathlib,subprocess,sys
root=pathlib.Path(__file__).resolve().parents[1]
cp=(root/'evidence/qualification/jvm-classpath.txt').read_text()
subprocess.run(['java','-cp',cp,'com.example.probe.LiveRpcProbeKt',str(root/'evidence/qualification/live-rpc.json')],check=True)
result=json.loads((root/'evidence/qualification/live-rpc.json').read_text())
sys.exit(0 if all(x['availability']=='AVAILABLE' for x in result['results']) else 1)
