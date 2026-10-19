import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
final class SendvidClient {
    static String source(String html,String embed)throws Exception{
        if(!NativeCatalog.kind(embed).equals("Sendvid"))throw new Exception("Invalid Sendvid embed");
        Document page=Jsoup.parse(html,embed);
        Element video=page.select("video#video-js-video source#video_source[src][type=video/mp4]").first();
        if(video==null)throw new Exception("Sendvid MP4 source unavailable");
        String value=video.absUrl("src");URL url=new URL(value);
        String id=new URL(embed).getPath().replaceFirst("^/embed/","").replaceFirst("/$","");
        if(!"https".equals(url.getProtocol())||!url.getHost().matches("videos[0-9]*\\.sendvid\\.com")||url.getUserInfo()!=null||!url.getPath().endsWith("/"+id+".mp4"))throw new Exception("Unexpected Sendvid media address");
        return value;
    }
    static ResolvedStream resolve(NativeCatalog.Candidate candidate)throws Exception{
        String html=NativeHttp.text(candidate.embed,NativeHttp.headers(candidate.page));
        return new ResolvedStream("Sendvid",source(html,candidate.embed),candidate.embed,candidate.identity(),0,"DozaAnimata",false);
    }
}
