const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
(async()=>{for(const {file,url,sha256} of require('./compile-dependencies.json')){
 const r=await fetch(url);if(!r.ok)throw Error(file+' HTTP '+r.status);
 const bytes=Buffer.from(await r.arrayBuffer());
 if(crypto.createHash('sha256').update(bytes).digest('hex')!==sha256)throw Error(file+' checksum mismatch');
 fs.writeFileSync(path.join(__dirname,file),bytes);console.log('Verified',file);
}})().catch(e=>{console.error(e.message);process.exitCode=1});
