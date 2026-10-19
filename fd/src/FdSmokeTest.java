import org.json.*;import java.nio.file.*;import java.util.*;
public final class FdSmokeTest {
 public static void main(String[] args)throws Exception{
  JSONArray cases=new JSONObject(new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8")).getJSONArray("fixtures"),results=new JSONArray();int addresses=0,streams=0;
  for(int i=0;i<cases.length();i++){JSONObject f=cases.getJSONObject(i),out=new JSONObject().put("title",f.getString("title"));long start=System.nanoTime();NativeHttp.CURRENT.set(new NativeHttp.Session(30000));
   try{FdCatalog.Located located=new FdCatalog().locate(f.getJSONObject("data"));boolean matched=located!=null&&located.titleUrl.equals(f.getString("expectedPage"))&&located.wrapper.equals(f.getString("expectedWrapper"));out.put("addressPass",matched);if(!matched)throw new Exception("Wrong title/episode wrapper");addresses++;List<NativeCatalog.Candidate> found=new FdCatalog().discover(f.getJSONObject("data"));JSONArray checks=new JSONArray();
    for(NativeCatalog.Candidate c:found){JSONObject check=new JSONObject().put("host",c.host);try{ResolvedStream stream=RoDubbedProvider.resolve(c);Object link=RoDubbedProvider.makeLink(stream),type=link.getClass().getMethod("getType").invoke(link);if(stream.isHls!=type.toString().equals("M3U8")||!stream.site.equals("FilmeDublate"))throw new Exception("Wrong native format/site");check.put("pass",true).put("format",type.toString());streams++;}catch(Exception e){check.put("pass",false).put("error",e.getMessage());}checks.put(check);}out.put("streams",checks);
   }catch(Exception e){out.put("error",e.getMessage());}finally{NativeHttp.session().stop();NativeHttp.CURRENT.remove();}out.put("ms",(System.nanoTime()-start)/1000000);results.put(out);System.out.println(out.toString());
  }Files.write(Paths.get(args[1]),new JSONObject().put("addressesPassed",addresses).put("total",cases.length()).put("streamsPassed",streams).put("results",results).toString(2).getBytes("UTF-8"));if(addresses!=cases.length())System.exit(1);
 }
}
