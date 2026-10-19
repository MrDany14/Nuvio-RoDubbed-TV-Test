import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.jsoup.parser.Parser;

final class DozaCatalog {
    static final class Located {
        final String titleUrl,pageUrl;final Document page;
        Located(String titleUrl,String pageUrl,Document page){this.titleUrl=titleUrl;this.pageUrl=pageUrl;this.page=page;}
    }
    static final String SITE="https://www.dozaanimata.net";
    static final Map<String,List<String>> SITEMAPS=new HashMap<>();
    static long sitemapTime;
    static boolean siteUrl(String value){try{URL u=new URL(value);return "https".equals(u.getProtocol())&&"www.dozaanimata.net".equals(u.getHost());}catch(Exception e){return false;}}
    static boolean dubbed(String heading){String n=NativeCatalog.normalize(heading);return RomanianAudio.declaredRomanian(heading)&&n.matches(".*\\bdublat(?:a)? in romana\\b.*");}
    static String heading(Document doc){Element h=doc.select(".mvic-desc h3[itemprop=name]").first();return h==null?"":h.text();}
    static boolean matches(Document doc,JSONObject data){
        String h=heading(doc);if(!dubbed(h))return false;
        boolean artwork=verifiedArtwork(doc,data);
        Matcher year=Pattern.compile("\\((\\d{4})\\)").matcher(h);
        if(data.optInt("releaseYear",0)>0&&(!year.find()||Integer.parseInt(year.group(1))!=data.getInt("releaseYear"))){
            // A publisher can date an adaptation by its source release. Require
            // exact TMDB artwork, title overlap and two independently named cast members.
            if(!artwork||!corroboratedCast(doc,data)||score(h,data)<6)return false;
        }
        String title=NativeCatalog.normalize(h.replaceFirst("\\s*\\((?:19|20)\\d{2}\\).*$","").replaceFirst("(?i)\\s*[–-]\\s*Dublat.*$",""));
        for(String key:Arrays.asList("localTitle","movieName","originalTitle"))if(!data.optString(key).isEmpty()&&title.equals(NativeCatalog.normalize(data.optString(key))))return true;
        if(artwork)return true;
        // Older posts keep their original title in the main artwork filename.
        // Require that identity plus the already-checked release year and dubbing declaration.
        Element mainImage=doc.select("img.hidden[itemprop=image][src]").first();
        for(Element image:mainImage==null?Collections.<Element>emptyList():Collections.singletonList(mainImage)){
            if(!image.attr("title").equals(h)&&!image.attr("alt").equals(h))continue;
            String src=image.attr("src");if(!src.startsWith(SITE+"/wp-content/uploads/"))continue;
            String name=src.substring(src.lastIndexOf('/')+1).replaceFirst("\\.[a-zA-Z0-9]+$","").replaceFirst("-\\d+x\\d+$","");
            String alias=NativeCatalog.normalize(name),compact=alias.replace(" ","");
            for(String key:Arrays.asList("originalTitle","englishTitle","localTitle")){
                String full=NativeCatalog.normalize(data.optString(key)).replace(" ","");
                if(compact.length()>=8&&compact.equals(full))return true;
                int distinctive=0;for(String word:alias.split(" "))if(word.length()>3&&!word.matches("the|and|with|from|live"))distinctive++;
                if(alias.split(" ").length>=3&&distinctive>=2&&compact.length()>=14&&full.startsWith(compact))return true;
            }
        }
        return false;
    }
    static boolean verifiedArtwork(Document doc,JSONObject data){
        Set<String> verified=new HashSet<>();JSONArray images=data.optJSONArray("verifiedArtwork");if(images==null)return false;
        for(int i=0;i<images.length();i++)verified.add(images.optString(i));
        // Only the current article's structured thumbnail, never related cards.
        for(Element image:doc.select(".mvic-info meta[itemprop=thumbnailUrl][content]"))try{
            URL u=new URL(image.attr("content"));String file=u.getPath().substring(u.getPath().lastIndexOf('/')+1);
            if("https".equals(u.getProtocol())&&"image.tmdb.org".equals(u.getHost())&&verified.contains(file))return true;
        }catch(Exception ignored){}
        return false;
    }
    static boolean corroboratedCast(Document doc,JSONObject data){
        Set<String> names=new HashSet<>();JSONArray cast=data.optJSONArray("verifiedCast");if(cast==null)return false;
        for(int i=0;i<cast.length();i++)names.add(NativeCatalog.normalize(cast.optString(i)));
        Set<String> common=new HashSet<>();for(Element a:doc.select(".mvic-info a[href*=/stars/]"))if(names.contains(NativeCatalog.normalize(a.text())))common.add(NativeCatalog.normalize(a.text()));
        return common.size()>=2;
    }
    static boolean episodeAddress(String url,int season,int episode){
        if(url.matches(".*-sezonul-"+season+"-episodul-"+episode+"(?:-|/|$).*"))return true;
        return season==1&&!url.contains("-sezonul-")&&url.matches(".*-episodul-"+episode+"(?:-|/|$).*");
    }
    static String segmentedEpisode(Document page,JSONObject data)throws Exception{
        int season=data.optInt("season"),episode=data.optInt("episode"),firstSegment=Integer.MAX_VALUE;TreeMap<Integer,String> ordered=new TreeMap<>();
        for(Element a:page.select("a[href]")){
            String url=a.absUrl("href");if(!siteUrl(url))continue;
            Matcher label=Pattern.compile("episodul (\\d+)(?: (\\d+))?").matcher(NativeCatalog.normalize(a.text()));
            if(!label.matches())continue;
            int group=Integer.parseInt(label.group(1)),part=label.group(2)==null?0:Integer.parseInt(label.group(2));
            if(!url.matches(".*-sezonul-"+season+"-episodul-"+group+(part>0?"-"+part:"")+"(?:-|/|$).*"))continue;
            ordered.put(group*100+part,url);if(part>0)firstSegment=Math.min(firstSegment,group);
        }
        // Ordinary episodes preceding the first split retain the same number.
        if(firstSegment==Integer.MAX_VALUE||episode<firstSegment)return "";
        // Segmented broadcast numbering is not TMDB numbering. Only use the
        // complete ordered season when independent episode names confirm it.
        if(data.optInt("tmdbID")==0)return "!";
        JSONObject tmdb=new JSONObject(NativeHttp.text("https://api.themoviedb.org/3/tv/"+data.getInt("tmdbID")+"/season/"+season+"?language=en-US&api_key="+NativeCatalog.TMDB_KEY,Collections.<String,String>emptyMap()));
        return selectSegmented(ordered,tmdb.optJSONArray("episodes"),episode);
    }
    static String selectSegmented(SortedMap<Integer,String> ordered,JSONArray episodes,int episode){
        if(episodes==null||episodes.length()!=ordered.size()||episode<1||episode>episodes.length())return "!";
        int anchors=0,index=0;String selected="";
        for(String url:ordered.values()){
            JSONObject item=episodes.getJSONObject(index);if(item.optInt("episode_number")!=index+1)return "!";
            String name=NativeCatalog.normalize(item.optString("name"));if(name.length()>=8&&NativeCatalog.normalize(url).contains(name))anchors++;
            if(++index==episode)selected=url;
        }
        return anchors>=2?selected:"!";
    }
    static List<NativeCatalog.Candidate> players(Document page,String url){
        List<NativeCatalog.Candidate> found=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Element iframe:page.select("#content-embed iframe[src], #media-player iframe[src]")){
            String embed=iframe.absUrl("src"),kind=NativeCatalog.kind(embed);
            if(!kind.isEmpty()&&seen.add(embed))found.add(new NativeCatalog.Candidate(kind,url,embed,"ROMANA",embed));
        }
        return found;
    }
    Located locateAt(String url,JSONObject data)throws Exception{
        if(!siteUrl(url))return null;
        boolean series=data.optInt("season",0)>0;if(series!=new URL(url).getPath().startsWith("/series/"))return null;
        Document page=Jsoup.parse(NativeHttp.text(url,Collections.<String,String>emptyMap()),url);
        if(!matches(page,data))return null;
        int season=data.optInt("season",0),episode=data.optInt("episode",0);
        if(season<=0||episode<=0)return new Located(url,url,page);
        String segmented=segmentedEpisode(page,data);if(segmented.equals("!"))return null;
        for(Element a:page.select("a[href]")){
            String target=a.absUrl("href");if(!siteUrl(target)||!new URL(target).getPath().startsWith("/episode/")||(segmented.isEmpty()?!episodeAddress(target,season,episode):!target.equals(segmented)))continue;
            Document ep=Jsoup.parse(NativeHttp.text(target,NativeHttp.headers(url)),target);
            String seriesTitle=NativeCatalog.normalize(heading(page).replaceFirst("\\s*\\((?:19|20)\\d{2}\\).*$",""));
            String episodeTitle=NativeCatalog.normalize(heading(ep)).replaceFirst(" (?:sezonul \\d+ )?episodul \\d+.*$","");
            if(!seriesTitle.equals(episodeTitle)||!RomanianAudio.declaredRomanian("ROMANA "+heading(ep)))return null;
            return new Located(url,target,ep);
        }
        return null;
    }
    List<NativeCatalog.Candidate> inspect(String url,JSONObject data)throws Exception{Located located=locateAt(url,data);return located==null?Collections.<NativeCatalog.Candidate>emptyList():players(located.page,located.pageUrl);}
    Located locate(JSONObject data)throws Exception{
        Set<String> visited=new LinkedHashSet<>(),titles=new LinkedHashSet<>();
        for(String key:Arrays.asList("localTitle","movieName","originalTitle"))if(!data.optString(key).isEmpty())titles.add(data.optString(key));
        for(String title:titles){
            String url=SITE+"/?s="+URLEncoder.encode(title,"UTF-8");Document search=Jsoup.parse(NativeHttp.text(url,Collections.<String,String>emptyMap()),url);int tried=0;
            for(Element a:search.select(".ml-item > a[href]")){
                String target=a.absUrl("href");if(!siteUrl(target)||target.contains("/episode/")||!visited.add(target))continue;
                // The visible result must declare dubbing before any detail/player fetch.
                if(!dubbed(a.text())||++tried>5)continue;
                Located found=locateAt(target,data);if(found!=null)return found;
            }
        }
        JSONObject identity=new JSONObject(data.toString());
        boolean series=data.optInt("season",0)>0;
        if(data.optInt("tmdbID",0)>0){
            JSONObject english=new JSONObject(NativeHttp.text("https://api.themoviedb.org/3/"+(series?"tv/":"movie/")+data.getInt("tmdbID")+"?language=en-US&append_to_response=credits,images&include_image_language=en,ro,null&api_key="+NativeCatalog.TMDB_KEY,Collections.<String,String>emptyMap()));
            identity.put("englishTitle",english.optString(series?"name":"title"));
            JSONArray artwork=new JSONArray(),cast=new JSONArray();
            for(String key:Arrays.asList("poster_path","backdrop_path")){String path=english.optString(key,"");if(!path.isEmpty())artwork.put(path.substring(path.lastIndexOf('/')+1));}
            JSONObject images=english.optJSONObject("images");if(images!=null)for(String key:Arrays.asList("posters","backdrops")){
                JSONArray list=images.optJSONArray(key);if(list!=null)for(int i=0;i<list.length();i++){String path=list.getJSONObject(i).optString("file_path");if(!path.isEmpty())artwork.put(path.substring(path.lastIndexOf('/')+1));}
            }
            JSONArray people=english.optJSONObject("credits")==null?null:english.getJSONObject("credits").optJSONArray("cast");
            if(people!=null)for(int i=0;i<people.length();i++)cast.put(people.getJSONObject(i).optString("name"));
            identity.put("verifiedArtwork",artwork).put("verifiedCast",cast);
        }
        // Retry search hits with the enriched independent identity. Previously
        // visited pages must not be discarded before this stronger comparison.
        for(String candidate:visited){Located found=locateAt(candidate,identity);if(found!=null)return found;}
        // WordPress text search omits taxonomy labels, although Doza often
        // preserves the original/English movie name as a public tag.
        Set<String> aliases=new LinkedHashSet<>();
        for(String key:Arrays.asList("englishTitle","originalTitle"))if(!NativeCatalog.normalize(identity.optString(key)).isEmpty())aliases.add(identity.optString(key));
        for(String alias:aliases){
            JSONArray tags=new JSONArray(NativeHttp.text(SITE+"/wp-json/wp/v2/tags?per_page=3&_fields=link,name&search="+URLEncoder.encode(alias,"UTF-8"),Collections.<String,String>emptyMap()));
            for(int i=0;i<tags.length();i++){
                String tag=tags.getJSONObject(i).optString("link");if(!siteUrl(tag)||!new URL(tag).getPath().startsWith("/tag/"))continue;
                Document archive=Jsoup.parse(NativeHttp.text(tag,Collections.<String,String>emptyMap()),tag);int checked=0;
                for(Element a:archive.select(".ml-item > a[href]")){
                    String target=a.absUrl("href");if(!siteUrl(target)||target.contains("/episode/")||!dubbed(a.text())||++checked>5)continue;
                    Located found=locateAt(target,identity);if(found!=null)return found;
                }
            }
        }
        List<String> urls=catalogUrls(series);List<String> candidates=new ArrayList<>();
        for(String url:urls)if(url.contains("-dublat-in-romana"))candidates.add(url);
        final JSONObject ranking=identity;
        Collections.sort(candidates,new Comparator<String>(){public int compare(String a,String b){return Integer.compare(score(b,ranking),score(a,ranking));}});
        int checked=0;
        for(String candidate:candidates){
            if(visited.contains(candidate)||++checked>16)continue;
            Located found=locateAt(candidate,identity);if(found!=null)return found;
        }
        return null;
    }
    static int score(String url,JSONObject data){
        String slug=NativeCatalog.normalize(url);int score=url.contains("-"+data.optInt("releaseYear",0)+"-")?20:0;
        for(String key:Arrays.asList("localTitle","originalTitle","englishTitle"))for(String word:NativeCatalog.normalize(data.optString(key)).split(" "))if(word.length()>3&&slug.contains(word))score+=word.length();return score;
    }
    static synchronized List<String> catalogUrls(boolean series)throws Exception{
        String type=series?"tv":"movie";
        if(System.currentTimeMillis()-sitemapTime>3600000){SITEMAPS.clear();sitemapTime=System.currentTimeMillis();}
        if(SITEMAPS.containsKey(type))return SITEMAPS.get(type);
        Document index=Jsoup.parse(NativeHttp.text(SITE+"/sitemap_index.xml",Collections.<String,String>emptyMap()),"",Parser.xmlParser());List<String> urls=new ArrayList<>();
        for(Element loc:index.select("sitemap > loc")){
            String file=loc.text();if(!siteUrl(file)||!file.matches(series?".*/tvshows-sitemap\\d*\\.xml":".*/post-sitemap\\d*\\.xml"))continue;
            Document map=Jsoup.parse(NativeHttp.text(file,Collections.<String,String>emptyMap()),"",Parser.xmlParser());
            for(Element entry:map.select("url > loc")){String target=entry.text();if(siteUrl(target)&&(series==target.contains("/series/")))urls.add(target);}
        }
        SITEMAPS.put(type,urls);return urls;
    }
    List<NativeCatalog.Candidate> discover(JSONObject data)throws Exception{Located located=locate(data);return located==null?Collections.<NativeCatalog.Candidate>emptyList():players(located.page,located.pageUrl);}
}
