import org.json.*;
import org.jsoup.Jsoup;
public final class DozaUnitTest {
 static void yes(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  JSONObject meta=new JSONObject().put("localTitle","Un Show Obisnuit: Casetele Pierdute").put("releaseYear",2026);
  String start="<div class='mvic-desc'><h3 itemprop='name'>",end="</h3></div>";
  yes(DozaCatalog.matches(Jsoup.parse(start+"Un Show Obisnuit: Casetele Pierdute (2026) - Dublat in Romana"+end),meta),"Matching dubbed series");
  yes(!DozaCatalog.matches(Jsoup.parse(start+"Un Show Obisnuit: Casetele Pierdute (2026) - Subtitrat in Romana"+end),meta),"Subtitled title excluded");
  yes(!DozaCatalog.matches(Jsoup.parse(start+"Un Show Obisnuit: Casetele Pierdute (2025) - Dublat in Romana"+end),meta),"Wrong year excluded");
  yes(!DozaCatalog.matches(Jsoup.parse(start+"Un Show Obisnuit (2026) - Dublat in Romana"+end),meta),"Related show excluded");
  yes(!DozaCatalog.matches(Jsoup.parse(start+"Un Show Obisnuit: Casetele Pierdute (2026) - Subtitrat in Romana"+end+"<h3>Other show Dublat in Romana</h3>"),meta),"Recommended dubbing cannot qualify current title");
  String html="<div id='content-embed'><iframe src='https://vidmoly.org/embed-unit.html'></iframe><iframe src='https://vidmoly.org/embed-unit.html'></iframe><iframe src='https://playmogo.com/e/test'></iframe><iframe src='https://player.desenefaine.net/e/test'></iframe></div><iframe src='https://ads.invalid/frame'></iframe>";
  java.util.List<NativeCatalog.Candidate> candidates=DozaCatalog.players(Jsoup.parse(html,DozaCatalog.SITE),DozaCatalog.SITE+"/episode/test/");
  yes(candidates.size()==1&&candidates.get(0).host.equals("Vidmoly"),"Supported host dedupe and blocked/unsupported hosts excluded");
  yes(!DozaCatalog.siteUrl("https://www.dozaanimata.net.evil.invalid/title"),"Unrelated catalog host excluded");
  JSONObject english=new JSONObject().put("originalTitle","Re-Animated").put("releaseYear",2006);
  String heading="Din mintea lui Jimmy: Filmul (2006) - Dublat in Romana";
  String artwork="<img class='hidden' itemprop='image' title='"+heading+"' src='https://www.dozaanimata.net/wp-content/uploads/2024/12/Re-Animated.jpg'>";
  yes(DozaCatalog.matches(Jsoup.parse(start+heading+end+artwork),english),"Original-title main artwork resolves missing translation");
  yes(!DozaCatalog.matches(Jsoup.parse(start+heading+end+artwork.replace("class='hidden'","class='recommendation'")),english),"Recommendation artwork cannot establish identity");
  yes(!DozaCatalog.matches(Jsoup.parse(start+heading.replace("2006","2007")+end+artwork),english),"Artwork cannot override release year");
  JSONObject generic=new JSONObject().put("originalTitle","The Adventures Of A Different Hero").put("releaseYear",2006);
  yes(!DozaCatalog.matches(Jsoup.parse(start+heading+end+artwork.replace("Re-Animated.jpg","The-Adventures-Of.jpg")),generic),"Generic artwork prefix cannot establish identity");
  JSONObject translated=new JSONObject().put("originalTitle","A Different Translation").put("releaseYear",2006).put("verifiedArtwork",new JSONArray().put("independent.jpg"));
  String thumb="<div class='mvic-info'><meta itemprop='thumbnailUrl' content='https://image.tmdb.org/t/p/w780/independent.jpg'></div>";
  yes(DozaCatalog.matches(Jsoup.parse(start+heading+end+thumb),translated),"Exact independently verified artwork resolves a different translation");
  yes(!DozaCatalog.matches(Jsoup.parse(start+heading+end+thumb.replace("mvic-info","recommendations")),translated),"Related artwork cannot establish identity");
  yes(!DozaCatalog.matches(Jsoup.parse(start+heading+end+thumb.replace("image.tmdb.org","untrusted.invalid")),translated),"Only independent TMDB artwork accepted");
  yes(!DozaCatalog.matches(Jsoup.parse(start+heading.replace("2006","2007")+end+thumb),translated),"Artwork alone cannot override a different year");
  yes(!DozaCatalog.matches(Jsoup.parse(start+heading.replace("Dublat","Subtitrat")+end+thumb),translated),"Verified artwork cannot bypass the dubbed requirement");
  yes(DozaCatalog.episodeAddress("https://www.dozaanimata.net/episode/show-episodul-1-title/",1,1),"Single-season legacy URL");
  yes(!DozaCatalog.episodeAddress("https://www.dozaanimata.net/episode/show-episodul-1-title/",2,1),"Legacy URL cannot match another season");
  yes(!DozaCatalog.episodeAddress("https://www.dozaanimata.net/episode/show-episodul-10-title/",1,1),"Episode one cannot match ten");
  java.util.SortedMap<Integer,String> segments=new java.util.TreeMap<>();segments.put(101,"/episode/show-sezonul-1-episodul-1-1-first-distinct-story/");segments.put(102,"/episode/show-sezonul-1-episodul-1-2-second-distinct-story/");
  JSONArray season=new JSONArray().put(new JSONObject().put("episode_number",1).put("name","First Distinct Story")).put(new JSONObject().put("episode_number",2).put("name","Second Distinct Story"));
  yes(DozaCatalog.selectSegmented(segments,season,2).equals(segments.get(102)),"Complete independently anchored segmented ordering");
  yes(DozaCatalog.selectSegmented(segments,new JSONArray().put(season.get(0)),1).equals("!"),"Incomplete segment catalogs rejected");
  yes(DozaCatalog.selectSegmented(segments,new JSONArray().put(new JSONObject().put("episode_number",1).put("name","Unrelated First")).put(new JSONObject().put("episode_number",2).put("name","Unrelated Second")),1).equals("!"),"Count alone cannot establish segment correspondence");
  System.out.println("PASS Doza dubbed-only, strict titles/year, recommendation isolation, supported-host filtering and dedupe");
 }
}
