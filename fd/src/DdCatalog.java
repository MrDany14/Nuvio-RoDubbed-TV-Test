import org.json.*;import org.jsoup.Jsoup;import org.jsoup.nodes.*;import java.util.*;import java.net.*;import java.util.regex.*;import java.nio.charset.StandardCharsets;
final class DdCatalog {
 static final String SITE="https://deseneledublate.com";
 static boolean siteUrl(String url){try{URL u=new URL(url);return "https".equals(u.getProtocol())&&"deseneledublate.com".equals(u.getHost());}catch(Exception e){return false;}}
 static String heading(Document page){Element h=page.select("h1").first();return h==null?"":h.text();}
 static String field(Document page,String name){for(Element row:page.select("#info .custom_fields"))if(NativeCatalog.normalize(row.select("b.variante").text()).equals(name))return row.select("span.valor").text();return "";}
 static String seriesName(String title){return title.replaceFirst("\\s*\\((?:19|20)\\d{2}\\)","").replaceFirst("(?i)\\s+(?:Seria\\s+)?Dublat[ăa]?\\s+[îi]n\\s+Rom[âa]n[ăa].*$","").trim();}
 static boolean matches(Document page,JSONObject data){
  String h=heading(page),normalized=NativeCatalog.normalize(h);if(!RomanianAudio.declaredRomanian(h)||!normalized.matches(".*\\bdublat(?:a)? in romana\\b.*"))return false;
  Matcher year=Pattern.compile("\\b((?:19|20)\\d{2})\\b").matcher(field(page,"first air date"));if(!year.find()||Integer.parseInt(year.group(1))!=data.optInt("releaseYear"))return false;
  String name=NativeCatalog.normalize(seriesName(h)),original=NativeCatalog.normalize(field(page,"original title"));
  for(String key:Arrays.asList("localTitle","movieName","originalTitle","englishTitle")){String expected=NativeCatalog.normalize(data.optString(key));if(!expected.isEmpty()&&(expected.equals(name)||expected.equals(original)))return true;}return false;
 }
 static final class Located {final String titleUrl,pageUrl,episodeTitle,seriesTitle;final Document page;Located(String t,String p,String e,String s,Document d){titleUrl=t;pageUrl=p;episodeTitle=e;seriesTitle=s;page=d;}}
 Located locateAt(String url,JSONObject data)throws Exception{
  if(!siteUrl(url)||!new URL(url).getPath().startsWith("/serial/"))return null;
  Document parent=Jsoup.parse(NativeHttp.text(url,Collections.<String,String>emptyMap()),url);if(!matches(parent,data))return null;
  int season=data.optInt("season"),episode=data.optInt("episode");if(season<=0||episode<=0)return null;
  for(Element a:parent.select("#seasons .episodiotitle a[href]")){
   String target=a.absUrl("href");if(!siteUrl(target)||!new URL(target).getPath().startsWith("/episoade/")||!target.matches(".*-sezonul-"+season+"-episodul-"+episode+"(?:-|/|$).*"))continue;
   Document ep=Jsoup.parse(NativeHttp.text(target,NativeHttp.headers(url)),target);String epHeading=NativeCatalog.normalize(heading(ep)),prefix=NativeCatalog.normalize(heading(parent));
   if(!epHeading.equals(prefix+" "+season+"x"+episode)||!RomanianAudio.declaredRomanian(heading(ep)))return null;
   return new Located(url,target,a.text(),seriesName(heading(parent)),ep);
  }return null;
 }
 Located locate(JSONObject data)throws Exception{
  if(data.optInt("season")<=0||data.optInt("episode")<=0)return null;
  LinkedHashSet<String> titles=new LinkedHashSet<>();for(String key:Arrays.asList("localTitle","movieName","originalTitle"))if(!data.optString(key).isEmpty())titles.add(data.optString(key));
  Set<String> seen=new HashSet<>();JSONObject identity=new JSONObject(data.toString());
  for(int phase=0;phase<2;phase++){
   if(phase==1){if(data.optInt("tmdbID")>0){JSONObject english=new JSONObject(NativeHttp.text("https://api.themoviedb.org/3/tv/"+data.getInt("tmdbID")+"?language=en-US&api_key="+NativeCatalog.TMDB_KEY,Collections.<String,String>emptyMap()));String name=english.optString("name");identity.put("englishTitle",name);if(!name.isEmpty())titles.add(name);}List<String> full=new ArrayList<>(titles);for(String title:full){String[] words=title.split("\\s+");if(words.length>1){if(words[0].length()>=4)titles.add(words[0]);else if(words[0].length()+words[1].length()>=6)titles.add(words[0]+" "+words[1]);}}seen.clear();}
   int queries=0;for(String title:titles){if(++queries>5)break;String search=SITE+"/?s="+URLEncoder.encode(title,"UTF-8");Document page=Jsoup.parse(NativeHttp.text(search,Collections.<String,String>emptyMap()),search);int checked=0;
    for(Element a:page.select(".result-item .title a[href], article.item .data h3 a[href]")){String target=a.absUrl("href");if(!siteUrl(target)||!target.contains("/serial/")||!seen.add(target)||++checked>8)continue;Located found=locateAt(target,identity);if(found!=null)return found;}
   }
  }return null;
 }
 List<NativeCatalog.Candidate> discover(JSONObject data)throws Exception{
  Located located=locate(data);if(located==null)return Collections.emptyList();List<NativeCatalog.Candidate> found=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(Element option:located.page.select(".dooplay_player_option[data-post][data-type][data-nume]")){
   // This site has mislabeled English Streamtape copies. Only the confirmed OK.ru family is enabled.
   String label=option.text().toLowerCase(Locale.ROOT);if(!RomanianAudio.declaredRomanian("ROMANA "+label)||label.matches(".*(?:netu|hqq|waaw|trailer|streamtape|sendvid|vidmoly).*"))continue;
   Map<String,String> headers=NativeHttp.headers(located.pageUrl);headers.put("Content-Type","application/x-www-form-urlencoded");String body="action=doo_player_ajax&post="+URLEncoder.encode(option.attr("data-post"),"UTF-8")+"&nume="+URLEncoder.encode(option.attr("data-nume"),"UTF-8")+"&type="+URLEncoder.encode(option.attr("data-type"),"UTF-8");
   JSONObject response=new JSONObject(new String(NativeHttp.request(SITE+"/wp-admin/admin-ajax.php",headers,262144,false,body),StandardCharsets.UTF_8));String raw=response.optString("embed_url");String iframe=Jsoup.parse(raw,located.pageUrl).select("iframe[src]").attr("abs:src");String embed=iframe.isEmpty()?raw:iframe;
   URL u;try{u=new URL(new URL(located.pageUrl),embed);}catch(Exception e){continue;}if(!"https".equals(u.getProtocol())||!NativeCatalog.kind(u.toString()).equals("OK.ru")||!seen.add(u.toString()))continue;
   JSONObject expected=new JSONObject().put("season",data.getInt("season")).put("episode",data.getInt("episode")).put("seriesTitle",located.seriesTitle).put("episodeTitle",located.episodeTitle);
   found.add(new NativeCatalog.Candidate("OK.ru",located.pageUrl,located.pageUrl,"ROMANA",u.toString(),expected));
  }return found;
 }
}
