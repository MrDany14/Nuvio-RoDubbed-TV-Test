import com.lagradost.cloudstream3.metaproviders.TmdbProvider;
import kotlin.jvm.functions.Function1;
import kotlin.coroutines.Continuation;
import org.json.*;
import java.lang.reflect.*;
import java.util.*;

public final class ByseNativeProvider extends TmdbProvider {
    public String getName(){return "RoDubbed Byse Native [TV Test]";}
    public String getMainUrl(){return "https://desenefaine.com";}
    public String getLang(){return "ro";}
    public boolean getHasMainPage(){return false;}

    public Object load(String input,Continuation continuation){
        try{return NativeMetadata.load(input,getName());}
        catch(Exception e){System.out.println("[Byse Native] metadata failed: "+e.getClass().getSimpleName()+": "+e.getMessage());return null;}
    }

    // Kotlin suspend functions may return immediately; no separate server/thread is used.
    public Object loadLinks(String data,boolean casting,Function1 subtitle,Function1 callback,Continuation continuation){
        try {
            long started=System.nanoTime();
            NativeCatalog catalog=new NativeCatalog();
            String embed=catalog.find(new JSONObject(data));
            if(embed==null)return Boolean.FALSE;
            ByseClient resolver=new ByseClient();JSONObject result=resolver.resolve(embed);
            JSONArray sources=result.getJSONArray("sources");
            for(int i=0;i<sources.length();i++){
                JSONObject s=sources.getJSONObject(i);String url=s.optString("url","");
                if(!url.startsWith("https://"))continue;
                int quality=s.optInt("height",0);
                if(quality==0){String q=s.optString("quality","").replaceAll("[^0-9]","");try{quality=Integer.parseInt(q);}catch(Exception ignored){}}
                callback.invoke(makeLink(url,resolver.frameUrl,quality));
            }
            System.out.println("[Byse Native] sources="+sources.length()+" attempts="+resolver.attempts+" powMs="+resolver.powMillis+" totalMs="+(System.nanoTime()-started)/1000000);
            return Boolean.TRUE;
        }catch(Exception e){
            System.out.println("[Byse Native] lookup failed: "+e.getClass().getSimpleName()+": "+e.getMessage());
            return Boolean.FALSE;
        }
    }
    static Object makeLink(String url,String referer,int quality) throws Exception {
        Class<?> clazz=Class.forName("com.lagradost.cloudstream3.utils.ExtractorLink");
        // Long-supported non-default constructor retained by the Cloudstream library.
        Constructor<?> c=clazz.getConstructor(String.class,String.class,String.class,String.class,int.class,boolean.class,Map.class,String.class);
        Map<String,String> h=new LinkedHashMap<>();h.put("User-Agent",ByseClient.USER_AGENT);h.put("Referer",referer);h.put("Origin",ByseClient.origin(referer));
        return c.newInstance("RoDubbed Byse Native", "Byse | RO",url,referer,quality,true,h,null);
    }
}
