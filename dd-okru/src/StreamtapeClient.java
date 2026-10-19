import java.net.*;import java.util.regex.*;
final class StreamtapeClient {
 static String source(String html,String embed)throws Exception{
  Matcher assignments=Pattern.compile("document\\.getElementById\\(['\"](?:botlink|robotlink)['\"]\\)\\.innerHTML\\s*=\\s*([^;]+);").matcher(html);String source=null;
  while(assignments.find()){
   String expression=assignments.group(1).trim();Matcher prefix=Pattern.compile("^(['\"])([^'\"]*)\\1\\s*\\+\\s*(?:''\\s*\\+\\s*)?\\(\\s*(['\"])([^'\"]*)\\3\\s*\\)\\s*(.*)$").matcher(expression);if(!prefix.matches())continue;
   String part=prefix.group(4),rest=prefix.group(5).trim();Matcher cuts=Pattern.compile("^\\.substring\\((\\d+)\\)(.*)$").matcher(rest);int count=0;
   while(cuts.matches()&&++count<=4){int n=Integer.parseInt(cuts.group(1));part=part.substring(Math.min(n,part.length()));rest=cuts.group(2).trim();cuts=Pattern.compile("^\\.substring\\((\\d+)\\)(.*)$").matcher(rest);}
   if(!rest.isEmpty()||count==0)continue;String raw=prefix.group(2)+part;URL target=new URL(new URL(embed),raw);
   if(!"streamtape.com".equals(target.getHost())||!target.getPath().equals("/get_video")||target.getUserInfo()!=null)continue;
   String id=new URL(embed).getPath().replaceFirst("^/e/","").replaceFirst("/$","");Matcher parameter=Pattern.compile("(?:^|&)id=([^&]+)").matcher(target.getQuery()==null?"":target.getQuery());if(!parameter.find()||!id.equals(parameter.group(1)))continue;
   source="https://streamtape.com"+target.getFile()+"&stream=1";
  }if(source==null)throw new Exception("Streamtape player link unavailable");return source;
 }
 static ResolvedStream resolve(NativeCatalog.Candidate candidate)throws Exception{
  if(!FdCatalog.siteUrl(candidate.page)||candidate.expected==null)throw new Exception("Streamtape site/audio provenance not enabled");
  int expected=candidate.expected.optInt("durationSeconds");if(expected<90)throw new Exception("Expected movie/episode duration missing");
  String url=source(NativeHttp.text(candidate.embed,NativeHttp.headers(candidate.page)),candidate.embed);
  double actual=Mp4Probe.movieDuration(NativeHttp.bytes(url,NativeHttp.headers(candidate.embed),4096,true));
  if(actual<90||Math.abs(actual-expected)>Math.max(60,expected*.10))throw new Exception("Streamtape duration mismatch/error clip: expected "+expected+"s, actual "+Math.round(actual)+"s");
  String finalUrl=NativeHttp.session().resolvedUrls.get(url);if(finalUrl==null)throw new Exception("Media redirect unavailable");URL target=new URL(finalUrl);
  if(!"https".equals(target.getProtocol())||target.getUserInfo()!=null||!(target.getHost().equals("streamtape.com")||target.getHost().endsWith(".tapecontent.net")))throw new Exception("Unexpected Streamtape media host");
  return new ResolvedStream("Streamtape",finalUrl,candidate.embed,candidate.identity(),0,"FilmeDublate",false);
 }
}
