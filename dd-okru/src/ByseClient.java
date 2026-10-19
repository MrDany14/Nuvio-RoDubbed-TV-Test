import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.math.BigInteger;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Fresh implementation of the public player's ordinary attestation/PoW/playback flow.
 * No JavaScript resolver is executed. Does not skip the host's required checks.
 */
public final class ByseClient {
    public static final String USER_AGENT="Mozilla/5.0 (Linux; Android 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36";
    private final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    private long deadline;
    private String stage="initialization";
    public long powMillis, totalMillis, attempts;
    public String frameUrl;
    private static final String B64="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    static String encode(byte[] bytes) {
        StringBuilder out=new StringBuilder();
        for(int i=0;i<bytes.length;i+=3) {
            int n=(bytes[i]&255)<<16;
            if(i+1<bytes.length)n|=(bytes[i+1]&255)<<8;
            if(i+2<bytes.length)n|=bytes[i+2]&255;
            out.append(B64.charAt(n>>>18)).append(B64.charAt((n>>>12)&63));
            if(i+1<bytes.length)out.append(B64.charAt((n>>>6)&63));
            if(i+2<bytes.length)out.append(B64.charAt(n&63));
        }
        return out.toString().replace('+','-').replace('/','_');
    }
    static byte[] decode(String text) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        int buffer=0,bits=0;
        for(char c:text.replace('-','+').replace('_','/').toCharArray()) {
            if(c=='=')break;
            int value=B64.indexOf(c);if(value<0)throw new IllegalArgumentException("Invalid base64");
            buffer=(buffer<<6)|value;bits+=6;
            if(bits>=8){bits-=8;out.write((buffer>>>bits)&255);}
        }
        return out.toByteArray();
    }
    static String origin(String url) throws Exception {
        URL u=new URL(url);
        if(!u.getProtocol().equals("https"))throw new IOException("HTTPS required");
        return u.getProtocol()+"://"+u.getAuthority();
    }
    private void checkDeadline() throws IOException {
        NativeHttp.session().check();
        if(Thread.currentThread().isInterrupted() || System.nanoTime()>deadline)throw new IOException("Lookup deadline reached at "+stage);
    }
    JSONObject request(String url,JSONObject body,Map<String,String> headers) throws Exception {
        checkDeadline();
        URL target=new URL(url);origin(url);
        HttpURLConnection conn=(HttpURLConnection)target.openConnection();
        NativeHttp.attach(conn);
        int remaining=(int)Math.max(1,Math.min(15000,(deadline-System.nanoTime())/1000000));
        conn.setConnectTimeout(remaining);conn.setReadTimeout(remaining);conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent",USER_AGENT);conn.setRequestProperty("Accept","application/json");
        for(Map.Entry<String,String> h:headers.entrySet())conn.setRequestProperty(h.getKey(),h.getValue());
        for(Map.Entry<String,List<String>> h:cookies.get(target.toURI(),Collections.<String,List<String>>emptyMap()).entrySet()) {
            StringBuilder value=new StringBuilder();
            for(String part:h.getValue()){if(value.length()>0)value.append("; ");value.append(part);}
            conn.setRequestProperty(h.getKey(),value.toString());
        }
        if(body!=null){
            conn.setRequestMethod("POST");conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json");
            byte[] data=body.toString().getBytes(StandardCharsets.UTF_8);conn.setFixedLengthStreamingMode(data.length);
            try(OutputStream out=conn.getOutputStream()){out.write(data);}
        }
        try {
            int status=conn.getResponseCode();cookies.put(target.toURI(),conn.getHeaderFields());
            if(status!=200)throw new IOException(stage+": HTTP "+status);
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=conn.getInputStream()){
                byte[] chunk=new byte[8192];int n;
                while((n=in.read(chunk))!=-1){checkDeadline();out.write(chunk,0,n);if(out.size()>2*1024*1024)throw new IOException("API response too large");}
            }
            return new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));
        } finally {NativeHttp.detach(conn);conn.disconnect();}
    }
    static Map<String,String> headers(String... pairs) {
        Map<String,String> map=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2)map.put(pairs[i],pairs[i+1]);return map;
    }
    static byte[] scalar(BigInteger n) {
        byte[] raw=n.toByteArray(),out=new byte[32];
        int length=Math.min(raw.length,32);System.arraycopy(raw,raw.length-length,out,32-length,length);return out;
    }
    static byte[] rawSignature(byte[] der) throws IOException {
        if(der.length<8||der[0]!=48||der[2]!=2)throw new IOException("Invalid ECDSA encoding");
        int rlen=der[3]&255,offset=4+rlen;
        if(offset+2>der.length||der[offset]!=2)throw new IOException("Invalid ECDSA scalar");
        int slen=der[offset+1]&255;
        if(offset+2+slen!=der.length)throw new IOException("Invalid ECDSA length");
        byte[] r=scalar(new BigInteger(1,Arrays.copyOfRange(der,4,4+rlen)));
        byte[] s=scalar(new BigInteger(1,Arrays.copyOfRange(der,offset+2,der.length)));
        byte[] raw=new byte[64];System.arraycopy(r,0,raw,0,32);System.arraycopy(s,0,raw,32,32);return raw;
    }
    JSONObject fingerprint(String apiOrigin) throws Exception {
        Map<String,String> h=headers("Origin",apiOrigin,"Referer",apiOrigin+"/");
        stage="access challenge";
        JSONObject c=request(apiOrigin+"/api/videos/access/challenge",new JSONObject(),h);
        stage="P-256 signing";
        KeyPairGenerator gen=KeyPairGenerator.getInstance("EC");gen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair=gen.generateKeyPair();ECPublicKey publicKey=(ECPublicKey)pair.getPublic();
        Signature signer=Signature.getInstance("SHA256withECDSA");signer.initSign(pair.getPrivate());
        signer.update(c.getString("nonce").getBytes(StandardCharsets.UTF_8));
        JSONObject jwk=new JSONObject().put("kty","EC").put("crv","P-256").put("ext",true)
            .put("key_ops",new JSONArray().put("verify")).put("x",encode(scalar(publicKey.getW().getAffineX())))
            .put("y",encode(scalar(publicKey.getW().getAffineY())));
        JSONObject client=new JSONObject().put("user_agent",USER_AGENT).put("languages",new JSONArray().put("ro-RO").put("en-US"))
            .put("timezone","UTC").put("extra",new JSONObject().put("vendor","").put("appVersion",USER_AGENT));
        JSONObject body=new JSONObject().put("viewer_id","").put("device_id","").put("challenge_id",c.getString("challenge_id"))
            .put("nonce",c.getString("nonce")).put("signature",encode(rawSignature(signer.sign()))).put("public_key",jwk)
            .put("client",client).put("storage",new JSONObject()).put("attributes",new JSONObject().put("entropy","low"));
        stage="attestation";
        JSONObject a=request(apiOrigin+"/api/videos/access/attest",body,h);
        return new JSONObject().put("token",a.getString("token")).put("viewer_id",a.optString("viewer_id",""))
            .put("device_id",a.optString("device_id","")).put("confidence",a.optDouble("confidence",0));
    }
    String solve(String nonce,int difficulty) throws Exception {
        if(difficulty<0||difficulty>32)throw new IOException("Unsupported challenge difficulty: "+difficulty);
        BysePow pow=new BysePow();long start=System.nanoTime();
        byte[] prefix=(nonce+":").getBytes(StandardCharsets.UTF_8),input=new byte[prefix.length+20];
        System.arraycopy(prefix,0,input,0,prefix.length);
        for(long counter=0;;counter++){
            if((counter&255)==0)checkDeadline();
            String decimal=Long.toString(counter);
            for(int i=0;i<decimal.length();i++)input[prefix.length+i]=(byte)decimal.charAt(i);
            if(Integer.numberOfLeadingZeros(pow.hash(input,prefix.length+decimal.length(),true)[0])>=difficulty){
                attempts=counter+1;powMillis=(System.nanoTime()-start)/1000000;return decimal;
            }
        }
    }
    static JSONObject decrypt(JSONObject encrypted) throws Exception {
        JSONArray parts=encrypted.getJSONArray("key_parts");
        int version=0;try{version=Integer.parseInt(encrypted.optString("version",""));}catch(NumberFormatException ignored){}
        ByteArrayOutputStream key=new ByteArrayOutputStream();
        if(version>=1&&version<=20&&version<=parts.length()&&31-version<=parts.length()){
            key.write(decode(parts.getString(version-1)));key.write(decode(parts.getString(30-version)));
        }else for(int i=0;i<parts.length();i++)key.write(decode(parts.getString(i)));
        Cipher aes=Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key.toByteArray(),"AES"),new GCMParameterSpec(128,decode(encrypted.getString("iv"))));
        byte[] plain=aes.doFinal(decode(encrypted.getString("payload")));
        return new JSONObject(new String(plain,StandardCharsets.UTF_8));
    }
    public JSONObject resolve(String embed) throws Exception {
        long start=System.nanoTime();deadline=Math.min(start+55000000000L,NativeHttp.session().deadline);
        try {
            String[] path=new URL(embed).getPath().split("/");
            if(path.length!=3||!path[1].equals("e")||!path[2].matches("[a-zA-Z0-9_-]+"))throw new IOException("Invalid embed URL");
            String code=path[2];stage="details";
            JSONObject details=request(origin(embed)+"/api/videos/"+code+"/embed/details",null,headers("Referer",embed));
            frameUrl=details.getString("embed_frame_url");String apiOrigin=origin(frameUrl);
            JSONObject fingerprint=fingerprint(apiOrigin);
            Map<String,String> h=headers("X-Embed-Parent",embed,"Referer",frameUrl);
            String api=apiOrigin+"/api/videos/"+code+"/embed/";
            stage="PoW challenge";
            JSONObject challenge=request(api+"captcha",new JSONObject().put("fingerprint",fingerprint),h);
            stage="PoW calculation";
            String solution=solve(challenge.getString("pow_nonce"),challenge.getInt("pow_difficulty"));
            stage="PoW verification";
            JSONObject verified=request(api+"captcha/verify",new JSONObject().put("pow_token",challenge.getString("pow_token"))
                .put("solution",solution).put("fingerprint",fingerprint),h);
            h.put("X-Captcha-Token",verified.getString("token"));stage="playback";
            JSONObject encrypted=request(api+"playback",new JSONObject().put("fingerprint",fingerprint),h);
            stage="authenticated playback decryption";
            JSONObject playback=decrypt(encrypted.getJSONObject("playback"));
            if(playback.getJSONArray("sources").length()==0)throw new IOException("No playback sources");
            totalMillis=(System.nanoTime()-start)/1000000;
            return playback;
        } catch(Exception e) {throw new IOException(stage+": "+e.getMessage(),e);}
    }
    public static void main(String[] args) throws Exception {
        ByseClient resolver=new ByseClient();
        JSONObject result=resolver.resolve(args[0]);
        System.out.println("NATIVE RESOLVE sources="+result.getJSONArray("sources").length()+" attempts="+resolver.attempts+" powMs="+resolver.powMillis+" totalMs="+resolver.totalMillis);
        // Verify playlist availability without printing expiring source URLs/tokens.
        String url=result.getJSONArray("sources").getJSONObject(0).getString("url");
        HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();
        conn.setConnectTimeout(10000);conn.setReadTimeout(10000);conn.setRequestProperty("User-Agent",USER_AGENT);conn.setRequestProperty("Referer",resolver.frameUrl);
        try {
            int status=conn.getResponseCode();
            String first;
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getInputStream(),StandardCharsets.UTF_8))){first=reader.readLine();}
            if(status!=200||!"#EXTM3U".equals(first))throw new IOException("Playlist validation failed: HTTP "+status);
            System.out.println("PLAYLIST PASS HTTP 200 #EXTM3U");
        }finally{conn.disconnect();}
    }
}
