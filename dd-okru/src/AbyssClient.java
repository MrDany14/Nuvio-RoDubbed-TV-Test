import java.net.*;import java.nio.charset.*;import java.security.*;import java.util.*;import java.util.regex.*;import javax.crypto.*;import javax.crypto.spec.*;import org.json.*;import org.jsoup.Jsoup;
final class AbyssClient {
 static void verifySeekRange(String url,String referer,long size)throws Exception{
  NativeHttp.Session session=NativeHttp.session();session.check();long offset=Math.min(size-2048,Math.max(5*1024*1024,size/10));java.net.HttpURLConnection connection=(java.net.HttpURLConnection)new URL(url).openConnection();NativeHttp.attach(connection);
  try{int timeout=(int)Math.max(1,Math.min(2500,(session.deadline-System.nanoTime())/1000000));connection.setConnectTimeout(timeout);connection.setReadTimeout(timeout);connection.setInstanceFollowRedirects(true);for(Map.Entry<String,String> h:mediaHeaders(referer).entrySet())connection.setRequestProperty(h.getKey(),h.getValue());connection.setRequestProperty("Range","bytes="+offset+"-"+(offset+2047));
   if(connection.getResponseCode()!=206)throw new Exception("Abyss media seek unavailable");String range=connection.getHeaderField("Content-Range");if(range==null||!range.equals("bytes "+offset+"-"+(offset+2047)+"/"+size))throw new Exception("Abyss media seek range mismatch");int count=0;try(java.io.InputStream input=connection.getInputStream()){byte[] buffer=new byte[2048];while(count<buffer.length){session.check();int n=input.read(buffer,count,buffer.length-count);if(n<0)break;count+=n;}}if(count!=2048)throw new Exception("Abyss media seek data incomplete");
  }finally{NativeHttp.detach(connection);connection.disconnect();}
 }
 static Map<String,String> mediaHeaders(String referer)throws Exception{Map<String,String> headers=NativeHttp.headers(referer);headers.remove("Origin");headers.put("User-Agent",ByseClient.USER_AGENT);return headers;}
 static String md5Key(byte[] input)throws Exception{byte[] digest=MessageDigest.getInstance("MD5").digest(input);StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
 static byte[] crypt(byte[] input,byte[] keyInput,int mode)throws Exception{byte[] key=md5Key(keyInput).getBytes(StandardCharsets.US_ASCII);Cipher cipher=Cipher.getInstance("AES/CTR/NoPadding");cipher.init(mode,new SecretKeySpec(key,"AES"),new IvParameterSpec(Arrays.copyOf(key,16)));return cipher.doFinal(input);}
 static byte[] numericKey(long number)throws Exception{if(number<=0)throw new Exception("Invalid media size");String digits=Long.toString(number);byte[] result=new byte[digits.length()];for(int i=0;i<result.length;i++)result[i]=(byte)(digits.charAt(i)-'0');return result;}
 static String token(String path,long size)throws Exception{byte[] encrypted=crypt(path.getBytes(StandardCharsets.UTF_8),numericKey(size),Cipher.ENCRYPT_MODE);String inner=Base64.getEncoder().withoutPadding().encodeToString(encrypted);return Base64.getEncoder().withoutPadding().encodeToString(inner.getBytes(StandardCharsets.US_ASCII));}
 static JSONObject config(String html,String embed)throws Exception{
  Matcher m=Pattern.compile("const\\s+datas\\s*=\\s*\"([A-Za-z0-9+/=]+)\"").matcher(html);if(!m.find())throw new Exception("Abyss player metadata missing");
  JSONObject data=new JSONObject(new String(Base64.getDecoder().decode(m.group(1)),StandardCharsets.ISO_8859_1));String slug=new URL(embed).getPath().replaceFirst("^/","").replaceFirst("/$","");
  if(!slug.matches("[A-Za-z0-9_-]{7,17}")||!slug.equals(data.optString("slug"))||data.optLong("user_id")<=0||data.optLong("md5_id")<=0)throw new Exception("Abyss video identity mismatch");
  String key=data.getLong("user_id")+":"+slug+":"+data.getLong("md5_id");byte[] ciphertext=data.getString("media").getBytes(StandardCharsets.ISO_8859_1);JSONObject media=new JSONObject(new String(crypt(ciphertext,key.getBytes(StandardCharsets.UTF_8),Cipher.DECRYPT_MODE),StandardCharsets.UTF_8));data.put("media",media);return data;
 }
 static String mediaUrl(JSONObject data,JSONObject source)throws Exception{
  long size=source.getLong("size");int res=source.getInt("res_id");String sub=source.getString("sub"),domain=null;if(!sub.matches("[a-z0-9]+")||res<1||res>5||size<1000000||size>100000000000L)throw new Exception("Invalid Abyss media identity");
  JSONArray domains=data.getJSONObject("media").getJSONObject("mp4").getJSONArray("domains");for(int i=0;i<domains.length();i++){String host=domains.getString(i);if(host.matches("[a-z0-9]+\\.sssrr\\.org")&&host.split("\\.")[0].equals(sub)){domain=host;break;}}
  if(domain==null)throw new Exception("Abyss media service unavailable");String path="/mp4/"+data.getLong("md5_id")+"/"+res+"/"+size+"?v="+data.getString("slug");return "https://"+domain+"/sora/"+size+"/"+token(path,size);
 }
 static ResolvedStream resolve(NativeCatalog.Candidate candidate)throws Exception{
  if(!FdCatalog.siteUrl(candidate.page)||candidate.expected==null||!NativeCatalog.kind(candidate.embed).equals("Abyss"))throw new Exception("Abyss provenance not enabled");int expected=candidate.expected.optInt("durationSeconds");if(expected<90)throw new Exception("Expected duration missing");
  String html;try{html=NativeHttp.text(candidate.embed,NativeHttp.headers(candidate.page));}catch(Exception e){throw new Exception("Abyss player: "+e.getMessage());}String title=Jsoup.parse(html).title().trim();if(title.isEmpty()||title.toLowerCase(Locale.ROOT).matches(".*(?:deleted|unavailable|not found).*"))throw new Exception("Abyss video unavailable");
  JSONObject data=config(html,candidate.embed);JSONArray all=data.getJSONObject("media").getJSONObject("mp4").getJSONArray("sources");List<JSONObject> usable=new ArrayList<>();for(int i=0;i<all.length();i++){JSONObject source=all.getJSONObject(i);if(source.optBoolean("status")&&source.optString("codec").equals("h264")&&source.optString("label").matches("(?:480|720|1080)p"))usable.add(source);}
  Collections.sort(usable,new Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){return b.optInt("res_id")-a.optInt("res_id");}});
  Exception failure=new Exception("No supported Abyss MP4");for(JSONObject source:usable){try{NativeHttp.session().check();String url=mediaUrl(data,source);double actual=Mp4Probe.movieDuration(NativeHttp.bytes(url,mediaHeaders(candidate.embed),4096,true));if(actual<90||Math.abs(actual-expected)>Math.max(60,expected*.10))throw new Exception("Abyss runtime mismatch: expected "+expected+"s, actual "+Math.round(actual)+"s");verifySeekRange(url,candidate.embed,source.getLong("size"));return new ResolvedStream("Abyss",url,candidate.embed,candidate.identity(),Integer.parseInt(source.getString("label").replace("p","")),"FilmeDublate",false);}catch(Exception e){failure=e;}}
  throw failure;
 }
}

