import java.util.*;

final class RomanianAudio {
    static boolean isRomanian(String value){String s=NativeCatalog.normalize(value);return s.matches(".*\\b(?:ro|ron|rum|romana|romanian|romaneste)\\b.*");}
    static boolean declaredRomanian(String label){
        String s=NativeCatalog.normalize(label);
        if(s.matches(".*\\b(?:english|engleza|eng|en|subtitrat|subtitrata|subtitrare|subbed)\\b.*"))return false;
        return isRomanian(s);
    }
    static boolean acceptServer(String label,String pageTitle){
        String s=NativeCatalog.normalize(label);
        if(s.matches(".*\\b(?:english|engleza|eng|en|subtitrat|subtitrata|subtitrare|subbed)\\b.*"))return false;
        boolean hostOnly=s.replaceFirst("^server \\d+ ?","").matches("(?:voe|filemoon|byse|streamp2p|seekstreaming|seeksreaming)");
        return declaredRomanian(label)||(hostOnly&&NativeCatalog.normalize(pageTitle).matches(".*\\bdublat(?:a)? (?:in )?romana\\b.*"));
    }
    static void checkHostDefault(String language)throws Exception{
        if(language!=null&&!language.trim().isEmpty()&&!isRomanian(language))throw new Exception("Non-Romanian default audio");
    }
    static void checkMaster(String playlist)throws Exception{
        boolean hasAudio=false,hasRomanian=false,foreignDefault=false;
        for(String line:playlist.split("\\r?\\n")){
            if(!line.startsWith("#EXT-X-MEDIA:")||!line.contains("TYPE=AUDIO"))continue;
            hasAudio=true;String lang=HlsProbe.attribute(line,"LANGUAGE"),name=HlsProbe.attribute(line,"NAME");
            boolean ro=(lang.isEmpty()||lang.equalsIgnoreCase("und"))?isRomanian(name):isRomanian(lang);hasRomanian|=ro;
            if(!ro&&"YES".equals(HlsProbe.attribute(line,"DEFAULT")))foreignDefault=true;
        }
        if(hasAudio&&!hasRomanian)throw new Exception("HLS has no identified Romanian audio");
        if(foreignDefault)throw new Exception("HLS defaults to foreign audio; Romanian auto-selection not established");
    }
}
