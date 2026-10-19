final class ResolvedStream {
    final String host,url,referer,identity,site;
    final int quality;
    final boolean isHls;
    ResolvedStream(String host,String url,String referer,String identity,int quality){this(host,url,referer,identity,quality,"DeseneFaine");}
    ResolvedStream(String host,String url,String referer,String identity,int quality,String site){this(host,url,referer,identity,quality,site,true);}
    ResolvedStream(String host,String url,String referer,String identity,int quality,String site,boolean isHls){this.site=site;this.host=host;this.url=url;this.referer=referer;this.identity=identity;this.quality=quality;this.isHls=isHls;}
}
