import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;
/** Uses the HTTP client bundled by Nuvio. No WebView, browser cookies or TLS overrides. */
final class PlaymogoHttp {
 static final Class<?> CLIENT=type("okhttp3.OkHttpClient"),BUILDER=type("okhttp3.OkHttpClient$Builder"),REQUEST=type("okhttp3.Request"),REQUEST_BUILDER=type("okhttp3.Request$Builder"),CALL=type("okhttp3.Call"),RESPONSE=type("okhttp3.Response"),BODY=type("okhttp3.ResponseBody");
 static Class<?> type(String name){try{return Class.forName(name);}catch(Exception e){throw new IllegalStateException("Nuvio native HTTP API unavailable",e);}}
 static final Object BASE=baseClient();
 static Object baseClient(){try{return BUILDER.getMethod("build").invoke(BUILDER.getConstructor().newInstance());}catch(Exception e){throw new IllegalStateException("Nuvio native HTTP client unavailable",e);}}
 static final class Page {final String html,url,userAgent;Page(String h,String u,String a){html=h;url=u;userAgent=a;}}
 static Page get(String url,String referer)throws Exception{
  if(!PlaymogoClient.requestUrl(url))throw new Exception("Unverified Playmogo request");String original=url;for(int hop=0;hop<5;hop++){
  NativeHttp.Session session=NativeHttp.session();session.check();long timeout=Math.max(1,Math.min(6000,(session.deadline-System.nanoTime())/1000000));Object builder=CLIENT.getMethod("newBuilder").invoke(BASE);for(String method:Arrays.asList("connectTimeout","readTimeout","callTimeout"))BUILDER.getMethod(method,long.class,TimeUnit.class).invoke(builder,timeout,TimeUnit.MILLISECONDS);
  BUILDER.getMethod("followRedirects",boolean.class).invoke(builder,false);BUILDER.getMethod("followSslRedirects",boolean.class).invoke(builder,false);
  Object client=BUILDER.getMethod("build").invoke(builder),rb=REQUEST_BUILDER.getConstructor().newInstance();REQUEST_BUILDER.getMethod("url",String.class).invoke(rb,url);REQUEST_BUILDER.getMethod("header",String.class,String.class).invoke(rb,"Referer",referer);Object request=REQUEST_BUILDER.getMethod("build").invoke(rb),call=CLIENT.getMethod("newCall",REQUEST).invoke(client,request),response=null;
  final Object pending=call;Runnable cancel=new Runnable(){public void run(){try{CALL.getMethod("cancel").invoke(pending);}catch(Exception ignored){}}};session.cancelActions.add(cancel);
  try{session.check();response=CALL.getMethod("execute").invoke(call);int status=(Integer)RESPONSE.getMethod("code").invoke(response);
   if(status==301||status==302||status==303||status==307||status==308){String location=(String)RESPONSE.getMethod("header",String.class).invoke(response,"Location");if(location==null)throw new Exception("Playmogo redirect missing location");String next=new java.net.URL(new java.net.URL(url),location).toString();PlaymogoClient.verifyRedirect(original,next);referer=url;url=next;continue;}
   if(status!=200)throw new Exception("Playmogo HTTP "+status);Object finalRequest=RESPONSE.getMethod("request").invoke(response);String actualUrl=REQUEST.getMethod("url").invoke(finalRequest).toString(),agent=(String)REQUEST.getMethod("header",String.class).invoke(finalRequest,"User-Agent");Object peek=RESPONSE.getMethod("peekBody",long.class).invoke(response,1024L*1024);String html=(String)BODY.getMethod("string").invoke(peek);if(html.length()>=1024*1024)throw new Exception("Playmogo response too large");session.check();return new Page(html,actualUrl,agent);}
  catch(InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw (Exception)cause;throw e;}finally{session.cancelActions.remove(cancel);if(response!=null)RESPONSE.getMethod("close").invoke(response);cancel.run();}
  }throw new Exception("Playmogo redirect limit reached");
 }
}
