import org.json.*;import org.jsoup.Jsoup;import org.jsoup.nodes.*;import java.net.*;import java.util.*;import java.util.regex.*;
final class OkruClient {
 static JSONObject metadata(String html)throws Exception{
  for(Element node:Jsoup.parse(html).select("[data-options]")){
   JSONObject options;try{options=new JSONObject(node.attr("data-options"));}catch(Exception e){continue;}JSONObject vars=options.optJSONObject("flashvars");if(vars==null||!vars.has("metadata"))continue;Object data=vars.get("metadata");return data instanceof JSONObject?(JSONObject)data:new JSONObject(String.valueOf(data));
  }throw new Exception("OK.ru player metadata unavailable");
 }
 static double verify(JSONObject metadata,NativeCatalog.Candidate candidate)throws Exception{
  if(candidate.expected==null)throw new Exception("Unverified episode identity");JSONObject movie=metadata.getJSONObject("movie");
  String id=new URL(candidate.embed).getPath().replaceFirst("^/video(?:embed)?/","");if(!id.equals(movie.optString("id"))||!"OK".equals(movie.optString("status"))||movie.optBoolean("notPublished")||movie.optBoolean("isClip"))throw new Exception("OK.ru file unavailable or mismatched");
  String title=NativeCatalog.normalize(movie.optString("title"));if(!RomanianAudio.declaredRomanian("ROMANA "+movie.optString("title")))throw new Exception("Foreign audio label");
  Matcher number=Pattern.compile("\\b0*(\\d+)x0*(\\d+)\\b").matcher(title);boolean exact=number.find()&&Integer.parseInt(number.group(1))==candidate.expected.getInt("season")&&Integer.parseInt(number.group(2))==candidate.expected.getInt("episode");
  if(!exact)throw new Exception("OK.ru episode number mismatch");String series=NativeCatalog.normalize(candidate.expected.optString("seriesTitle")),episode=NativeCatalog.normalize(candidate.expected.optString("episodeTitle"));
  if(!(series.length()>=6&&title.startsWith(series+" "))&&!(episode.length()>=8&&!episode.matches("episodul \\d+")&&title.contains(episode)))throw new Exception("OK.ru episode title mismatch");
  double duration=Double.parseDouble(movie.optString("duration","0"));if(duration<90)throw new Exception("Short/error clip excluded");
  for(String key:Arrays.asList("defaultAudio","audioLanguage"))RomanianAudio.checkHostDefault(metadata.optString(key,""));return duration;
 }
 static boolean mediaUrl(String text){try{URL u=new URL(text);return "https".equals(u.getProtocol())&&u.getUserInfo()==null&&(u.getHost().equals("okcdn.ru")||u.getHost().endsWith(".okcdn.ru")||u.getHost().equals("mycdn.me")||u.getHost().endsWith(".mycdn.me"));}catch(Exception e){return false;}}
 static ResolvedStream resolve(NativeCatalog.Candidate candidate)throws Exception{
  if(candidate.expected==null)throw new Exception("Unverified episode identity");
  JSONObject info=metadata(NativeHttp.text(candidate.embed,NativeHttp.headers(candidate.page)));double duration=verify(info,candidate);JSONArray videos=info.optJSONArray("videos");Exception last=null;
  if(videos!=null)for(String quality:Arrays.asList("full","hd","sd","low","lowest","mobile"))for(int i=0;i<videos.length();i++){
   JSONObject video=videos.getJSONObject(i);if(!quality.equals(video.optString("name")))continue;String url=video.optString("url");if(!mediaUrl(url))continue;int height=quality.equals("full")?1080:quality.equals("hd")?720:quality.equals("sd")?480:quality.equals("low")?360:240;
   ResolvedStream stream=new ResolvedStream("OK.ru",url,candidate.embed,candidate.identity(),height,"DeseneleDublate",false);try{Mp4Probe.validate(stream,duration);return stream;}catch(Exception e){last=e;}
  }
  throw new Exception("No validated full-episode MP4"+(last==null?"":": "+last.getMessage()));
 }
}
