import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

final class NativeCatalog {
    static final String SITE="https://desenefaine.com",TMDB_KEY="e6333b32409e02a4a6eba6fb7ff866bb";
    static final class Candidate {
        final String host,page,wrapper,label,embed;
        Candidate(String host,String page,String wrapper,String label,String embed){this.host=host;this.page=page;this.wrapper=wrapper;this.label=label;this.embed=embed;}
        String identity()throws Exception{URL u=new URL(embed);String ref=u.getRef();return host+":"+(ref==null?u.getPath():ref.split("&")[0]);}
    }
    final Set<String> visited=new HashSet<>();final Set<String> titles=new LinkedHashSet<>();
    int year,season,episode;boolean series;
    String get(String url)throws Exception{return NativeHttp.text(url,Collections.<String,String>emptyMap());}
    static String normalize(String s){return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
    static String kind(String url){
        try{
            String host=new URL(url).getHost().toLowerCase(Locale.ROOT);
            if(host.matches("(?:[a-z0-9-]+\\.)*byse[a-z0-9-]*\\.[a-z]{2,}"))return "Byse";
            if(host.matches("(?:streamp2p|p2pplay)\\..*"))return "StreamP2P";
            if(host.matches("(?:seekstream(?:ing)?|seeksreaming|embedseek)\\..*"))return "SeekStreaming";
            if(host.equals("player.desenefaine.net")||host.matches("(?:[a-z0-9-]+\\.)?(?:netu|hqq|waaw)\\.(?:ac|tv|to|io)"))return "Netu";
        }catch(Exception ignored){}return "";
    }
    static String labelKind(String label){String s=normalize(label);if(s.contains("byse")||s.contains("filemoon"))return "Byse";if(s.contains("streamp2p")||s.contains("p2pplay"))return "StreamP2P";if(s.contains("seekstream")||s.contains("seeksreaming"))return "SeekStreaming";if(s.contains("netu")||s.contains("hqq"))return "Netu";return "";}
    static boolean catalogUrl(String url){try{URL u=new URL(url);return "https".equals(u.getProtocol())&&"desenefaine.com".equals(u.getHost());}catch(Exception e){return false;}}
    boolean matches(Document page,String url){
        if(series&&!url.matches(".*(?:sezonul-|sezon-|s0?)"+season+"-(?:episodul-|ep|e0?)"+episode+"(?:-|/|$).*"))return false;
        Set<String> pageNames=new HashSet<>();
        for(Element h:page.select("h1")){
            String name=normalize(h.text());if(series)name=name.replaceFirst("\\s+(?:sezonul|sezon|season) \\d+ episodul \\d+.*$","");
            pageNames.add(name.replaceFirst(" (?:19|20)\\d{2}$",""));
        }
        for(Element li:page.select(".details-lst li")){
            String text=normalize(li.text());
            if(text.startsWith("titlu secundar")||text.startsWith("titlu original")){
                Element last=li.select("span").last();if(last!=null)pageNames.add(normalize(last.text()));
            }
        }
        boolean title=false;for(String name:titles)if(pageNames.contains(normalize(name)))title=true;
        if(!title)return false;
        Matcher m=Pattern.compile("\\b(?:19|20)\\d{2}\\b").matcher(page.select(".year").first()==null?"":page.select(".year").first().text());
        return series||year==0||!m.find()||Integer.parseInt(m.group())==year;
    }
    List<Candidate> inspect(String url)throws Exception{
        if(!visited.add(url))return Collections.emptyList();
        String html;try{html=get(url);}catch(Exception e){if(e.getMessage()!=null&&e.getMessage().contains("HTTP 404"))return Collections.emptyList();throw e;}
        Document page=Jsoup.parse(html,url);if(!matches(page,url))return Collections.emptyList();
        List<Candidate> found=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Element server:page.select("a[data-src]")){
            String label=server.text();if(!RomanianAudio.declaredRomanian(label))continue;
            String wrapper;try{wrapper=new String(ByseClient.decode(server.attr("data-src")),StandardCharsets.UTF_8);}catch(Exception e){continue;}
            String direct=kind(wrapper),expected=direct.isEmpty()?labelKind(label):direct;
            if(expected.isEmpty()||(!catalogUrl(wrapper)&&direct.isEmpty())||!seen.add(wrapper))continue;
            found.add(new Candidate(expected,url,wrapper,label,direct.isEmpty()?null:wrapper));
        }
        return found;
    }
    List<Candidate> discover(JSONObject data)throws Exception{
        season=data.optInt("season",0);episode=data.optInt("episode",0);series=season>0&&episode>0;year=data.optInt("releaseYear",0);
        for(String key:Arrays.asList("movieName","localTitle","originalTitle"))if(!data.optString(key,"").isEmpty())titles.add(data.getString(key));
        if(titles.isEmpty())return Collections.emptyList();
        List<String> order=new ArrayList<>(titles);Collections.reverse(order);
        for(String title:order){
            String base=SITE+(series?"/epi/":"/film/")+normalize(title).replace(' ','-');
            List<String> urls=series?Arrays.asList(base+"-sezonul-"+season+"-episodul-"+episode+"/",base+"-s"+season+"-ep"+episode+"/"):Collections.singletonList(base+"/");
            for(String url:urls){List<Candidate> found=inspect(url);if(!found.isEmpty())return found;}
        }
        for(String title:order){
            Document search=Jsoup.parse(get(SITE+"/?s="+URLEncoder.encode(title,"UTF-8")),SITE);int count=0;
            for(Element a:search.select("article a[href], .search-page a[href]")){
                String url=a.absUrl("href");if(!url.startsWith(SITE+(series?"/epi/":"/film/"))||visited.contains(url))continue;
                if(++count>6)break;List<Candidate> found=inspect(url);if(!found.isEmpty())return found;
            }
        }
        return Collections.emptyList();
    }
    static Candidate resolveWrapper(Candidate candidate)throws Exception{
        if(candidate.embed!=null)return candidate;
        String html=NativeHttp.text(candidate.wrapper,NativeHttp.headers(candidate.page));Document page=Jsoup.parse(html,candidate.wrapper);
        for(Element frame:page.select("iframe[src]")){
            String target=frame.absUrl("src"),actual=kind(target);
            if(!actual.isEmpty())return new Candidate(actual,candidate.page,candidate.wrapper,candidate.label,target);
            if(catalogUrl(target)&&target.contains("trhide=1")){
                Matcher m=Pattern.compile("[?&]tid=([a-fA-F0-9]+)").matcher(target);
                if(m.find()){
                    String hex=new StringBuilder(m.group(1)).reverse().toString();if((hex.length()&1)!=0)continue;
                    byte[] bytes=new byte[hex.length()/2];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)Integer.parseInt(hex.substring(i*2,i*2+2),16);
                    String decoded=new String(bytes,StandardCharsets.UTF_8);actual=kind(decoded);
                    if(!actual.isEmpty())return new Candidate(actual,candidate.page,candidate.wrapper,candidate.label,decoded);
                }
            }
        }
        throw new Exception("No supported host behind wrapper");
    }
}
