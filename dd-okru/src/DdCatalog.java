import org.json.*;import org.jsoup.Jsoup;import org.jsoup.nodes.*;import java.util.*;import java.net.*;import java.util.regex.*;import java.nio.charset.StandardCharsets;
final class DdCatalog {
 static final String SITE=DdTitleCatalog.SITE;
 List<NativeCatalog.Candidate> discover(JSONObject data)throws Exception{String url=new DdTitleCatalog().locate(data);System.out.println("[DD] title="+url);return at(url,data);}
 private List<NativeCatalog.Candidate> at(String titleUrl,JSONObject data)throws Exception{
  if(titleUrl==null||!DdTitleCatalog.siteUrl(titleUrl))return Collections.emptyList();
  Document parent=Jsoup.parse(NativeHttp.text(titleUrl,Collections.<String,String>emptyMap()),titleUrl);
  // locate already checked this exact parent with independently enriched metadata.
  // Requiring the original, unenriched payload here would discard those matches.
  if(!DdTitleCatalog.seasonMatches(parent,data))return Collections.emptyList();
  boolean series=DdTitleCatalog.series(data);String pageUrl=titleUrl,episodeTitle="";Document page=parent;
  if(series){int season=data.optInt("season"),episode=data.optInt("episode");if(season<=0||episode<=0)return Collections.emptyList();String target=null;
   for(Element a:parent.select("#seasons .episodiotitle a[href]")){String url=a.absUrl("href");Matcher m=Pattern.compile("/episoade/[^/]*-sezonul-(\\d+)-episodul-(\\d+)(?:-|/|$|(?=dublat))").matcher(DdTitleCatalog.newPath(url));if(DdTitleCatalog.siteUrl(url)&&m.find()&&Integer.parseInt(m.group(1))==season&&Integer.parseInt(m.group(2))==episode){target=url;episodeTitle=a.text();break;}}
   if(target==null)return Collections.emptyList();pageUrl=target;page=Jsoup.parse(NativeHttp.text(target,NativeHttp.headers(titleUrl)),target);
   String h=NativeCatalog.normalize(DdTitleCatalog.title(page));if(!h.equals(NativeCatalog.normalize(DdTitleCatalog.title(parent))+" "+season+"x"+episode)){System.out.println("[DD] heading rejected="+h);return Collections.emptyList();}
  }
  JSONObject expected=new JSONObject().put("season",data.optInt("season")).put("episode",data.optInt("episode")).put("seriesTitle",DdTitleCatalog.titleName(DdTitleCatalog.title(parent))).put("originalTitle",DdTitleCatalog.field(parent,"original title")).put("episodeTitle",episodeTitle);
  if(!series){JSONObject tmdb=new JSONObject(NativeHttp.text("https://api.themoviedb.org/3/movie/"+data.getInt("tmdbID")+"?api_key="+NativeCatalog.TMDB_KEY,Collections.<String,String>emptyMap()));if(tmdb.optInt("runtime")<10)return Collections.emptyList();expected.put("runtime",tmdb.getInt("runtime"));}
  List<NativeCatalog.Candidate> found=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(Element option:page.select(".dooplay_player_option[data-post][data-type][data-nume]")){
   String label=NativeCatalog.normalize(option.text());if(!RomanianAudio.declaredRomanian("ROMANA "+label)||label.matches(".*(?:netu|hqq|waaw|trailer|streamtape|sendvid|vidmoly).*"))continue;
   Map<String,String> headers=NativeHttp.headers(pageUrl);headers.put("Content-Type","application/x-www-form-urlencoded");String body="action=doo_player_ajax&post="+URLEncoder.encode(option.attr("data-post"),"UTF-8")+"&nume="+URLEncoder.encode(option.attr("data-nume"),"UTF-8")+"&type="+URLEncoder.encode(option.attr("data-type"),"UTF-8");
   JSONObject response=new JSONObject(new String(NativeHttp.request(SITE+"/wp-admin/admin-ajax.php",headers,262144,false,body),StandardCharsets.UTF_8));String raw=response.optString("embed_url"),iframe=Jsoup.parse(raw,pageUrl).select("iframe[src]").attr("abs:src");String embed=OkruClient.embedUrl(iframe.isEmpty()?raw:iframe);
   if(embed==null&&raw.startsWith("//"))embed=OkruClient.embedUrl("https:"+raw);
   if(embed!=null&&seen.add(embed))found.add(new NativeCatalog.Candidate("OK.ru",pageUrl,pageUrl,"ROMANA",embed,expected));
  }return found;
 }
}
