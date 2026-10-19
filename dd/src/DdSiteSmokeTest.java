import org.json.*;import java.nio.file.*;import java.util.*;
public final class DdSiteSmokeTest {
 public static void main(String[] args)throws Exception{
  JSONArray cases=new JSONObject(new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8")).getJSONArray("fixtures"),results=new JSONArray();int passed=0;
  for(int i=0;i<4;i++){JSONObject f=cases.getJSONObject(i),r=new JSONObject().put("title",f.getString("title"));long start=System.nanoTime();NativeHttp.CURRENT.set(new NativeHttp.Session(30000));
   try{List<NativeCatalog.Candidate> found=new DdCatalog().discover(f.getJSONObject("data"));if(found.size()!=1||!found.get(0).host.equals("OK.ru"))throw new Exception("Expected only OK.ru");ResolvedStream stream=RoDubbedProvider.resolve(found.get(0));if(stream.isHls||!stream.site.equals("DeseneleDublate"))throw new Exception("Wrong native source format/site");Object link=RoDubbedProvider.makeLink(stream),type=link.getClass().getMethod("getType").invoke(link);if(!type.toString().equals("VIDEO"))throw new Exception("Not native VIDEO type");r.put("pass",true).put("host",stream.host).put("quality",stream.quality).put("type",type.toString());passed++;}catch(Throwable e){r.put("pass",false).put("error",e.getMessage());}finally{NativeHttp.session().stop();NativeHttp.CURRENT.remove();}r.put("ms",(System.nanoTime()-start)/1000000);results.put(r);System.out.println(r.toString());
  }Files.write(Paths.get(args[1]),new JSONObject().put("passed",passed).put("total",4).put("results",results).toString(2).getBytes("UTF-8"));if(passed!=4)System.exit(1);
 }
}
