import org.json.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

final class SharedPlayerClient {
    static final byte[] KEY="kiemtienmua911ca".getBytes(StandardCharsets.US_ASCII),IV="1234567890oiuytr".getBytes(StandardCharsets.US_ASCII);
    static byte[] unhex(String s)throws Exception{if((s.length()&1)!=0||!s.matches("[a-fA-F0-9]+"))throw new Exception("Invalid encrypted payload");byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}
    static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(java.util.Locale.ROOT,"%02x",x&255));return s.toString();}
    static Cipher cipher(int mode)throws Exception{Cipher c=Cipher.getInstance("AES/CBC/PKCS5Padding");c.init(mode,new SecretKeySpec(KEY,"AES"),new IvParameterSpec(IV));return c;}
    static JSONObject decrypt(String text)throws Exception{return new JSONObject(new String(cipher(Cipher.DECRYPT_MODE).doFinal(unhex(text.trim())),StandardCharsets.UTF_8));}
    static String encode(String text)throws Exception{return URLEncoder.encode(text,"UTF-8").replace("+","%20");}
    static ResolvedStream resolve(NativeCatalog.Candidate candidate)throws Exception{
        String origin=ByseClient.origin(candidate.embed),id=new URL(candidate.embed).getRef();
        if(id==null||id.isEmpty())throw new Exception("Video ID missing");id=id.split("&")[0];
        String website=new URL(candidate.page).getHost();Map<String,String> headers=NativeHttp.headers(candidate.embed);
        // Same ordinary video API used by both current players; no ads or analytics executed.
        JSONObject video=decrypt(NativeHttp.text(origin+"/api/v1/video?id="+encode(id)+"&w=1920&h=1080&r="+encode(website),headers));
        JSONObject player=video.optJSONObject("player");String language=player!=null?player.optString("defaultAudio",""):"";
        if(language.isEmpty())language=video.optString("defaultAudio",video.optString("audioLanguage",""));
        RomanianAudio.checkHostDefault(language);
        JSONObject key=video.optJSONObject("pk"),metric=video.optJSONObject("metric");
        if(key==null&&metric!=null){
            byte[] random=new byte[16];new SecureRandom().nextBytes(random);
            JSONObject request=new JSONObject().put("website",website).put("playing",true).put("sessionId",ByseClient.encode(random));
            for(String field:Arrays.asList("userId","playerId","videoId","country","platform","browser","os"))if(metric.has(field))request.put(field,metric.get(field));
            if(!request.has("videoId"))request.put("videoId",id);
            String encrypted=hex(cipher(Cipher.ENCRYPT_MODE).doFinal(request.toString().getBytes(StandardCharsets.UTF_8)));
            key=new JSONObject(NativeHttp.text(origin+"/api/v1/player?t="+encrypted,headers));
        }
        int quality=1080;try{quality=Integer.parseInt(video.optString("resolution",video.optString("quality","1080")).replaceAll("[^0-9]",""));}catch(Exception ignored){}
        Exception last=null;
        for(String url:options(video,origin,key)){
            if(!url.matches("(?i)https://.*\\.(?:m3u8|txt)(?:[?#].*)?"))continue;
            ResolvedStream stream=new ResolvedStream(candidate.host,url,candidate.embed,candidate.identity(),quality);
            try{HlsProbe.validate(stream);return stream;}catch(Exception e){last=e;}
        }
        throw new Exception("No validated HLS backend"+(last==null?"":": "+last.getMessage()));
    }
    static String source(JSONObject video,String origin,JSONObject key)throws Exception{
        return options(video,origin,key).get(0);
    }
    static List<String> options(JSONObject video,String origin,JSONObject key)throws Exception{
        JSONObject config;try{config=new JSONObject(video.optString("streamingConfig","{}"));}catch(Exception e){config=new JSONObject();}
        JSONObject adjust=config.optJSONObject("adjust");if(adjust==null)adjust=new JSONObject();
        LinkedHashMap<String,String> urls=new LinkedHashMap<>();
        urls.put("Tiktok",video.optString("hlsVideoTiktok",""));urls.put("Google",video.optString("hlsVideoGoogle",""));
        String cloud=video.optString("cf","");if(cloud.isEmpty())cloud=video.optString("cfNative","");
        urls.put("Cloudflare",cloud);urls.put("In-House",video.optString("source",""));
        List<String> order=new ArrayList<>();JSONArray configured=config.optJSONArray("order");
        if(configured!=null)for(int i=0;i<configured.length();i++)order.add(configured.getString(i));else order.addAll(urls.keySet());
        if(!urls.get("In-House").isEmpty()){order.remove("In-House");order.add(0,"In-House");}
        List<String> found=new ArrayList<>();
        for(String name:order){
            String value=urls.get(name);JSONObject setting=adjust.optJSONObject(name);if(setting==null)setting=new JSONObject();
            if(value==null||value.isEmpty()||setting.optBoolean("disabled",false))continue;
            String url=new URL(new URL(origin+"/"),value).toString();
            if(!setting.optString("domain","").isEmpty())url=url.replace("/hls/","/hlsmod/"+setting.getString("domain")+"/");
            JSONObject params=setting.optJSONObject("params");if(params!=null)for(Iterator<String> i=params.keys();i.hasNext();){String k=i.next();url+=(url.contains("?")?"&":"?")+encode(k)+"="+encode(String.valueOf(params.get(k)));}
            if(key!=null&&!key.optString("k","").isEmpty()&&!url.matches(".*[?&]k=.*"))url+=(url.contains("?")?"&":"?")+"k="+encode(key.getString("k"))+"&kx="+encode(key.optString("kx",""));
            if(!found.contains(url))found.add(url);
        }
        if(found.isEmpty())throw new Exception("No enabled streaming backend");return found;
    }
}
