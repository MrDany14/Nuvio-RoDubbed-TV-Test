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
        final JSONObject expected;
        Candidate(String host,String page,String wrapper,String label,String embed){this(host,page,wrapper,label,embed,null);}
        Candidate(String host,String page,String wrapper,String label,String embed,JSONObject expected){this.host=host;this.page=page;this.wrapper=wrapper;this.label=label;this.embed=embed;this.expected=expected;}
        String identity()throws Exception{URL u=new URL(embed);String ref=u.getRef();return host+":"+(ref==null?u.getPath():ref.split("&")[0]);}
    }
    final Set<String> visited=new HashSet<>();final Set<String> titles=new LinkedHashSet<>();
    int year,season,episode;boolean series;
    final Set<Integer> verifiedReleaseYears=new HashSet<>();final Set<String> verifiedPosters=new HashSet<>();
    static final class Located {
        final String titleUrl,pageUrl;final Document page;
        Located(String titleUrl,String pageUrl,Document page){this.titleUrl=titleUrl;this.pageUrl=pageUrl;this.page=page;}
    }
    String get(String url)throws Exception{return NativeHttp.text(url,Collections.<String,String>emptyMap());}
    static String normalize(String s){return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
    static String kind(String url){
        try{
            String host=new URL(url).getHost().toLowerCase(Locale.ROOT);
            if(host.matches("(?:[a-z0-9-]+\\.)*byse[a-z0-9-]*\\.[a-z]{2,}"))return "Byse";
            if(host.matches("(?:streamp2p|p2pplay)\\..*"))return "StreamP2P";
            if(host.matches("(?:seekstream(?:ing)?|seeksreaming|embedseek)\\..*"))return "SeekStreaming";
            if(MorenciusClient.playerUrl(url))return "Morencius";
            if(PlaymogoClient.playerUrl(url))return "Playmogo";
            if(host.equals("vidmoly.org")||host.equals("vidmoly.net"))return "Vidmoly";
            if((host.equals("player.abyssplayer.com")||host.equals("play.abyssplayer.com"))&&new URL(url).getPath().matches("/[a-zA-Z0-9_-]{7,17}/?"))return "Abyss";
            if(host.equals("streamtape.com")&&new URL(url).getPath().matches("/e/[a-zA-Z0-9]+/?"))return "Streamtape";
            if(host.equals("voe.sx")||host.equals("voe.to"))return "VOE";
                    }catch(Exception ignored){}return "";
    }
    static String labelKind(String label){String s=normalize(label);if(s.contains("doodstream")||s.contains("playmogo"))return "Playmogo";if(s.contains("byse")||s.contains("filemoon"))return "Byse";if(s.contains("streamp2p")||s.contains("p2pplay"))return "StreamP2P";if(s.contains("seekstream")||s.contains("seeksreaming"))return "SeekStreaming";if(s.replaceFirst("^server \\d+ ?","").matches("voe(?: .*)?"))return "VOE";return "";}
    static boolean catalogUrl(String url){try{URL u=new URL(url);return "https".equals(u.getProtocol())&&"desenefaine.com".equals(u.getHost());}catch(Exception e){return false;}}
    boolean matches(Document page,String url){
        if(!url.startsWith(SITE+(series?"/serial/":"/film/")))return false;
        Set<String> pageNames=new HashSet<>();
        Element heading=page.select("h1").first();
        if(series&&heading!=null&&normalize(heading.text()).matches(".*\\bshorts\\b.*")){
            boolean requested=false;for(String name:titles)if(normalize(name).matches(".*\\bshorts\\b.*"))requested=true;
            if(!requested)return false;
        }
        for(Element h:heading==null?Collections.<Element>emptyList():Collections.singletonList(heading)){
            String name=normalize(h.text());
            pageNames.add(name.replaceFirst(" (?:19|20)\\d{2}$",""));
        }
        Element article=heading==null?null:heading.closest("article.post.single");
        Element details=(article==null?page:article).select(".details-lst").first();
        for(Element li:details==null?Collections.<Element>emptyList():details.select("li")){
            String text=normalize(li.text());
            if(text.startsWith("titlu secundar")||text.startsWith("titlu original")){
                Element last=li.select("span").last();if(last!=null)pageNames.add(normalize(last.text()));
            }
        }
        boolean title=false;for(String name:titles)if(pageNames.contains(normalize(name)))title=true;
        // A publisher's exact movie slug may use the short original name while
        // its heading adds a Romanian subtitle (for example, "Ozzy: ...").
        if(!series&&!title&&heading!=null){
            String raw=heading.text();int colon=raw.indexOf(':');
            if(colon>0){String shortName=normalize(raw.substring(0,colon));String slug=url.replaceFirst("/+$","");slug=slug.substring(slug.lastIndexOf('/')+1);
                for(String name:titles)if(shortName.equals(normalize(name))&&slug.equals(normalize(name).replace(' ','-')))title=true;
            }
        }
        if(!title)return false;
        Element header=heading==null?null:heading.closest(".entry-header");
        Element yearElement=(header==null?(article==null?page:article):header).select(".year").first();
        Matcher m=Pattern.compile("\\b(?:19|20)\\d{2}\\b").matcher(yearElement==null?"":yearElement.text());
        if(year==0)return true;if(!m.find())return false;int siteYear=Integer.parseInt(m.group());
        if(siteYear==year)return true;
        // A premiere can be listed by pilot year rather than regular broadcast
        // year. An adjacent year needs the exact main TMDB artwork as well as
        // the title/type identity checks above; year proximity alone is unsafe.
        if(verifiedReleaseYears.contains(siteYear)||Math.abs(siteYear-year)==1){
            Element image=(article==null?page:article).select("img[src]").first();
            if(image!=null)try{URL poster=new URL(image.absUrl("src").isEmpty()?image.attr("src"):image.absUrl("src"));String path=poster.getPath();
                if("https".equals(poster.getProtocol())&&"image.tmdb.org".equals(poster.getHost())&&verifiedPosters.contains(path.substring(path.lastIndexOf('/')+1)))return true;
            }catch(Exception ignored){}
        }
        return false;
    }
    static String canonical(Document page,String fallback){Element e=page.select("link[rel=canonical]").first();return e!=null&&catalogUrl(e.attr("href"))?e.attr("href"):fallback;}
    static int[] episodeNumber(String url){
        Matcher m=Pattern.compile("(?:-(?:sezonul-?|sezon-?|s)(\\d+)-(?:episodul-?|episoul-?|ep|e)(\\d+)|-(\\d+)x(\\d+))(?:-|/|$)").matcher(url);
        if(!m.find())return null;return new int[]{Integer.parseInt(m.group(1)==null?m.group(3):m.group(1)),Integer.parseInt(m.group(2)==null?m.group(4):m.group(2))};
    }
    Located locateAt(String url)throws Exception{
        if(!catalogUrl(url)||!visited.add(url))return null;
        String html;try{html=get(url);}catch(Exception e){if(e.getMessage()!=null&&e.getMessage().contains("HTTP 404"))return null;throw e;}
        Document page=Jsoup.parse(html,url);if(!matches(page,url))return null;
        String parent=canonical(page,url);if(!parent.startsWith(SITE+(series?"/serial/":"/film/")))return null;
        if(!series)return new Located(parent,parent,page);
        Element h=page.select("h1").first();String parentName=normalize(h.text()).replaceFirst(" (?:19|20)\\d{2}$","");
        for(Element link:page.select(".seasons-lst a[href]")){
            String target=link.absUrl("href");if(!catalogUrl(target)||!target.startsWith(SITE+"/epi/"))continue;
            int[] number=episodeNumber(target);if(number==null||number[0]!=season||number[1]!=episode)continue;
            Document ep=Jsoup.parse(get(target),target);Element epHeading=ep.select("h1").first();if(epHeading==null)return null;
            String name=normalize(epHeading.text()).replaceFirst(" (?:sezonul|sezon|season) \\d+ episodul \\d+.*$","").replaceFirst(" \\d+x\\d+.*$","");
            if(!name.equals(parentName))return null;
            // Episode years may differ from the series premiere year. The
            // verified parent, its own episode list and exact numbering bind it.
            String actual=canonical(ep,target);int[] actualNumber=episodeNumber(actual);
            if(actualNumber==null||actualNumber[0]!=season||actualNumber[1]!=episode)return null;
            return new Located(parent,actual,ep);
        }
        return null;
    }
    List<Candidate> candidates(Located located){
        if(located==null)return Collections.emptyList();Document page=located.page;String url=located.pageUrl;
        List<Candidate> found=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Element server:page.select("a[data-src]")){
            String label=server.text();if(!RomanianAudio.acceptServer(label,page.select("title").text()))continue;
            String wrapper;try{wrapper=new String(ByseClient.decode(server.attr("data-src")),StandardCharsets.UTF_8);}catch(Exception e){continue;}
            String direct=kind(wrapper),expected=direct.isEmpty()?labelKind(label):direct;
            if(expected.isEmpty()||(!catalogUrl(wrapper)&&direct.isEmpty())||!seen.add(wrapper))continue;
            JSONObject context=expected.equals("Playmogo")?new JSONObject().put("dfVerified",true).put("season",season).put("episode",episode):null;
            found.add(new Candidate(expected,url,wrapper,label,direct.isEmpty()?null:wrapper,context));
        }
        return found;
    }
    Located locate(JSONObject data)throws Exception{
        visited.clear();titles.clear();verifiedReleaseYears.clear();verifiedPosters.clear();
        season=data.optInt("season",0);episode=data.optInt("episode",0);series=season>0&&episode>0;year=data.optInt("releaseYear",0);
        for(String key:Arrays.asList("localTitle","movieName","originalTitle"))if(!normalize(data.optString(key,"")).isEmpty())titles.add(data.getString(key));
        Located found=searchTitles(new ArrayList<>(titles));if(found!=null)return found;
        if(data.optInt("tmdbID",0)>0){
            JSONObject english=new JSONObject(NativeHttp.text("https://api.themoviedb.org/3/"+(series?"tv/":"movie/")+data.getInt("tmdbID")+"?language=en-US&api_key="+TMDB_KEY+(series?"":"&append_to_response=release_dates"),Collections.<String,String>emptyMap()));
            String poster=english.optString("poster_path","");if(!poster.isEmpty())verifiedPosters.add(poster.substring(poster.lastIndexOf('/')+1));
            if(series){JSONArray seasons=english.optJSONArray("seasons");if(seasons!=null)for(int i=0;i<seasons.length();i++){JSONObject s=seasons.getJSONObject(i);if(s.optInt("season_number",99)<=1)addReleaseYear(s.optString("air_date",""));}}
            else{JSONObject releases=english.optJSONObject("release_dates");JSONArray countries=releases==null?null:releases.optJSONArray("results");if(countries!=null)for(int i=0;i<countries.length();i++){JSONArray dates=countries.getJSONObject(i).optJSONArray("release_dates");if(dates!=null)for(int n=0;n<dates.length();n++)addReleaseYear(dates.getJSONObject(n).optString("release_date",""));}
                boolean alternate=false;for(int known:verifiedReleaseYears)if(known!=year)alternate=true;
                if(alternate){JSONObject artwork=new JSONObject(NativeHttp.text("https://api.themoviedb.org/3/movie/"+data.getInt("tmdbID")+"/images?api_key="+TMDB_KEY,Collections.<String,String>emptyMap()));JSONArray posters=artwork.optJSONArray("posters");if(posters!=null)for(int i=0;i<posters.length();i++){String file=posters.getJSONObject(i).optString("file_path","");if(!file.isEmpty())verifiedPosters.add(file.substring(file.lastIndexOf('/')+1));}}
            }
            String title=english.optString(series?"name":"title");LinkedHashSet<String> retry=new LinkedHashSet<>();if(!normalize(title).isEmpty()){titles.add(title);retry.add(title);}retry.addAll(titles);
            visited.clear();return searchTitles(new ArrayList<>(retry));
        }
        return null;
    }
    void addReleaseYear(String date){if(date.matches("(?:19|20)\\d{2}-.*"))verifiedReleaseYears.add(Integer.parseInt(date.substring(0,4)));}
    Located searchTitles(List<String> order)throws Exception{
        for(String title:order){Located found=locateAt(SITE+(series?"/serial/":"/film/")+normalize(title).replace(' ','-')+"/");if(found!=null)return found;}
        for(String title:order){
            Document search=Jsoup.parse(get(SITE+"/?s="+URLEncoder.encode(title,"UTF-8")),SITE);int count=0;
            for(Element a:search.select("article a[href], .search-page a[href]")){
                String url=a.absUrl("href");if(!url.startsWith(SITE+(series?"/serial/":"/film/"))||visited.contains(url))continue;
                if(++count>8)break;Located found=locateAt(url);if(found!=null)return found;
            }
        }
        return null;
    }
    List<Candidate> discover(JSONObject data)throws Exception{return candidates(locate(data));}
    static Candidate resolveWrapper(Candidate candidate)throws Exception{
        if(candidate.embed!=null)return candidate;
        String html=NativeHttp.text(candidate.wrapper,NativeHttp.headers(candidate.page));Document page=Jsoup.parse(html,candidate.wrapper);
        for(Element frame:page.select("iframe[src]")){
            String target=frame.absUrl("src"),actual=kind(target);
            if(!actual.isEmpty())return new Candidate(actual,candidate.page,candidate.wrapper,candidate.label,target,candidate.expected);
            if(catalogUrl(target)&&target.contains("trhide=1")){
                Matcher m=Pattern.compile("[?&]tid=([a-fA-F0-9]+)").matcher(target);
                if(m.find()){
                    String hex=new StringBuilder(m.group(1)).reverse().toString();if((hex.length()&1)!=0)continue;
                    byte[] bytes=new byte[hex.length()/2];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)Integer.parseInt(hex.substring(i*2,i*2+2),16);
                    String decoded=new String(bytes,StandardCharsets.UTF_8);actual=kind(decoded);
                    if(!actual.isEmpty())return new Candidate(actual,candidate.page,candidate.wrapper,candidate.label,decoded,candidate.expected);
                }
            }
        }
        throw new Exception("No supported host behind wrapper");
    }
}
