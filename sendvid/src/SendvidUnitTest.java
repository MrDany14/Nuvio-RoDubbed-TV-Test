public final class SendvidUnitTest {
 static void yes(boolean value,String message){if(!value)throw new AssertionError(message);}
 static String html(String url){return "<video id='video-js-video'><source id='video_source' type='video/mp4' src='"+url+"'></video>";}
 static void rejects(String html,String embed)throws Exception{try{SendvidClient.source(html,embed);throw new AssertionError("Unexpected source accepted");}catch(AssertionError e){throw e;}catch(Exception expected){}}
 public static void main(String[] args)throws Exception{
  String embed="https://sendvid.com/embed/abc123",url="https://videos2.sendvid.com/a/b/abc123.mp4?token=one%2Btwo&amp;validto=123";
  yes(SendvidClient.source(html(url),embed).endsWith("token=one%2Btwo&validto=123"),"Signed query and HTML entity preserved");
  rejects(html("https://ads.invalid/abc123.mp4"),embed);rejects(html("https://videos2.sendvid.com/other.mp4"),embed);
  rejects(html("http://videos2.sendvid.com/abc123.mp4"),embed);rejects(html(url),"https://sendvid.com.evil.invalid/embed/abc123");
  rejects(html(url).replace("video-js-video","ad-video"),embed);
  rejects("<script>var video_source='https://ads.invalid/video.mp4';</script>",embed);
  byte[] bytes=new byte[32];bytes[3]=32;bytes[4]='f';bytes[5]='t';bytes[6]='y';bytes[7]='p';
  yes(Mp4Probe.validHeader(bytes),"MP4 file-type atom");bytes[3]=64;yes(!Mp4Probe.validHeader(bytes),"Truncated MP4 atom rejected");
  yes(!Mp4Probe.validHeader("<html>Access denied</html>".getBytes("UTF-8")),"HTML error rejected");
  yes(!Mp4Probe.validHeader("#EXTM3U playlist".getBytes("UTF-8")),"MP4 path does not accept HLS");
  yes(new ResolvedStream("Byse","url","ref","id",0).isHls,"Existing hosts remain HLS");
  System.out.println("PASS Sendvid signed URLs, embed/media identity, ad rejection and MP4 validation");
 }
}
