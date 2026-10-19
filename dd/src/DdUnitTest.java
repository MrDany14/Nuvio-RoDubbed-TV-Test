import org.json.*;import org.jsoup.Jsoup;import java.nio.*;
public final class DdUnitTest {
 static void yes(boolean v,String text){if(!v)throw new AssertionError(text);}
 static JSONObject input(){return new JSONObject().put("localTitle","Bob Constructorul").put("originalTitle","Bob the Builder").put("releaseYear",2015);}
 static void rejects(JSONObject metadata,NativeCatalog.Candidate c)throws Exception{try{OkruClient.verify(metadata,c);throw new AssertionError("Bad source accepted");}catch(AssertionError e){throw e;}catch(Exception expected){}}
 public static void main(String[] args)throws Exception{
  String html="<h1>Bob Constructorul (2015) Seria Dublata in Romana</h1><div id='info'><div class='custom_fields'><b class='variante'>Original title</b><span class='valor'>Bob the Builder</span></div><div class='custom_fields'><b class='variante'>First air date</b><span class='valor'>Sep. 1, 2015</span></div></div>";
  yes(DdCatalog.matches(Jsoup.parse(html),input()),"Exact dubbed parent and premiere year");
  yes(!DdCatalog.matches(Jsoup.parse(html.replace("2015","1998")),input()),"Remake year rejected");
  yes(!DdCatalog.matches(Jsoup.parse(html.replace("Dublata","Subtitrata")),input()),"Subtitled parent rejected");
  yes(!DdCatalog.matches(Jsoup.parse(html.replace("id='info'","id='recommendations'")),input()),"Related metadata cannot establish identity");
  yes(NativeCatalog.kind("https://streamtape.com/e/english").isEmpty(),"Unconfirmed English Streamtape host excluded");
  yes(NativeCatalog.kind("https://sendvid.com/embed/error").isEmpty(),"Withdrawn Sendvid excluded");
  JSONObject expected=new JSONObject().put("seriesTitle","Bob Constructorul").put("episodeTitle","Scoop e la mare inaltime").put("season",1).put("episode",1);
  NativeCatalog.Candidate c=new NativeCatalog.Candidate("OK.ru",DdCatalog.SITE+"/episoade/unit/","","ROMANA","https://ok.ru/videoembed/12345",expected);
  JSONObject movie=new JSONObject().put("id","12345").put("status","OK").put("title","Bob Constructorul 01x01 Scoop e la mare inaltime").put("duration","662");JSONObject metadata=new JSONObject().put("movie",movie);
  yes(OkruClient.verify(metadata,c)==662,"Exact host video identity and episode");
  rejects(new JSONObject(metadata.toString()).put("movie",new JSONObject(movie.toString()).put("duration",5)),c);
  rejects(new JSONObject(metadata.toString()).put("movie",new JSONObject(movie.toString()).put("id","999")),c);
  rejects(new JSONObject(metadata.toString()).put("movie",new JSONObject(movie.toString()).put("title","Bob Constructorul 01x02 Other")),c);
  rejects(new JSONObject(metadata.toString()).put("movie",new JSONObject(movie.toString()).put("title","Unrelated Show 01x01 Other")),c);
  rejects(new JSONObject(metadata.toString()).put("movie",new JSONObject(movie.toString()).put("title","Bob Constructorul 01x01 English")),c);
  yes(!OkruClient.mediaUrl("https://okcdn.ru.evil.invalid/file"),"Unrelated media host rejected");
  byte[] header=new byte[80];ByteBuffer b=ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);b.putInt(24).put("ftyp".getBytes("US-ASCII"));b.position(24);b.putInt(56).put("moov".getBytes("US-ASCII")).putInt(48).put("mvhd".getBytes("US-ASCII"));b.position(52);b.putInt(1000).putInt(662000);yes(Mp4Probe.movieDuration(header)==662,"Independent MP4 movie duration atom");
  yes(new ResolvedStream("Byse","url","ref","id",0).isHls,"Existing HLS format retained");
  System.out.println("PASS DD parent/year/language isolation; OK.ru episode identity, short/error rejection and MP4 duration");
 }
}
