final class Mp4Probe {
    static boolean validHeader(byte[] data){
        if(data.length<16||data[4]!='f'||data[5]!='t'||data[6]!='y'||data[7]!='p')return false;
        long size=((data[0]&255L)<<24)|((data[1]&255L)<<16)|((data[2]&255L)<<8)|(data[3]&255L);
        return size>=16&&size<=data.length;
    }
    static void validate(ResolvedStream stream)throws Exception{
        byte[] data=NativeHttp.bytes(stream.url,NativeHttp.headers(stream.referer),4096,true);
        if(!validHeader(data))throw new Exception("Not an MP4 media response");
    }
}
