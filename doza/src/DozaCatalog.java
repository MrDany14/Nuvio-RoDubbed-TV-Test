import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

final class DozaCatalog {
    static final String SITE="https://www.dozaanimata.net";
    static boolean siteUrl(String value){try{URL u=new URL(value);return "https".equals(u.getProtocol())&&"www.dozaanimata.net".equals(u.getHost());}catch(Exception e){return false;}}
    static boolean dubbed(String heading){String n=NativeCatalog.normalize(heading);return RomanianAudio.declaredRomanian(heading)&&n.matches(".*\\bdublat(?:a)? in romana\\b.*");}
    static String heading(Document doc){Element h=doc.select(".mvic-desc h3[itemprop=name]").first();return h==null?"":h.text();}
    static boolean matches(Document doc,JSONObject data){
        String h=heading(doc);if(!dubbed(h))return false;
        Matcher year=Pattern.compile("\\((\\d{4})\\)").matcher(h);
        if(data.optInt("releaseYear",0)>0&&(!year.find()||Integer.parseInt(year.group(1))!=data.getInt("releaseYear")))return false;
        String title=NativeCatalog.normalize(h.replaceFirst("\\s*\\((?:19|20)\\d{2}\\).*$","").replaceFirst("(?i)\\s*[–-]\\s*Dublat.*$",""));
        for(String key:Arrays.asList("localTitle","movieName","originalTitle"))if(!data.optString(key).isEmpty()&&title.equals(NativeCatalog.normalize(data.optString(key))))return true;
        return false;
    }
    static List<NativeCatalog.Candidate> players(Document page,String url){
        List<NativeCatalog.Candidate> found=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Element iframe:page.select("#content-embed iframe[src], #media-player iframe[src]")){
            String embed=iframe.absUrl("src"),kind=NativeCatalog.kind(embed);
            if(!kind.isEmpty()&&seen.add(embed))found.add(new NativeCatalog.Candidate(kind,url,embed,"ROMANA",embed));
        }
        return found;
    }
    List<NativeCatalog.Candidate> inspect(String url,JSONObject data)throws Exception{
        if(!siteUrl(url))return Collections.emptyList();
        Document page=Jsoup.parse(NativeHttp.text(url,Collections.<String,String>emptyMap()),url);
        if(!matches(page,data))return Collections.emptyList();
        int season=data.optInt("season",0),episode=data.optInt("episode",0);
        if(season<=0||episode<=0)return players(page,url);
        String pattern=".*-sezonul-"+season+"-episodul-"+episode+"(?:-|/|$).*";
        for(Element a:page.select("a[href]")){
            String target=a.absUrl("href");if(!siteUrl(target)||!target.matches(pattern))continue;
            Document ep=Jsoup.parse(NativeHttp.text(target,NativeHttp.headers(url)),target);
            String seriesTitle=NativeCatalog.normalize(heading(page).replaceFirst("\\s*\\((?:19|20)\\d{2}\\).*$",""));
            String episodeTitle=NativeCatalog.normalize(heading(ep)).replaceFirst(" sezonul \\d+ episodul \\d+.*$","");
            if(!seriesTitle.equals(episodeTitle)||!RomanianAudio.declaredRomanian("ROMANA "+heading(ep)))return Collections.emptyList();
            return players(ep,target);
        }
        return Collections.emptyList();
    }
    List<NativeCatalog.Candidate> discover(JSONObject data)throws Exception{
        Set<String> visited=new HashSet<>(),titles=new LinkedHashSet<>();
        for(String key:Arrays.asList("localTitle","movieName","originalTitle"))if(!data.optString(key).isEmpty())titles.add(data.optString(key));
        for(String title:titles){
            String url=SITE+"/?s="+URLEncoder.encode(title,"UTF-8");Document search=Jsoup.parse(NativeHttp.text(url,Collections.<String,String>emptyMap()),url);int tried=0;
            for(Element a:search.select(".ml-item > a[href]")){
                String target=a.absUrl("href");if(!siteUrl(target)||target.contains("/episode/")||!visited.add(target))continue;
                // The visible result must declare dubbing before any detail/player fetch.
                if(!dubbed(a.text())||++tried>5)continue;
                List<NativeCatalog.Candidate> found=inspect(target,data);if(!found.isEmpty())return found;
            }
        }
        return Collections.emptyList();
    }
}
