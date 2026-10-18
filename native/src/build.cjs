const fs=require('node:fs'),path=require('node:path'),cp=require('node:child_process'),crypto=require('node:crypto');
process.chdir(__dirname);
const bin=process.argv[2]||'C:/Program Files/Android/Android Studio/jbr/bin';
const run=(exe,args)=>cp.execFileSync(path.join(bin,exe+'.exe'),args,{stdio:'inherit'});
require('./make-compile-stubs.cjs');
run('javac',['--release','8','-Xlint:-options','-cp','json.jar;jsoup.jar','BysePow.java','ByseClient.java','NativeCatalog.java','NativeMetadata.java','stubs/kotlin/jvm/functions/Function1.java','stubs/kotlin/coroutines/Continuation.java','stubs/com/lagradost/cloudstream3/metaproviders/TmdbProvider.java','ByseNativeProvider.java','ByseNativePlugin.java']);
// Explicit allowlist excludes compile stubs, dependencies and test entry points.
run('java',['-cp','r8.jar','com.android.tools.r8.D8','--min-api','24','--output','RoDubbedByseNative.cs3.zip','BysePow.class','ByseClient.class','NativeCatalog.class','NativeCatalog$URLPath.class','NativeMetadata.class','ByseNativeProvider.class','ByseNativePlugin.class']);
run('jar',['--update','--file','RoDubbedByseNative.cs3.zip','manifest.json']);
fs.copyFileSync('RoDubbedByseNative.cs3.zip','RoDubbedByseNative.cs3');
console.log('Built extension SHA256',crypto.createHash('sha256').update(fs.readFileSync('RoDubbedByseNative.cs3')).digest('hex'));
