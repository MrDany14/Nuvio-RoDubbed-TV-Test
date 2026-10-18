import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Fresh catalog discovery: HTTP/HTML only, without loading players or ads. */
public final class NativeCatalog {
    static final String SITE="https://desenefaine.com";
    // Public metadata credential shipped in Cloudstream's TmdbProvider source.
    static final String TMDB_KEY="e6333b32409e02a4a6eba6fb7ff866bb";
    private final long deadline=System.nanoTime()+25000000000L;
    private final Set<String> visited=new HashSet<>();
    private int year, season, episode;
    private boolean series;
    private final LinkedHashSet<String> names=new LinkedHashSet<>();
    public String pageUrl;

    String get(String url) throws Exception {
        if(Thread.currentThread().isInterrupted()||System.nanoTime()>deadline)throw new IOException("Catalog deadline");
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        int timeout=(int)Math.max(1,Math.min(7000,(deadline-System.nanoTime())/1000000));
        c.setConnectTimeout(timeout);c.setReadTimeout(timeout);c.setRequestProperty("User-Agent",ByseClient.USER_AGENT);
        try {
            int status=c.getResponseCode();if(status==404)return "";if(status!=200)throw new IOException("Catalog HTTP "+status);
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){
                byte[] buf=new byte[8192];int n;
                while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>3*1024*1024)throw new IOException("Catalog response too large");}
            }
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }finally{c.disconnect();}
    }
    static String normalize(String s) {
        return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").trim();
    }
    static String slug(String s){return normalize(s).replace(' ','-');}
    static boolean byse(String url) {
        try{return new URL(url).getHost().toLowerCase(Locale.ROOT).matches("(?:[a-z0-9-]+\\.)*byse[a-z0-9-]*\\.[a-z]{2,}");}
        catch(Exception e){return false;}
    }
    boolean matches(Document page,String url) {
        if(series){
            String path=new URLPath(url).path;
            boolean parts=path.matches(".*(?:sezonul-|sezon-|s0?)"+season+"-(?:episodul-|ep|e0?)"+episode+"(?:-|/|$).*");
            if(!parts)return false;
        }
        String labels=normalize(page.select("h1.entry-title, h1, .details-lst").text());
        boolean title=false;
        for(String n:names){String normalized=normalize(n);if(!normalized.isEmpty()&&(" "+labels+" ").contains(" "+normalized+" ")){title=true;break;}}
        if(!title)return false;
        String y=page.select(".year, .details-lst").text();
        Matcher m=Pattern.compile("\\b(?:19|20)\\d{2}\\b").matcher(y);
        return series||year==0||!m.find()||Integer.parseInt(m.group())==year;
    }
    static final class URLPath {final String path;URLPath(String url){try{path=new URL(url).getPath();}catch(Exception e){throw new IllegalArgumentException(e);}}}
    String wrapper(String url) throws Exception {
        if(byse(url))return url;
        Document page=Jsoup.parse(get(url),url);
        for(Element frame:page.select("iframe[src]")) {
            String target=frame.absUrl("src");
            if(byse(target))return target;
            if(target.contains("trhide=1")){
                Matcher m=Pattern.compile("[?&]tid=([0-9a-fA-F]+)").matcher(target);
                if(m.find()){
                    String hex=new StringBuilder(m.group(1)).reverse().toString();
                    ByteArrayOutputStream out=new ByteArrayOutputStream();
                    for(int i=0;i+1<hex.length();i+=2)out.write(Integer.parseInt(hex.substring(i,i+2),16));
                    String decoded=new String(out.toByteArray(),StandardCharsets.UTF_8);
                    if(byse(decoded))return decoded;
                }
            }
        }
        return null;
    }
    String inspect(String url) throws Exception {
        if(!visited.add(url))return null;
        String html=get(url);if(html.isEmpty())return null;
        Document page=Jsoup.parse(html,url);if(!matches(page,url))return null;
        for(Element server:page.select("a[data-src]")) {
            String label=normalize(server.text()),target;
            try{target=new String(ByseClient.decode(server.attr("data-src")),StandardCharsets.UTF_8);}catch(Exception ignored){continue;}
            boolean candidate=byse(target)||label.contains("byse")||label.contains("filemoon");
            if(!candidate)continue;
            if(!new URL(target).getHost().equals("desenefaine.com")&&!byse(target))continue;
            String embed=wrapper(target);
            if(embed!=null){pageUrl=url;return embed;}
        }
        return null;
    }
    public String find(JSONObject data) throws Exception {
        season=data.optInt("season",0);episode=data.optInt("episode",0);series=season>0&&episode>0;
        if(!data.optString("movieName","").isEmpty())names.add(data.getString("movieName"));
        for(String field:Arrays.asList("localTitle","originalTitle"))if(!data.optString(field,"").isEmpty())names.add(data.getString(field));
        year=data.optInt("releaseYear",0);
        int id=data.optInt("tmdbID",0);
        if(id>0&&!data.has("localTitle")){
            JSONObject meta=new JSONObject(get("https://api.themoviedb.org/3/"+(series?"tv/":"movie/")+id+"?language=ro-RO&api_key="+TMDB_KEY));
            String local=meta.optString(series?"name":"title",""),original=meta.optString(series?"original_name":"original_title","");
            if(!local.isEmpty())names.add(local);if(!original.isEmpty())names.add(original);
            String date=meta.optString(series?"first_air_date":"release_date","");if(date.length()>=4)year=Integer.parseInt(date.substring(0,4));
        }
        if(names.isEmpty())return null;
        // Prefer localized slugs, then metadata title; no title-specific URL table.
        List<String> titles=new ArrayList<>(names);Collections.reverse(titles);
        for(String name:titles){
            String base=SITE+(series?"/epi/":"/film/")+slug(name);
            List<String> candidates=series?Arrays.asList(base+"-sezonul-"+season+"-episodul-"+episode+"/",base+"-s"+season+"-ep"+episode+"/"):Collections.singletonList(base+"/");
            for(String u:candidates){String found=inspect(u);if(found!=null)return found;}
        }
        for(String name:titles){
            Document search=Jsoup.parse(get(SITE+"/?s="+URLEncoder.encode(name,"UTF-8")),SITE);
            int count=0;
            for(Element a:search.select("article a[href], .search-page a[href]")){
                String u=a.absUrl("href");
                if(!u.startsWith(SITE+(series?"/epi/":"/film/"))||visited.contains(u))continue;
                if(++count>6)break;
                String found=inspect(u);if(found!=null)return found;
            }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        NativeCatalog c=new NativeCatalog();
        JSONObject input=new JSONObject().put("tmdbID",Integer.parseInt(args[0]));
        if(args.length>2)input.put("season",Integer.parseInt(args[1])).put("episode",Integer.parseInt(args[2]));
        String embed=c.find(input);
        if(embed==null)throw new IOException("No matching Byse server");
        System.out.println("CATALOG PASS matched title and Byse server");
        ByseClient resolver=new ByseClient();JSONObject r=resolver.resolve(embed);
        System.out.println("FRESH END-TO-END sources="+r.getJSONArray("sources").length()+" powMs="+resolver.powMillis+" resolverMs="+resolver.totalMillis);
    }
}
