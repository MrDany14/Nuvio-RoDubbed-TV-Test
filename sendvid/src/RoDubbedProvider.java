import com.lagradost.cloudstream3.metaproviders.TmdbProvider;
import kotlin.jvm.functions.Function1;
import kotlin.coroutines.Continuation;
import org.json.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

public final class RoDubbedProvider extends TmdbProvider {
    public String getName(){return "RoDubbed [Sendvid Test]";}
    public String getMainUrl(){return NativeCatalog.SITE;}
    public String getLang(){return "ro";}
    public boolean getHasMainPage(){return false;}
    public Object load(String input,Continuation continuation){
        NativeHttp.Session s=new NativeHttp.Session(12000);NativeHttp.CURRENT.set(s);
        try{return NativeMetadata.load(input,getName());}
        catch(Exception e){System.out.println("[RoDubbed] metadata failed: "+e.getMessage());return null;}
        finally{s.stop();NativeHttp.CURRENT.remove();}
    }
    static List<NativeCatalog.Candidate> order(List<NativeCatalog.Candidate> found){
        List<NativeCatalog.Candidate> ordered=new ArrayList<>();
        for(int pass=0;pass<2;pass++)for(String host:Arrays.asList("Byse","SeekStreaming","StreamP2P","VOE","Vidmoly")){
            int n=0;for(NativeCatalog.Candidate c:found)if(c.host.equals(host)){if(n++==pass){ordered.add(c);break;}}
        }
        return ordered;
    }
    public Object loadLinks(String data,boolean casting,Function1 subtitle,Function1 callback,Continuation continuation){
        final NativeHttp.Session session=new NativeHttp.Session(38000);NativeHttp.CURRENT.set(session);
        ExecutorService pool=Executors.newFixedThreadPool(3,new ThreadFactory(){public Thread newThread(Runnable task){Thread t=new Thread(task,"RoDubbed-host");t.setDaemon(true);return t;}});
        CompletionService<ResolvedStream> completion=new ExecutorCompletionService<>(pool);
        List<Future<ResolvedStream>> futures=new ArrayList<>();Set<String> returned=new HashSet<>();
        long started=System.nanoTime();int count=0,active=0,next=0;
        try{
            for(int website=0;website<2&&count<3;website++){
            session.check();List<NativeCatalog.Candidate> found;
            try{found=website==0?new NativeCatalog().discover(new JSONObject(data)):new DozaCatalog().discover(new JSONObject(data));}
            catch(Exception e){System.out.println("[RoDubbed] catalog "+website+" skipped: "+e.getMessage());continue;}
            List<NativeCatalog.Candidate> queue=order(found);next=0;
            while(count<3&&(active>0||next<queue.size())){
                while(count+active<3&&next<queue.size()){
                    final NativeCatalog.Candidate candidate=queue.get(next++);
                    futures.add(completion.submit(new Callable<ResolvedStream>(){public ResolvedStream call(){
                        NativeHttp.CURRENT.set(session);
                        try{return resolve(candidate);}
                        catch(Exception e){System.out.println("[RoDubbed] "+candidate.host+" skipped: "+e.getMessage());return null;}
                        finally{NativeHttp.CURRENT.remove();}
                    }}));active++;
                }
                long remaining=session.deadline-System.nanoTime();if(remaining<=0)break;
                Future<ResolvedStream> done=completion.poll(remaining,TimeUnit.NANOSECONDS);if(done==null)break;active--;
                ResolvedStream stream=done.get();
                if(stream!=null&&returned.add(stream.identity)){callback.invoke(makeLink(stream));count++;}
            }
            }
        }catch(Exception e){System.out.println("[RoDubbed] discovery stopped: "+e.getMessage());}
        finally{for(Future<?> f:futures)if(!f.isDone())f.cancel(true);session.stop();pool.shutdownNow();NativeHttp.CURRENT.remove();}
        System.out.println("[RoDubbed] confirmedRO="+count+" totalMs="+(System.nanoTime()-started)/1000000);
        return count>0;
    }
    static ResolvedStream resolve(NativeCatalog.Candidate raw)throws Exception{
        NativeCatalog.Candidate c=NativeCatalog.resolveWrapper(raw);ResolvedStream stream;boolean checked=false;
        if(c.host.equals("Byse")){
            ByseClient resolver=new ByseClient();JSONObject playback=resolver.resolve(c.embed);JSONArray sources=playback.getJSONArray("sources");
            JSONObject source=sources.getJSONObject(0);stream=new ResolvedStream("Byse",source.getString("url"),resolver.frameUrl,c.identity(),source.optInt("height",1080));
            System.out.println("[RoDubbed] Byse powMs="+resolver.powMillis);
        }else if(c.host.equals("SeekStreaming")||c.host.equals("StreamP2P")){stream=SharedPlayerClient.resolve(c);checked=true;}
        else if(c.host.equals("VOE"))stream=VoeClient.resolve(c);
        else if(c.host.equals("Vidmoly"))stream=VidmolyClient.resolve(c);
        else throw new Exception("Unsupported host");
        if(!checked)HlsProbe.validate(stream);return new ResolvedStream(stream.host,stream.url,stream.referer,stream.identity,stream.quality,DozaCatalog.siteUrl(c.page)?"DozaAnimata":"DeseneFaine");
    }
    static Object makeLink(ResolvedStream stream)throws Exception{
        Constructor<?> c=Class.forName("com.lagradost.cloudstream3.utils.ExtractorLink").getConstructor(String.class,String.class,String.class,String.class,int.class,boolean.class,Map.class,String.class);
        Map<String,String> headers=NativeHttp.headers(stream.referer);headers.put("User-Agent",ByseClient.USER_AGENT);
        return c.newInstance("RoDubbed",stream.host+" | RO Dub | "+stream.site,stream.url,stream.referer,stream.quality,true,headers,null);
    }
}
