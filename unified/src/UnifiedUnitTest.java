import org.json.*;
import org.jsoup.Jsoup;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class UnifiedUnitTest {
 static void yes(boolean value,String label){if(!value)throw new AssertionError(label);}
 static void rejects(String playlist)throws Exception{try{RomanianAudio.checkMaster(playlist);throw new AssertionError("Foreign audio accepted");}catch(AssertionError e){throw e;}catch(Exception expected){}}
 public static void main(String[] args)throws Exception{
  yes(RomanianAudio.declaredRomanian("Byse - ROMÂNĂ"),"Romanian declared");
  for(String bad:new String[]{"English","Unknown","ROMÂNĂ SUBTITRAT","ROMANA / ENGLISH"})yes(!RomanianAudio.declaredRomanian(bad),"Reject "+bad);
  rejects("#EXTM3U\n#EXT-X-MEDIA:TYPE=AUDIO,NAME=\"English\",LANGUAGE=\"en\",DEFAULT=YES");
  rejects("#EXTM3U\n#EXT-X-MEDIA:TYPE=AUDIO,NAME=\"Română\",LANGUAGE=\"en\",DEFAULT=YES");
  rejects("#EXTM3U\n#EXT-X-MEDIA:TYPE=AUDIO,LANGUAGE=\"ro\",DEFAULT=NO\n#EXT-X-MEDIA:TYPE=AUDIO,LANGUAGE=\"en\",DEFAULT=YES");
  RomanianAudio.checkMaster("#EXTM3U\n#EXT-X-MEDIA:TYPE=AUDIO,NAME=\"Română\",LANGUAGE=\"ro\",DEFAULT=YES");
  RomanianAudio.checkMaster("#EXTM3U\n#EXT-X-MEDIA:TYPE=SUBTITLES,LANGUAGE=\"en\",DEFAULT=YES");
  NativeCatalog catalog=new NativeCatalog();catalog.titles.add("Toy Story 5");catalog.year=2026;
  yes(catalog.matches(Jsoup.parse("<h1>Toy Story 5</h1><span class='year'>2026</span>"),"https://desenefaine.com/film/toy-story-5/"),"Correct movie");
  yes(!catalog.matches(Jsoup.parse("<h1>Toy Story 5: A Special Perspective</h1><span class='year'>2026</span>"),"https://desenefaine.com/film/other/"),"Reject related extra");
  yes(!catalog.matches(Jsoup.parse("<h1>Toy Story 5</h1><span class='year'>2025</span>"),"https://desenefaine.com/film/other/"),"Reject year mismatch");
  JSONArray fixtures=new JSONArray(new String(Files.readAllBytes(Paths.get("unit-fixtures.json")),StandardCharsets.UTF_8));
  for(int i=0;i<fixtures.length();i++){
   JSONObject f=fixtures.getJSONObject(i),decoded=SharedPlayerClient.decrypt(f.getString("encrypted"));
   yes(decoded.toString().length()>0,"Node/JCA crypto");
   String actual=SharedPlayerClient.source(decoded,"https://seeksreaming.embedseek.com",decoded.optJSONObject("pk"));
   yes(actual.equals(f.getString("expectedSource")),"Working JS source compatibility case "+i);
  }
  yes(NativeCatalog.kind("https://bysewihe.com/e/id").equals("Byse"),"Byse host");
  yes(NativeCatalog.labelKind("Filemoon - ROMÂNĂ").equals("Byse"),"Legacy Filemoon candidate");
  yes(NativeCatalog.kind("https://ads.invalid/e/id").isEmpty(),"Reject unrelated host");
  System.out.println("PASS Romanian gates, HLS audio, strict title/year matching, independent AES and existing-source compatibility.");
 }
}
