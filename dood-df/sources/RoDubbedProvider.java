import com.lagradost.cloudstream3.metaproviders.TmdbProvider;
import kotlin.jvm.functions.Function1;
import kotlin.coroutines.Continuation;
import org.json.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

public final class RoDubbedProvider extends TmdbProvider {
    public String getName(){return "RoDubbed [Dood Test]";}
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
        for(int pass=0;pass<2;pass++)for(String host:Arrays.asList("Byse","SeekStreaming","StreamP2P","VOE","Vidmoly","Streamtape","Abyss","Playmogo","OK.ru","Morencius")){
            int n=0;for(NativeCatalog.Candidate c:found)if(c.host.equals(host)){if(n++==pass){ordered.add(c);break;}}
        }
        return ordered;
    }
    public Object loadLinks(String data,boolean casting,Function1 subtitle,Function1 callback,Continuation continuation){
        final NativeHttp.Session session=new NativeHttp.Session(38000);NativeHttp.CURRENT.set(session);
        ExecutorService pool=Executors.newFixedThreadPool(3,new ThreadFactory(){public Thread newThread(Runnable task){Thread t=new Thread(task,"RoDubbed-host");t.setDaemon(true);return t;}});
        ExecutorService catalogs=Executors.newFixedThreadPool(4,new ThreadFactory(){public Thread newThread(Runnable task){Thread t=new Thread(task,"RoDubbed-catalog");t.setDaemon(true);return t;}});
        CompletionService<List<NativeCatalog.Candidate>> discoveries=new ExecutorCompletionService<>(catalogs);
        List<Future<List<NativeCatalog.Candidate>>> catalogFutures=new ArrayList<>();
        CompletionService<ResolvedStream> completion=new ExecutorCompletionService<>(pool);
        List<Future<ResolvedStream>> futures=new ArrayList<>();Set<String> returned=new HashSet<>();final Set<String> returnedHosts=new HashSet<>();
        long started=System.nanoTime();int count=0,active=0,next=0;
        try{
            // Discover sites together so an absent title on one site cannot consume
            // the entire lookup deadline before another site's quick result is used.
            for(int website=0;website<1;website++){
                final int site=website;
                catalogFutures.add(discoveries.submit(new Callable<List<NativeCatalog.Candidate>>(){public List<NativeCatalog.Candidate> call(){
                    NativeHttp.CURRENT.set(session);
                    try{JSONObject input=new JSONObject(data);return site==0?new NativeCatalog().discover(input):site==1?new DozaCatalog().discover(input):site==2?new FdCatalog().discover(input):new DdCatalog().discover(input);}
                    catch(Exception e){System.out.println("[RoDubbed] catalog "+site+" skipped: "+e.getMessage());return Collections.emptyList();}
                    finally{NativeHttp.CURRENT.remove();}
                }}));
            }
            for(int website=0;website<1&&count<3;website++){
            session.check();long catalogRemaining=session.deadline-System.nanoTime();if(catalogRemaining<=0)break;
            Future<List<NativeCatalog.Candidate>> discovered=discoveries.poll(catalogRemaining,TimeUnit.NANOSECONDS);if(discovered==null)break;
            List<NativeCatalog.Candidate> found=discovered.get();
            for(Iterator<NativeCatalog.Candidate> it=found.iterator();it.hasNext();)if(!it.next().host.equals("Playmogo"))it.remove();List<NativeCatalog.Candidate> queue=order(found);Collections.sort(queue,new Comparator<NativeCatalog.Candidate>(){public int compare(NativeCatalog.Candidate a,NativeCatalog.Candidate b){return Boolean.compare(returnedHosts.contains(a.host),returnedHosts.contains(b.host));}});next=0;
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
                if(stream!=null&&returned.add(stream.identity)){callback.invoke(makeLink(stream));returnedHosts.add(stream.host);count++;}
            }
            }
        }catch(Exception e){System.out.println("[RoDubbed] discovery stopped: "+e.getMessage());}
        finally{for(Future<?> f:futures)if(!f.isDone())f.cancel(true);for(Future<?> f:catalogFutures)if(!f.isDone())f.cancel(true);session.stop();pool.shutdownNow();catalogs.shutdownNow();NativeHttp.CURRENT.remove();}
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
        else if(c.host.equals("Playmogo")){stream=PlaymogoClient.resolve(c);checked=true;}
        else if(c.host.equals("Abyss")){stream=AbyssClient.resolve(c);checked=true;}
        else if(c.host.equals("Streamtape")){stream=StreamtapeClient.resolve(c);checked=true;}
        else if(c.host.equals("Morencius"))stream=MorenciusClient.resolve(c);
        else if(c.host.equals("OK.ru")){stream=OkruClient.resolve(c);checked=true;}
        else throw new Exception("Unsupported host");
        if(!checked)HlsProbe.validate(stream);return new ResolvedStream(stream.host,stream.url,stream.referer,stream.identity,stream.quality,DdTitleCatalog.siteUrl(c.page)?"DeseneleDublate":FdCatalog.siteUrl(c.page)?"FilmeDublate":DozaCatalog.siteUrl(c.page)?"DozaAnimata":"DeseneFaine",stream.isHls,stream.playbackHeaders);
    }
    static Object makeLink(ResolvedStream stream)throws Exception{
        Constructor<?> c=Class.forName("com.lagradost.cloudstream3.utils.ExtractorLink").getConstructor(String.class,String.class,String.class,String.class,int.class,boolean.class,Map.class,String.class);
        Map<String,String> headers=stream.playbackHeaders!=null?new HashMap<>(stream.playbackHeaders):stream.host.equals("Abyss")?AbyssClient.mediaHeaders(stream.referer):NativeHttp.headers(stream.referer);if(!headers.containsKey("User-Agent"))headers.put("User-Agent",ByseClient.USER_AGENT);
        return c.newInstance("RoDubbed",stream.host+" | RO Dub | "+stream.site,stream.url,stream.referer,stream.quality,stream.isHls,headers,null);
    }
}
