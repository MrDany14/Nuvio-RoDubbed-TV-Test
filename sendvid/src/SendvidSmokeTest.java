import org.json.*;import java.nio.file.*;import java.util.*;
public final class SendvidSmokeTest {
 public static void main(String[] args)throws Exception{
  NativeHttp.CURRENT.set(new NativeHttp.Session(30000));long start=System.nanoTime();
  try{
   JSONObject input=new JSONObject(new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8"));
   List<NativeCatalog.Candidate> candidates=new DozaCatalog().discover(input);
   if(candidates.size()!=1||!candidates.get(0).host.equals("Sendvid"))throw new AssertionError("Expected one supported Sendvid candidate; Netu excluded");
   ResolvedStream stream=RoDubbedProvider.resolve(candidates.get(0));
   if(stream.isHls||!stream.host.equals("Sendvid")||!stream.site.equals("DozaAnimata"))throw new AssertionError("Wrong MP4 source attribution");
   Object link=RoDubbedProvider.makeLink(stream);Object type=link.getClass().getMethod("getType").invoke(link);
   if(!type.toString().equals("VIDEO"))throw new AssertionError("MP4 passed as "+type);
   JSONObject result=new JSONObject().put("host",stream.host).put("site",stream.site).put("linkType",type.toString()).put("supportedCandidates",candidates.size()).put("mp4HeaderValidated",true).put("ms",(System.nanoTime()-start)/1000000).put("playbackTested",false);
   Files.write(Paths.get(args[1]),result.toString(2).getBytes("UTF-8"));System.out.println("PASS "+result.toString());
  }catch(Throwable e){System.out.println("FAIL "+e.getClass().getSimpleName()+": "+e.getMessage());System.exit(1);}
  finally{NativeHttp.session().stop();NativeHttp.CURRENT.remove();}
 }
}
