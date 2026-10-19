import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

final class NativeHttp {
    static final class Session {
        final long deadline;
        final Set<HttpURLConnection> active=Collections.newSetFromMap(new ConcurrentHashMap<HttpURLConnection,Boolean>());
        volatile boolean cancelled;
        final Map<String,String> resolvedUrls=new ConcurrentHashMap<>();
        final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        Session(long millis){deadline=System.nanoTime()+millis*1000000L;}
        void check()throws IOException{if(cancelled||Thread.currentThread().isInterrupted()||System.nanoTime()>deadline)throw new IOException("Lookup stopped or timed out");}
        void stop(){cancelled=true;for(HttpURLConnection c:active)c.disconnect();active.clear();}
    }
    static final ThreadLocal<Session> CURRENT=new ThreadLocal<>();
    static Session session(){Session s=CURRENT.get();if(s==null){s=new Session(12000);CURRENT.set(s);}return s;}
    static void attach(HttpURLConnection c)throws IOException{Session s=session();s.check();s.active.add(c);}
    static void detach(HttpURLConnection c){session().active.remove(c);}
    static byte[] bytes(String url,Map<String,String> headers,int limit,boolean partial)throws Exception{
        return request(url,headers,limit,partial,null);
    }
    static byte[] request(String url,Map<String,String> headers,int limit,boolean partial,String body)throws Exception{
        Session s=session();s.check();URL u=new URL(url);
        if(!"https".equals(u.getProtocol()))throw new IOException("HTTPS required");
        HttpURLConnection c=(HttpURLConnection)u.openConnection();attach(c);
        int timeout=(int)Math.max(1,Math.min(6000,(s.deadline-System.nanoTime())/1000000));
        c.setConnectTimeout(timeout);c.setReadTimeout(timeout);c.setRequestProperty("User-Agent",ByseClient.USER_AGENT);
        for(Map.Entry<String,String> h:headers.entrySet())c.setRequestProperty(h.getKey(),h.getValue());
        synchronized(s.cookies){for(Map.Entry<String,List<String>> h:s.cookies.get(u.toURI(),Collections.<String,List<String>>emptyMap()).entrySet()){
            StringBuilder value=new StringBuilder();for(String part:h.getValue()){if(value.length()>0)value.append("; ");value.append(part);}c.setRequestProperty(h.getKey(),value.toString());
        }}
        if(partial)c.setRequestProperty("Range","bytes=0-4095");
        try{
            if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type",headers.containsKey("Content-Type")?headers.get("Content-Type"):"application/json");byte[] data=body.getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(data.length);try(OutputStream out=c.getOutputStream()){out.write(data);}}
            int status=c.getResponseCode();synchronized(s.cookies){s.cookies.put(u.toURI(),c.getHeaderFields());}if(status!=200&&status!=206)throw new IOException("HTTP "+status);s.resolvedUrls.put(url,c.getURL().toString());
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){
                byte[] buf=new byte[Math.min(limit,8192)];int n;
                while(out.size()<limit&&(n=in.read(buf,0,Math.min(buf.length,limit-out.size())))!=-1){s.check();out.write(buf,0,n);}
            }
            if(out.size()==limit&&!partial)throw new IOException("Response too large");return out.toByteArray();
        }catch(javax.net.ssl.SSLException e){throw new IOException("TLS verification failed for "+u.getHost()+": "+e.getMessage(),e);}
        finally{detach(c);c.disconnect();}
    }
    static String text(String url,Map<String,String> headers)throws Exception{return new String(bytes(url,headers,2*1024*1024,false),StandardCharsets.UTF_8);}
    static org.json.JSONObject post(String url,Map<String,String> headers,org.json.JSONObject body)throws Exception{return new org.json.JSONObject(new String(request(url,headers,1024*1024,false,body.toString()),StandardCharsets.UTF_8));}
    static Map<String,String> headers(String referer)throws Exception{return ByseClient.headers("Referer",referer.split("#")[0],"Origin",ByseClient.origin(referer));}
}
