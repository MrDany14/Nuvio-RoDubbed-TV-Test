import org.json.*;
import kotlin.jvm.functions.Function1;
import java.util.*;

public final class UnifiedSmokeTest {
 public static void main(String[] args)throws Exception{
  RoDubbedProvider provider=new RoDubbedProvider();boolean tv=args.length>2;
  Object loaded=provider.load(new JSONObject().put("id",Integer.parseInt(args[0])).put("type",tv?"tv":"movie").toString(),null);
  if(loaded==null)throw new AssertionError("Metadata failed");String data=null;
  if(!tv)data=(String)loaded.getClass().getMethod("getDataUrl").invoke(loaded);
  else for(Object ep:(List<?>)loaded.getClass().getMethod("getEpisodes").invoke(loaded)){
   if(Integer.valueOf(args[2]).equals(ep.getClass().getMethod("getSeason").invoke(ep))&&Integer.valueOf(args[3]).equals(ep.getClass().getMethod("getEpisode").invoke(ep))){data=(String)ep.getClass().getMethod("getData").invoke(ep);break;}
  }
  if(data==null)throw new AssertionError("Episode missing");final List<String> names=new ArrayList<>();
  Function1 callback=new Function1(){public Object invoke(Object link){try{
   String name=(String)link.getClass().getMethod("getName").invoke(link),url=(String)link.getClass().getMethod("getUrl").invoke(link);
   if(!url.startsWith("https://")||!name.contains("RO Dub"))throw new AssertionError("Invalid source");names.add(name);System.out.println("SOURCE "+name);return null;
  }catch(Exception e){throw new RuntimeException(e);}}};
  provider.loadLinks(data,false,new Function1(){public Object invoke(Object x){return null;}},callback,null);
  if(names.size()!=Integer.parseInt(args[1])||names.size()>3)throw new AssertionError("Expected "+args[1]+" sources, got "+names.size());
  System.out.println("PASS REAL APK unified source count "+names.size());
 }
}
