import org.json.*;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.*;
import java.util.zip.InflaterInputStream;

/** Netu candidate flow; unsupported player/browser checks fail closed. Not yet validated as working. */
final class NetuClient {
    static String match(String html,String pattern)throws Exception{Matcher m=Pattern.compile(pattern).matcher(html);if(!m.find())throw new Exception("Netu playback metadata unavailable");return m.group(1);}
    static ResolvedStream resolve(NativeCatalog.Candidate c)throws Exception{
        String html=NativeHttp.text(c.embed,NativeHttp.headers(c.page)),origin=ByseClient.origin(c.embed);
        if(org.jsoup.Jsoup.parse(html).text().toLowerCase(java.util.Locale.ROOT).contains("can't find the file"))throw new Exception("Netu file deleted or unavailable");
        String video=match(html,"[\"']videoid[\"']\\s*:\\s*[\"'](\\d+)[\"']");
        String key=match(html,"[\"']videokey[\"']\\s*:\\s*[\"']([^\"']+)[\"']"),advertiser=match(html,"\\badbn\\s*=\\s*[\"']([^\"']+)[\"']");
        Map<String,String> h=NativeHttp.headers(c.embed);h.put("X-Requested-With","XMLHttpRequest");h.put("Accept","application/json");
        JSONObject challenge=NativeHttp.post(origin+"/player/get_player_image.php",h,new JSONObject().put("videoid",video).put("videokey",key).put("width",400).put("height",400));
        if(!challenge.optBoolean("success",false))throw new Exception("Netu challenge unavailable");
        int[] point=point(challenge.getString("image"));byte[] entropy=new byte[20];new SecureRandom().nextBytes(entropy);
        JSONObject request=new JSONObject().put("adb",advertiser).put("sh",SharedPlayerClient.hex(entropy)).put("ver","4").put("secure","0").put("htoken","")
            .put("v",SharedPlayerClient.encode(key)).put("token","").put("gt","").put("embed_from","0").put("wasmcheck",1).put("adscore","")
            .put("click_hash",SharedPlayerClient.encode(challenge.getString("hash_image"))).put("clickx",point[0]).put("clicky",point[1]);
        JSONObject result=NativeHttp.post(origin+"/player/get_md5.php",h,request);
        if(result.optInt("need_captcha",0)==1||result.optInt("blocked",0)==1||result.optInt("try_again",0)==1)throw new Exception("Netu requires unsupported browser verification");
        if(result.optInt("pending",0)!=0)throw new Exception("Netu video unavailable");
        String value=result.optString("obf_link","");
        if(value.matches("#[a-fA-F0-9]+")&&(value.length()-1)%3==0){StringBuilder decoded=new StringBuilder();for(int i=1;i<value.length();i+=3)decoded.append((char)Integer.parseInt(value.substring(i,i+3),16));value=decoded.toString();}
        if(!value.matches("//[a-zA-Z0-9.-]+/.*"))throw new Exception("Netu source unavailable");
        if(!value.matches(".*\\.mp4\\.m3u8(?:[?#].*)?"))value+=".mp4.m3u8";
        return new ResolvedStream("Netu","https:"+value,c.embed,c.identity(),0);
    }
    static int[] point(String uri)throws Exception{
        if(!uri.startsWith("data:image/png;base64,"))throw new Exception("Unsupported Netu image");
        byte[] png=ByseClient.decode(uri.substring(uri.indexOf(',')+1));if(png.length>524288)throw new Exception("Image too large");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(png));if(in.readLong()!=0x89504e470d0a1a0aL)throw new Exception("Invalid PNG");
        int w=0,h=0;byte[] alpha=null;ByteArrayOutputStream compressed=new ByteArrayOutputStream();
        while(in.available()>=12){int length=in.readInt();if(length<0||length>in.available()-8)throw new Exception("Invalid PNG chunk");byte[] name=new byte[4];in.readFully(name);String type=new String(name,"US-ASCII");byte[] data=new byte[length];in.readFully(data);in.readInt();
            if(type.equals("IHDR")){DataInputStream header=new DataInputStream(new ByteArrayInputStream(data));w=header.readInt();h=header.readInt();if(w<1||h<1||w>2048||h>2048||(long)w*h>1048576||header.readUnsignedByte()!=8||header.readUnsignedByte()!=3||header.readUnsignedByte()!=0||header.readUnsignedByte()!=0||header.readUnsignedByte()!=0)throw new Exception("Unsupported PNG format");}
            else if(type.equals("tRNS"))alpha=data;else if(type.equals("IDAT"))compressed.write(data);else if(type.equals("IEND"))break;
        }
        if(w==0||alpha==null)throw new Exception("Netu image transparency missing");
        InputStream raw=new InflaterInputStream(new ByteArrayInputStream(compressed.toByteArray()));int[] previous=new int[w],current=new int[w];long weight=0,xsum=0,ysum=0;
        for(int y=0;y<h;y++){int filter=raw.read();if(filter<0||filter>4)throw new Exception("Invalid PNG filter");for(int x=0;x<w;x++){
            int sample=raw.read();if(sample<0)throw new Exception("Truncated image");int left=x>0?current[x-1]:0,above=previous[x],diagonal=x>0?previous[x-1]:0,predict=0;
            if(filter==1)predict=left;else if(filter==2)predict=above;else if(filter==3)predict=(left+above)/2;else if(filter==4){int p=left+above-diagonal,a=Math.abs(p-left),b=Math.abs(p-above),d=Math.abs(p-diagonal);predict=a<=b&&a<=d?left:b<=d?above:diagonal;}
            int index=current[x]=(sample+predict)&255,a=index<alpha.length?alpha[index]&255:255;weight+=a;xsum+=(long)x*a;ysum+=(long)y*a;
        }int[] swap=previous;previous=current;current=swap;}
        raw.close();if(weight==0||weight>(long)w*h*128)throw new Exception("Netu play marker missing");return new int[]{(int)Math.round((double)xsum/weight),(int)Math.round((double)ysum/weight)};
    }
}
