import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;
import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

final class VoeClient {
    static JSONObject decode(String encoded)throws Exception{
        if(encoded.length()>1024*1024)throw new Exception("VOE configuration too large");
        StringBuilder rotated=new StringBuilder();
        for(char c:encoded.toCharArray())rotated.append((char)(c>='A'&&c<='Z'?(c-'A'+13)%26+'A':c>='a'&&c<='z'?(c-'a'+13)%26+'a':c));
        String value=rotated.toString();for(String noise:new String[]{"@$","^^","~@","%?","*~","!!","#&","_"})value=value.replace(noise,"");
        byte[] bytes=ByseClient.decode(value);StringBuilder shifted=new StringBuilder();
        for(byte b:bytes)shifted.append((char)((b&255)-3));
        return new JSONObject(new String(ByseClient.decode(shifted.reverse().toString()),StandardCharsets.UTF_8));
    }
    static ResolvedStream resolve(NativeCatalog.Candidate c)throws Exception{
        String current=c.embed,html="";
        for(int step=0;step<3;step++){
            html=NativeHttp.text(current,NativeHttp.headers(c.page));
            for(Element script:Jsoup.parse(html).select("script[type=application/json]")){
                JSONArray payload;try{payload=new JSONArray(script.data());}catch(Exception e){continue;}
                if(payload.length()==0||!(payload.opt(0) instanceof String))continue;
                JSONObject config=decode(payload.getString(0));String source=config.optString("source","");
                if(!source.startsWith("https://"))throw new Exception("VOE HTTPS stream unavailable");
                for(String key:new String[]{"defaultAudio","audioLanguage"})RomanianAudio.checkHostDefault(config.optString(key,""));
                return new ResolvedStream("VOE",source,current,c.identity(),0);
            }
            Matcher redirect=Pattern.compile("window\\.location\\.href\\s*=\\s*['\"](https://[^'\"]+)['\"]").matcher(html);
            if(!redirect.find())throw new Exception("VOE playback configuration unavailable");
            URL target=new URL(redirect.group(1));
            if(!target.getPath().equals(new URL(c.embed).getPath())||target.getUserInfo()!=null)throw new Exception("Unexpected VOE redirect");
            current=target.toString();
        }
        throw new Exception("VOE redirect limit exceeded");
    }
}
