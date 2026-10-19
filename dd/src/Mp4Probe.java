final class Mp4Probe {
 static long u32(byte[] b,int p){return ((b[p]&255L)<<24)|((b[p+1]&255L)<<16)|((b[p+2]&255L)<<8)|(b[p+3]&255L);}
 static String type(byte[] b,int p){return new String(b,p,4,java.nio.charset.StandardCharsets.US_ASCII);}
 static double movieDuration(byte[] b)throws Exception{
  if(b.length<24||!type(b,4).equals("ftyp"))throw new Exception("Not MP4 media");
  for(int p=0;p+8<=b.length;){long size=u32(b,p);if(size<8||size>Integer.MAX_VALUE)break;String box=type(b,p+4);if(box.equals("moov")){
   int end=(int)Math.min(b.length,p+size);for(int q=p+8;q+8<=end;){long child=u32(b,q);if(child<8||child>Integer.MAX_VALUE)break;if(type(b,q+4).equals("mvhd")){if(q+40>end)break;int version=b[q+8]&255,o=q+8+(version==1?20:12);if(o+(version==1?12:8)>end)break;long scale=u32(b,o),ticks=version==1?(u32(b,o+4)<<32)|u32(b,o+8):u32(b,o+4);if(scale>0)return (double)ticks/scale;break;}if(q+child>end)break;q+=(int)child;}
  }if(p+size>b.length)break;p+=(int)size;}
  throw new Exception("MP4 duration unavailable in bounded header");
 }
 static void validate(ResolvedStream stream,double expected)throws Exception{
  double actual=movieDuration(NativeHttp.bytes(stream.url,NativeHttp.headers(stream.referer),4096,true));
  if(actual<90||Math.abs(actual-expected)>Math.max(5,expected*.05))throw new Exception("MP4 duration does not match requested episode");
 }
}
