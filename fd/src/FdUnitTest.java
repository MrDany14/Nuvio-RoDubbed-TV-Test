import org.json.*;import org.jsoup.Jsoup;
public final class FdUnitTest {
 static void yes(boolean v,String msg){if(!v)throw new AssertionError(msg);}
 public static void main(String[] args)throws Exception{
  String html="<title>Toy Story 5 Dublat in Romana</title><h1 class='film-title'>Povestea jucariilor 5</h1><p class='orig-title'>Toy Story 5</p><div class='meta-row'><span class='meta-item'>2026</span></div><iframe src='/embed/filmsrv.php?search=toy-story-5'></iframe>";
  JSONObject input=new JSONObject().put("localTitle","Toy Story 5").put("releaseYear",2026);
  yes(FdCatalog.matches(Jsoup.parse(html,FdCatalog.SITE),input),"Original title resolves translated movie");
  yes(!FdCatalog.matches(Jsoup.parse(html.replace("2026","2025")),input),"Wrong release year rejected");
  yes(!FdCatalog.matches(Jsoup.parse(html.replace("Dublat","Subtitrat")),input),"Subtitled movie rejected");
  yes(!FdCatalog.matches(Jsoup.parse(html.replace("Toy Story 5","Toy Story 4")),input),"Another film rejected");
  yes(FdCatalog.runtime("2026 1h 40m 2.6K views")==6000,"Duration excludes year/views");
  String episodes="<button class='ep-btn' title='S1E01' onclick=\"playEp('ironheart-s01e01', this)\"></button><button class='ep-btn' title='S1E02' onclick=\"playEp('ironheart-s01e02', this)\"></button><iframe src='/embed/filmsrv.php?search=ironheart-s01e01'></iframe>";
  yes(FdCatalog.wrapper(Jsoup.parse(episodes,FdCatalog.SITE),new JSONObject().put("season",1).put("episode",2)).endsWith("ironheart-s01e02"),"Requested episode replaces default iframe");
  yes(FdCatalog.wrapper(Jsoup.parse(episodes,FdCatalog.SITE),new JSONObject().put("season",1).put("episode",10)).isEmpty(),"Missing episode cannot return default");
  yes(FdCatalog.wrapper(Jsoup.parse(episodes,FdCatalog.SITE),new JSONObject()).isEmpty(),"Series default iframe cannot qualify as movie");
  String link="<script>document.getElementById('robotlink').innerHTML = '//stre'+ ('xcdamtape.com/get_video?id=abc123&token=test').substring(2).substring(1);</script>";
  yes(StreamtapeClient.source(link,"https://streamtape.com/e/abc123/").equals("https://streamtape.com/get_video?id=abc123&token=test&stream=1"),"Literal player string reconstruction");
  try{StreamtapeClient.source(link,"https://streamtape.com/e/other/");throw new AssertionError("Wrong video accepted");}catch(AssertionError e){throw e;}catch(Exception expected){}
  NativeCatalog.Candidate wrongSite=new NativeCatalog.Candidate("Streamtape","https://deseneledublate.com/desen/english/","","ROMANA","https://streamtape.com/e/abc123/",new JSONObject().put("durationSeconds",6000));
  try{StreamtapeClient.resolve(wrongSite);throw new AssertionError("Unconfirmed site accepted");}catch(AssertionError e){throw e;}catch(Exception expected){yes(expected.getMessage().contains("provenance"),"Reject wrong site before network");}
  yes(!FdCatalog.siteUrl("https://filmedublate.net.evil.invalid/"),"Unrelated catalog host rejected");
  System.out.println("PASS FD translated title/year/dubbing, exact episodes, Streamtape video identity and site-specific provenance");
 }
}
