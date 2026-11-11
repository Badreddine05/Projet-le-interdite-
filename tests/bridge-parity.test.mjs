import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {reset,command,state} from '../web/generated/engine.js';

const root=fileURLToPath(new URL('..',import.meta.url));
test('TeaVM browser engine matches every JVM state in deterministic reference traces',()=>{
  const java=process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin',process.platform==='win32'?'java.exe':'java'):'java';
  const classpath=['target/classes','target/test-classes'].map(p=>path.join(root,p)).join(path.delimiter);
  const reference=spawnSync(java,['-Dfile.encoding=UTF-8','-cp',classpath,'fr.badreddine.island.ParityTrace'],{encoding:'utf8',maxBuffer:20*1024*1024,cwd:root});
  assert.equal(reference.status,0,reference.stderr||reference.error?.message);
  const trace=reference.stdout.trim().split(/\r?\n/).map(line=>JSON.parse(line));
  assert.ok(trace.length>100,'Reference must exercise many transitions');
  for(const [i,entry] of trace.entries()){
    const actual=JSON.parse(entry.reset?reset(...entry.reset):command(entry.command));
    assert.deepEqual(actual,entry.state,`State ${i} after ${entry.command??'reset'}`);
    assert.deepEqual(JSON.parse(state()),actual,'state() must be read-only');
  }
});
