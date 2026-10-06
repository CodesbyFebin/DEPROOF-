#!/usr/bin/env python3
"""Run the same JVM tests without Gradle when its daemon is sandbox-blocked.
Uses locally cached pinned jars; does not claim Android build qualification.
"""
import pathlib, subprocess, os, sys
root=pathlib.Path(__file__).resolve().parents[1]
cache=pathlib.Path(os.environ.get('DEPROOF_MAVEN_CACHE',str(pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1')))
def jar(group,artifact,version):
    matches=list((cache/group/artifact/version).glob('*/'+artifact+'-'+version+'.jar'))
    if len(matches)!=1: raise SystemExit('BLOCKED: missing pinned jar '+artifact+':'+version)
    return str(matches[0])
compiler=[jar('org.jetbrains.kotlin','kotlin-compiler-embeddable','2.4.20'),jar('org.jetbrains.kotlin','kotlin-stdlib','2.4.20'),jar('org.jetbrains.kotlin','kotlin-script-runtime','2.4.20'),jar('org.jetbrains.kotlin','kotlin-reflect','1.6.10'),jar('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.11.0'),jar('org.jetbrains','annotations','13.0')]
cp=[jar('org.bouncycastle','bcprov-jdk18on','1.79'),jar('org.jetbrains.kotlin','kotlin-stdlib','2.4.20'),jar('com.fasterxml.jackson.core','jackson-core','2.21.1'),jar('com.fasterxml.jackson.core','jackson-databind','2.21.1'),jar('com.fasterxml.jackson.core','jackson-annotations','2.21'),jar('junit','junit','4.13.2'),jar('org.hamcrest','hamcrest-core','1.3')]
out=root/'evidence/qualification/jvm-classes';out.mkdir(parents=True,exist_ok=True)
sources=list((root/'app/src/main/java/com/example/domain').glob('*.kt'))+[root/'app/src/main/java/com/example/data/Rpc.kt',root/'app/src/main/java/com/example/data/NodeClient.kt']+list((root/'tools/jvm').glob('*.kt'))+list((root/'app/src/test/java/com/example/domain').glob('*.kt'))
subprocess.run(['java','-cp',os.pathsep.join(compiler),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','17','-classpath',os.pathsep.join(cp),'-d',str(out),*map(str,sources)],check=True,cwd=root)
tests=sorted('com.example.domain.'+p.stem for p in (root/'app/src/test/java/com/example/domain').glob('*Test.kt'))
subprocess.run(['java','-cp',os.pathsep.join([str(out),*cp]),'org.junit.runner.JUnitCore',*tests],check=True,cwd=root)
(root/'evidence/qualification/jvm-classpath.txt').write_text(os.pathsep.join([str(out),*cp]))
