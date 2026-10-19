import org.json.*;import java.nio.file.*;
public final class DdAddressAudit {
 public static void main(String[] args)throws Exception{
  JSONArray cases=new JSONObject(new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8")).getJSONArray("fixtures"),results=new JSONArray();int passed=0;
  for(int i=0;i<cases.length();i++){JSONObject f=cases.getJSONObject(i),r=new JSONObject().put("title",f.getString("title")).put("expected",f.getString("expectedPage"));NativeHttp.CURRENT.set(new NativeHttp.Session(30000));long start=System.nanoTime();
   try{DdCatalog.Located found=new DdCatalog().locate(f.getJSONObject("data"));r.put("actual",found==null?"":found.pageUrl).put("pass",found!=null&&found.pageUrl.equals(f.getString("expectedPage")));if(r.getBoolean("pass"))passed++;}catch(Exception e){r.put("pass",false).put("error",e.getMessage());}finally{NativeHttp.session().stop();NativeHttp.CURRENT.remove();}
   r.put("ms",(System.nanoTime()-start)/1000000);results.put(r);System.out.println((r.getBoolean("pass")?"PASS ":"FAIL ")+f.getString("title"));
  }Files.write(Paths.get(args[1]),new JSONObject().put("passed",passed).put("total",cases.length()).put("results",results).toString(2).getBytes("UTF-8"));System.out.println("DD ADDRESS "+passed+"/"+cases.length());
 }
}
