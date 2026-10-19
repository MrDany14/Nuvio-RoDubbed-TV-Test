import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

final class HlsProbe {
    static String attribute(String line,String name){Matcher m=Pattern.compile("(?:[:,])"+name+"=(?:\"([^\"]*)\"|([^,]*))").matcher(line);return m.find()?(m.group(1)!=null?m.group(1):m.group(2)):"";}
    static void validate(ResolvedStream stream)throws Exception{
        String current=stream.url,body="";
        for(int depth=0;depth<3;depth++){
            body=NativeHttp.text(current,NativeHttp.headers(stream.referer)).trim();
            if(!body.startsWith("#EXTM3U"))throw new Exception("Not an HLS playlist");
            RomanianAudio.checkMaster(body);
            String[] lines=body.split("\\r?\\n");String chosen=null;long best=-1;
            for(int i=0;i+1<lines.length;i++)if(lines[i].startsWith("#EXT-X-STREAM-INF:")){
                String resolution=attribute(lines[i],"RESOLUTION");int height=0;
                try{height=Integer.parseInt(resolution.substring(resolution.indexOf('x')+1));}catch(Exception ignored){}
                long rank=height>1080?0:height+1;
                String next=lines[i+1].trim();if(!next.isEmpty()&&!next.startsWith("#")&&rank>best){best=rank;chosen=new URL(new URL(current),next).toString();}
            }
            if(chosen==null)break;current=chosen;
        }
        if(body.contains("#EXT-X-STREAM-INF:"))throw new Exception("HLS nesting limit exceeded");
        String segment=null;
        for(String line:body.split("\\r?\\n"))if(!line.trim().isEmpty()&&!line.startsWith("#")){segment=new URL(new URL(current),line.trim()).toString();break;}
        if(segment==null)throw new Exception("HLS contains no media segment");
        byte[] sample=NativeHttp.bytes(segment,NativeHttp.headers(stream.referer),4096,true);
        String prefix=new String(sample,0,Math.min(sample.length,80),StandardCharsets.US_ASCII).trim().toLowerCase(java.util.Locale.ROOT);
        if(sample.length<16||prefix.startsWith("<!doctype")||prefix.startsWith("<html")||prefix.startsWith("{\"error"))throw new Exception("Media segment is an error response");
    }
}
