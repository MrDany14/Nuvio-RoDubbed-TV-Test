import org.json.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

/** Supplies Nuvio's native movie/episode models without relying on inherited TMDB loading. */
public final class NativeMetadata {
    static Object withDefaults(String className,Object... required) throws Exception {
        for(Constructor<?> ctor:Class.forName(className).getConstructors()){
            Class<?>[] types=ctor.getParameterTypes();
            if(types.length<required.length+2||!types[types.length-1].getName().equals("kotlin.jvm.internal.DefaultConstructorMarker"))continue;
            int fields=types.length-2;if(fields>31)continue;
            Object[] args=new Object[types.length];
            for(int i=0;i<fields;i++){
                if(i<required.length)args[i]=required[i];
                else if(types[i]==boolean.class)args[i]=false;
                else if(types[i]==int.class)args[i]=0;
                else if(types[i]==long.class)args[i]=0L;
                else if(types[i]==float.class)args[i]=0f;
                else if(types[i]==double.class)args[i]=0d;
            }
            args[fields]=(-1)<<required.length;args[fields+1]=null;
            return ctor.newInstance(args);
        }
        throw new IllegalStateException("Unsupported Nuvio model: "+className);
    }
    static JSONObject link(int id,JSONObject metadata,boolean tv) throws Exception {
        String title=metadata.getString(tv?"name":"title"),original=metadata.optString(tv?"original_name":"original_title",title);
        String date=metadata.optString(tv?"first_air_date":"release_date","");
        return new JSONObject().put("tmdbID",id).put("movieName",title).put("localTitle",title).put("originalTitle",original)
            .put("releaseYear",date.length()>=4?Integer.parseInt(date.substring(0,4)):0);
    }
    static Object load(String input,String apiName)throws Exception{
        int id;boolean tv;
        if(input.startsWith("{")){JSONObject j=new JSONObject(input);id=j.getInt("id");tv="tv".equals(j.optString("type"));}
        else{
            Matcher m=Pattern.compile("/(movie|tv)/(\\d+)").matcher(input);
            if(!m.find())throw new IllegalArgumentException("Invalid metadata input");tv=m.group(1).equals("tv");id=Integer.parseInt(m.group(2));
        }
        if(id<=0)throw new IllegalArgumentException("Invalid TMDB ID");
        JSONObject metadata=new JSONObject(new NativeCatalog().get("https://api.themoviedb.org/3/"+(tv?"tv/":"movie/")+id+"?language=ro-RO&api_key="+NativeCatalog.TMDB_KEY));
        JSONObject data=link(id,metadata,tv);
        String name=data.getString("movieName"),url="https://www.themoviedb.org/"+(tv?"tv/":"movie/")+id;
        Class<? extends Enum> type=Class.forName("com.lagradost.cloudstream3.TvType").asSubclass(Enum.class);
        Object tvType=Enum.valueOf(type,tv?"TvSeries":"Movie");
        if(!tv)return withDefaults("com.lagradost.cloudstream3.MovieLoadResponse",name,url,apiName,tvType,data.toString());
        List<Object> episodes=new ArrayList<>();JSONArray seasons=metadata.getJSONArray("seasons");
        for(int i=0;i<seasons.length();i++){
            JSONObject s=seasons.getJSONObject(i);int number=s.getInt("season_number"),count=s.getInt("episode_count");
            for(int n=1;n<=count;n++){
                JSONObject item=new JSONObject(data.toString()).put("season",number).put("episode",n);
                Object ep=withDefaults("com.lagradost.cloudstream3.Episode",item.toString());
                ep.getClass().getMethod("setSeason",Integer.class).invoke(ep,number);
                ep.getClass().getMethod("setEpisode",Integer.class).invoke(ep,n);episodes.add(ep);
            }
        }
        return withDefaults("com.lagradost.cloudstream3.TvSeriesLoadResponse",name,url,apiName,tvType,episodes);
    }
}
