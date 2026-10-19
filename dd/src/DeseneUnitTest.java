import org.jsoup.Jsoup;
public final class DeseneUnitTest {
 static void yes(boolean value,String label){if(!value)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception{
  NativeCatalog c=new NativeCatalog();c.series=true;c.year=2026;c.titles.add("Dinozaurii");
  yes(!c.matches(Jsoup.parse("<h1>Dinozaurii</h1><span class='year'>1991</span>"),NativeCatalog.SITE+"/serial/dinozaurii/"),"Reject1991 series for2026 remake");
  yes(c.matches(Jsoup.parse("<h1>Dinozaurii</h1><span class='year'>2026</span>"),NativeCatalog.SITE+"/serial/dinozaurii-26/"),"Accept correct parent");
  yes(!c.matches(Jsoup.parse("<h1>Dinozaurii</h1>"),NativeCatalog.SITE+"/serial/dinozaurii/"),"Unknown parent year cannot qualify remake");
  yes(!c.matches(Jsoup.parse("<h1>Other</h1><span class='year'>2026</span><ul class='details-lst'><li>Genre<span>Animation</span></li></ul><ul class='details-lst'><li>Titlu Original<span>Dinozaurii</span></li></ul>"),NativeCatalog.SITE+"/serial/other/"),"Related metadata cannot establish identity");
  yes(!c.matches(Jsoup.parse("<article class='post single'><header class='entry-header'><h1>Other</h1><span class='year'>2026</span></header></article><aside><ul class='details-lst'><li>Titlu Original<span>Dinozaurii</span></li></ul></aside>"),NativeCatalog.SITE+"/serial/other/"),"Missing own details cannot borrow recommendation metadata");
  yes(!c.matches(Jsoup.parse("<article class='post single'><header class='entry-header'><h1>Dinozaurii</h1></header></article><aside><span class='year'>2026</span></aside>"),NativeCatalog.SITE+"/serial/dinozaurii/"),"Missing own year cannot borrow recommendation year");
  for(String url:new String[]{"/epi/show-sezonul-3-episodul-10/","/epi/show-s3-ep10/","/epi/show-3x10/","/epi/show-sezonul3-episoul10/"}){int[] n=NativeCatalog.episodeNumber(url);yes(n!=null&&n[0]==3&&n[1]==10,"Episode syntax "+url);}
  int[] ten=NativeCatalog.episodeNumber("/epi/show-1x10/");yes(ten[1]!=1,"Episode1 cannot match10");
  NativeCatalog movie=new NativeCatalog();movie.year=2016;movie.titles.add("Ozzy");
  String html="<h1>Ozzy: Marea Evadare</h1><span class='year'>2016</span>";
  yes(movie.matches(Jsoup.parse(html),NativeCatalog.SITE+"/film/ozzy/"),"Short original movie slug plus subtitle");
  yes(!movie.matches(Jsoup.parse(html),NativeCatalog.SITE+"/film/ozzy-another/"),"Different movie slug cannot use short-name fallback");
  yes(!movie.matches(Jsoup.parse(html.replace("2016","2025")),NativeCatalog.SITE+"/film/ozzy/"),"Short-title fallback cannot override year");
  NativeCatalog pilot=new NativeCatalog();pilot.series=true;pilot.year=1995;pilot.titles.add("Life with Louie");
  String pilotHtml="<h1>Life with Louie</h1><span class='year'>1994</span><img src='https://image.tmdb.org/t/p/w342/verified.jpg'>";
  yes(!pilot.matches(Jsoup.parse(pilotHtml),NativeCatalog.SITE+"/serial/life-with-louie/"),"Adjacent year alone cannot qualify");
  pilot.verifiedPosters.add("verified.jpg");yes(pilot.matches(Jsoup.parse(pilotHtml),NativeCatalog.SITE+"/serial/life-with-louie/"),"Adjacent pilot year plus exact TMDB artwork");
  yes(!pilot.matches(Jsoup.parse(pilotHtml.replace("1994","1991")),NativeCatalog.SITE+"/serial/life-with-louie/"),"Artwork cannot allow arbitrary year mismatch");
  NativeCatalog release=new NativeCatalog();release.year=2013;release.titles.add("The Jungle Bunch");release.verifiedReleaseYears.add(2011);
  String released="<h1>The Jungle Bunch</h1><span class='year'>2011</span><img src='https://image.tmdb.org/t/p/w342/proof.jpg'>";
  yes(!release.matches(Jsoup.parse(released),NativeCatalog.SITE+"/film/the-jungle-bunch/"),"A release-date entry alone cannot confuse a remake");
  release.verifiedPosters.add("proof.jpg");yes(release.matches(Jsoup.parse(released),NativeCatalog.SITE+"/film/the-jungle-bunch/"),"Documented alternate release plus exact main artwork");
  String shorts=pilotHtml.replace("Life with Louie</h1>","Life with Louie (Shorts)</h1>")+"<ul class='details-lst'><li>Titlu Original<span>Life with Louie</span></li></ul>";
  yes(!pilot.matches(Jsoup.parse(shorts),NativeCatalog.SITE+"/serial/life-with-louie-shorts/"),"Shorts collection cannot replace full series even with reused metadata/artwork");
  System.out.println("PASS Desene parent identity/year, episode syntaxes, recommendation isolation and movie alias safeguards");
 }
}
