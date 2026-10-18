const fs=require('node:fs'),path=require('node:path');
const root=path.join(__dirname,'stubs');
const files={
 'kotlin/jvm/functions/Function1.java':'package kotlin.jvm.functions; public interface Function1 { Object invoke(Object value); }',
 'kotlin/coroutines/Continuation.java':'package kotlin.coroutines; public interface Continuation {}',
 'com/lagradost/cloudstream3/metaproviders/TmdbProvider.java':'package com.lagradost.cloudstream3.metaproviders; public class TmdbProvider { public TmdbProvider() {} public Object loadLinks(String d, boolean c, kotlin.jvm.functions.Function1 s, kotlin.jvm.functions.Function1 f, kotlin.coroutines.Continuation k) { return null; } }'
};
for(const [file,text] of Object.entries(files)){fs.mkdirSync(path.dirname(path.join(root,file)),{recursive:true});fs.writeFileSync(path.join(root,file),text+'\n');}
console.log('Compile-only signature stubs created. None may be bundled in the extension.');
