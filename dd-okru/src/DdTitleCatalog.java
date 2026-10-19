import java.net.*;import java.util.*;import java.util.regex.*;import org.json.*;import org.jsoup.Jsoup;import org.jsoup.nodes.*;
/** Address-only experiment. Never requests a player, stream host or media. */
final class DdTitleCatalog {
 static final String SITE="https://deseneledublate.com";
 static boolean siteUrl(String value){try{URL u=new URL(value);return "https".equals(u.getProtocol())&&SITE.equals("https://"+u.getHost())&&u.getUserInfo()==null;}catch(Exception e){return false;}}
 static String field(Document doc,String label){for(Element e:doc.select("#info .custom_fields"))if(NativeCatalog.normalize(e.select("b.variante").text()).equals(label))return e.select("span.valor").text();return "";}
 static String title(Document doc){return doc.select("h1").first()==null?"":doc.select("h1").first().text();}
 static boolean series(JSONObject data){return data.optInt("season")>0||data.optString("mediaType").equals("tv");}
 static String titleName(String value){return NativeCatalog.normalize(value.replaceFirst("\\s*\\((?:19|20)\\d{2}\\).*$","")).replaceFirst(" (?:seria |toata seria |seria completa |toate episoadele |sezonul \\d+ )?dublat(?:a|e)? in romana.*$","").trim();}
 static String query(String value){return value.replaceAll("[^\\p{L}\\p{N}\\s]"," ").replaceAll("\\s+"," ").trim();}
 static boolean seasonMatches(Document doc,JSONObject data){
  if(!series(data)||data.optInt("season")<=0)return true;
  int season=data.getInt("season"),episode=data.optInt("episode");Matcher declared=Pattern.compile("\\bsezonul (\\d+)\\b").matcher(NativeCatalog.normalize(title(doc)));
  if(declared.find()){if(Integer.parseInt(declared.group(1))!=season)return false;if(episode<=0)return true;}
  // A combined title must actually list the requested season. Related cards,
  // descriptive season totals and navigation links cannot prove coverage.
  for(Element a:doc.select("#seasons .episodiotitle a[href]")){
   String target=a.absUrl("href");if(!siteUrl(target))continue;Matcher m=Pattern.compile("/episoade/[^/]*-sezonul-(\\d+)-episodul-(\\d+)(?:-|/|$|(?=dublat))").matcher(newPath(target));if(m.find()&&Integer.parseInt(m.group(1))==season&&(episode<=0||Integer.parseInt(m.group(2))==episode))return true;
  }
  for(Element a:doc.select("#info .wp-content a[href]")){Matcher m=Pattern.compile("\\bsezonul (\\d+) episodul (\\d+)\\b").matcher(NativeCatalog.normalize(a.text()));if(m.find()&&Integer.parseInt(m.group(1))==season&&(episode<=0||Integer.parseInt(m.group(2))==episode))return true;}
  return false;
 }
 static String newPath(String value){try{return new URL(value).getPath();}catch(Exception e){return "";}}
 static int year(Document doc,boolean series){String date=series?field(doc,"first air date"):doc.select(".sheader .extra span.date").text();Matcher m=Pattern.compile("\\b((?:19|20)\\d{2})\\b").matcher(date);if(m.find())return Integer.parseInt(m.group(1));m=Pattern.compile("\\((\\d{4})\\)").matcher(title(doc));return m.find()?Integer.parseInt(m.group(1)):0;}
 static boolean matches(Document doc,JSONObject data){
  String h=title(doc),normalized=NativeCatalog.normalize(h);if(!RomanianAudio.declaredRomanian(h)||!normalized.matches(".*\\bdublat(?:a|e)? in romana\\b.*"))return false;
  if(!seasonMatches(doc,data))return false;
  boolean series=series(data);if(year(doc,series)!=data.optInt("releaseYear")||data.optInt("releaseYear")<=0)return false;
  String heading=titleName(h),original=NativeCatalog.normalize(field(doc,"original title"));for(String key:Arrays.asList("localTitle","movieName","originalTitle","englishTitle")){String expected=NativeCatalog.normalize(data.optString(key));if(!expected.isEmpty()&&(expected.equals(heading)||expected.equals(original)))return true;}
  JSONArray aliases=data.optJSONArray("verifiedNames");if(aliases!=null)for(int i=0;i<aliases.length();i++){String expected=NativeCatalog.normalize(aliases.optString(i));if(!expected.isEmpty()&&(expected.equals(heading)||expected.equals(original)))return true;}
  Element poster=doc.select(".sheader .poster img[src]").first();JSONArray known=data.optJSONArray("verifiedArtwork");if(poster==null||known==null)return false;String file=poster.attr("src").replaceFirst("^.*[/]","").replaceFirst("-\\d+x\\d+(?=\\.)","");for(int i=0;i<known.length();i++)if(file.equals(known.optString(i)))return true;return false;
 }
 String locate(JSONObject input)throws Exception{
  JSONObject data=new JSONObject(input.toString());boolean series=series(data);Set<String> names=new LinkedHashSet<>(),seen=new HashSet<>();for(String key:Arrays.asList("localTitle","movieName","originalTitle"))if(!data.optString(key).isEmpty())names.add(data.optString(key));
  for(int phase=0;phase<2;phase++){
   if(phase==1){JSONObject m=new JSONObject(NativeHttp.text("https://api.themoviedb.org/3/"+(series?"tv/":"movie/")+data.getInt("tmdbID")+"?language=en-US&append_to_response=images,alternative_titles&include_image_language=en,ro,null&api_key="+NativeCatalog.TMDB_KEY,Collections.<String,String>emptyMap()));String english=m.optString(series?"name":"title");data.put("englishTitle",english);if(!english.isEmpty())names.add(english);JSONArray known=new JSONArray();for(String key:Arrays.asList("poster_path","backdrop_path")){String path=m.optString(key);if(path.startsWith("/"))known.put(path.substring(1));}JSONObject images=m.optJSONObject("images");if(images!=null)for(String key:Arrays.asList("posters","backdrops")){JSONArray list=images.optJSONArray(key);if(list!=null)for(int i=0;i<list.length();i++){String path=list.getJSONObject(i).optString("file_path");if(path.startsWith("/"))known.put(path.substring(1));}}data.put("verifiedArtwork",known);
    JSONArray verified=new JSONArray();JSONObject alternatives=m.optJSONObject("alternative_titles");JSONArray list=alternatives==null?null:alternatives.optJSONArray(series?"results":"titles");if(list!=null)for(int i=0;i<list.length();i++){JSONObject alias=list.getJSONObject(i);String name=alias.optString("title"),country=alias.optString("iso_3166_1");if(name.isEmpty())continue;verified.put(name);if(country.matches("RO|US|GB"))names.add(name);}data.put("verifiedNames",verified);
    for(String target:seen){Document page=Jsoup.parse(NativeHttp.text(target,Collections.<String,String>emptyMap()),target);if(matches(page,data))return target;}
    List<String> full=new ArrayList<>(names);for(String name:full){String[] words=name.split("\\s+");if(words.length>1&&words[0].length()>=4)names.add(words[0]);}seen.clear();
   }
   Set<String> queries=new LinkedHashSet<>();for(String name:names){String normalized=query(name);if(!normalized.isEmpty())queries.add(normalized);}int count=0;for(String name:queries){if(++count>6)break;String url=SITE+"/?s="+URLEncoder.encode(name,"UTF-8");Document search=Jsoup.parse(NativeHttp.text(url,Collections.<String,String>emptyMap()),url);int checked=0;for(Element a:search.select(".result-item .title a[href], article.item .data h3 a[href]")){String target=a.absUrl("href");if(!siteUrl(target)||!new URL(target).getPath().startsWith(series?"/serial/":"/desen/")||!seen.add(target)||++checked>10)continue;Document page=Jsoup.parse(NativeHttp.text(target,Collections.<String,String>emptyMap()),target);if(matches(page,data))return target;}}
  }return null;
 }
}
