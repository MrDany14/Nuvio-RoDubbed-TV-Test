import org.json.*;import org.jsoup.Jsoup;import org.jsoup.nodes.*;import java.net.*;import java.util.*;import java.util.regex.*;
final class FdCatalog {
 static final String SITE="https://filmedublate.net";
 static boolean siteUrl(String text){try{URL u=new URL(text);return "https".equals(u.getProtocol())&&"filmedublate.net".equals(u.getHost())&&u.getUserInfo()==null;}catch(Exception e){return false;}}
 static boolean titleUrl(String text){if(!siteUrl(text))return false;try{return new URL(text).getPath().matches("/(?:film-dublat|desen-animat|serial-dublat)/[a-z0-9-]+-dublat-in-romana/?");}catch(Exception e){return false;}}
 static int runtime(String text){Matcher h=Pattern.compile("\\b(\\d+)h\\b").matcher(text),m=Pattern.compile("\\b(\\d+)m\\b").matcher(text);return ((h.find()?Integer.parseInt(h.group(1))*60:0)+(m.find()?Integer.parseInt(m.group(1)):0))*60;}
 static boolean matches(Document page,JSONObject data){
  Element h=page.select("h1.film-title").first();if(h==null)return false;
  String declaration=page.title()+" "+page.select("meta[name=description]").attr("content");if(!RomanianAudio.declaredRomanian(declaration))return false;
  String date=page.select(".meta-row > .meta-item").first()==null?"":page.select(".meta-row > .meta-item").first().text();if(!date.trim().equals(String.valueOf(data.optInt("releaseYear"))))return false;
  Set<String> names=new HashSet<>();names.add(NativeCatalog.normalize(h.text()));names.add(NativeCatalog.normalize(page.select("p.orig-title").first()==null?"":page.select("p.orig-title").first().text()));
  for(String key:Arrays.asList("localTitle","movieName","originalTitle","englishTitle")){String name=NativeCatalog.normalize(data.optString(key));if(!name.isEmpty()&&names.contains(name))return true;}return false;
 }
 static String wrapper(Document page,JSONObject data)throws Exception{
  int season=data.optInt("season"),episode=data.optInt("episode");
  if(season>0&&episode>0){for(Element button:page.select("button.ep-btn[onclick][title]")){
   if(!button.attr("title").matches("S0*"+season+"E0*"+episode))continue;
   Matcher call=Pattern.compile("^playEp\\('([a-z0-9-]+-s0*"+season+"e0*"+episode+")',\\s*this\\)$").matcher(button.attr("onclick"));if(call.find())return SITE+"/embed/filmsrv.php?search="+URLEncoder.encode(call.group(1),"UTF-8");
  }return "";}
  // A series' default iframe must not qualify as a movie.
  if(!page.select("button.ep-btn").isEmpty())return "";
  for(Element frame:page.select("iframe[src]")){String target=frame.absUrl("src");if(siteUrl(target)&&new URL(target).getPath().equals("/embed/filmsrv.php"))return target;}return "";
 }
 static final class Located {final String titleUrl,wrapper;final Document page;Located(String t,String w,Document p){titleUrl=t;wrapper=w;page=p;}}
 Located locateAt(String url,JSONObject data)throws Exception{
  if(!titleUrl(url))return null;Document page=Jsoup.parse(NativeHttp.text(url,Collections.<String,String>emptyMap()),url);if(!matches(page,data))return null;String wrapper=wrapper(page,data);return wrapper.isEmpty()?null:new Located(url,wrapper,page);
 }
 static List<String> searchUrls(Document page){
  LinkedHashSet<String> urls=new LinkedHashSet<>();for(Element card:page.select("article.card[onclick]")){
   Matcher m=Pattern.compile("^goFilm\\('([A-Za-z]+)',\\s*'([a-z0-9-]+)'\\)$").matcher(card.attr("onclick"));if(!m.find())continue;String prefix=m.group(1).equals("Serial")?"/serial-dublat/":m.group(1).equals("Desene")?"/desen-animat/":"/film-dublat/";urls.add(SITE+prefix+m.group(2)+"-dublat-in-romana");
  }return new ArrayList<>(urls);
 }
 Located locate(JSONObject data)throws Exception{
  LinkedHashSet<String> names=new LinkedHashSet<>();for(String key:Arrays.asList("localTitle","movieName","originalTitle"))if(!data.optString(key).isEmpty())names.add(data.optString(key));Set<String> visited=new HashSet<>();
  for(String name:names){String url=SITE+"/search.php?q="+URLEncoder.encode(name,"UTF-8");Document search=Jsoup.parse(NativeHttp.text(url,Collections.<String,String>emptyMap()),url);int checked=0;for(String target:searchUrls(search)){if(!visited.add(target)||++checked>8)continue;Located located=locateAt(target,data);if(located!=null)return located;}}return null;
 }
 List<NativeCatalog.Candidate> discover(JSONObject data)throws Exception{
  Located located=locate(data);if(located==null)return Collections.emptyList();String html=NativeHttp.text(located.wrapper,NativeHttp.headers(located.titleUrl));List<NativeCatalog.Candidate> found=new ArrayList<>();Set<String> seen=new HashSet<>();
  Matcher m=Pattern.compile("case\\s+\\d+:\\s*src\\s*=\\s*[\"'](https://[^\"']+)[\"']\\s*;").matcher(html);
  int duration=runtime(located.page.select(".meta-row").text());JSONObject expected=new JSONObject().put("durationSeconds",duration).put("title",located.page.select("h1.film-title").text());
  while(m.find()){String embed=m.group(1),host=NativeCatalog.kind(embed);if(!(host.equals("Byse")||host.equals("Streamtape"))||!seen.add(embed))continue;if(host.equals("Streamtape")&&duration<90)continue;found.add(new NativeCatalog.Candidate(host,located.titleUrl,located.wrapper,"ROMANA",embed,expected));}
  return found;
 }
}
