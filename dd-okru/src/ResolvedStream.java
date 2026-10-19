final class ResolvedStream {
    final String host,url,referer,identity,site;
    final int quality;
    final boolean isHls;
    final java.util.Map<String,String> playbackHeaders;
    ResolvedStream(String host,String url,String referer,String identity,int quality){this(host,url,referer,identity,quality,"DeseneFaine");}
    ResolvedStream(String host,String url,String referer,String identity,int quality,String site){this(host,url,referer,identity,quality,site,true);}
    ResolvedStream(String host,String url,String referer,String identity,int quality,String site,boolean isHls){this(host,url,referer,identity,quality,site,isHls,null);}
    ResolvedStream(String host,String url,String referer,String identity,int quality,String site,boolean isHls,java.util.Map<String,String> playbackHeaders){this.playbackHeaders=playbackHeaders==null?null:new java.util.HashMap<>(playbackHeaders);this.site=site;this.host=host;this.url=url;this.referer=referer;this.identity=identity;this.quality=quality;this.isHls=isHls;}
}
