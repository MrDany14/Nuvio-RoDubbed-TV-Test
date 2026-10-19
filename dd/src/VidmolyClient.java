import java.util.regex.*;
final class VidmolyClient {
    static ResolvedStream resolve(NativeCatalog.Candidate c)throws Exception{
        String html=NativeHttp.text(c.embed,NativeHttp.headers(c.page));
        Matcher source=Pattern.compile("sources\\s*:\\s*\\[\\s*\\{\\s*file\\s*:\\s*['\"](https://[^'\"]+)['\"]").matcher(html);
        if(!source.find())throw new Exception("Vidmoly HLS configuration unavailable");
        String url=source.group(1).replace("\\/","/");
        if(!url.contains(".m3u8"))throw new Exception("Vidmoly HLS source unavailable");
        return new ResolvedStream("Vidmoly",url,c.embed,c.identity(),0);
    }
}
