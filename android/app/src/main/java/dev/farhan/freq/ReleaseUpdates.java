package dev.farhan.freq;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;

/** Downloads only this repository's APKs, then pins package, version, hash and installed signer. */
final class ReleaseUpdates {
 static final String REPO="retr0sdq/freq-home";
 static final String RELEASES="https://github.com/"+REPO+"/releases";
 private JSONObject candidate;
 private final Context context;
 ReleaseUpdates(Context context){this.context=context;}
 JSONObject check()throws Exception{
  PackageInfo local=context.getPackageManager().getPackageInfo(context.getPackageName(),0);int current=local.versionCode;
  HttpURLConnection connection=connect(new URL(RELEASES+"/latest/download/update.json"));
  try{if(connection.getResponseCode()==404){candidate=null;return new JSONObject().put("available",false).put("published",false).put("version",local.versionName).put("versionCode",current).put("currentVersion",local.versionName);}
   if(connection.getResponseCode()!=200)throw new IOException("Release server returned "+connection.getResponseCode());
   byte[] bytes=readLimited(connection.getInputStream(),32768);JSONObject m=new JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));
   if(!context.getPackageName().equals(m.getString("packageName"))||!m.getString("sha256").matches("[0-9a-fA-F]{64}"))throw new IOException("Invalid update metadata.");
   URL apk=new URL(m.getString("apkUrl"));if(!"https".equals(apk.getProtocol())||!"github.com".equals(apk.getHost())||apk.getUserInfo()!=null||!apk.getPath().startsWith("/"+REPO+"/releases/download/"))throw new IOException("Unexpected update source.");
   boolean available=m.getInt("versionCode")>current;candidate=available?m:null;
   return new JSONObject().put("available",available).put("published",true).put("version",m.getString("versionName")).put("versionCode",m.getInt("versionCode")).put("currentVersion",local.versionName);
  }finally{connection.disconnect();}
 }
 File downloadVerified()throws Exception{
  JSONObject m=candidate;if(m==null)throw new IOException("Check for updates first.");
  File directory=new File(context.getCacheDir(),"updates");directory.mkdirs();File part=new File(directory,"candidate.part"),apk=new File(directory,"freq-update.apk");
  HttpURLConnection c=connect(new URL(m.getString("apkUrl")));
  try{if(c.getResponseCode()!=200)throw new IOException("Could not download update.");MessageDigest hash=MessageDigest.getInstance("SHA-256");long total=0;
   try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(part)){byte[] buf=new byte[32768];int n;while((n=in.read(buf))!=-1){total+=n;if(total>50L*1024*1024)throw new IOException("Update is too large.");hash.update(buf,0,n);out.write(buf,0,n);}}
   if(!hex(hash.digest()).equalsIgnoreCase(m.getString("sha256")))throw new IOException("Update checksum did not match.");
   int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
   PackageInfo next=context.getPackageManager().getPackageArchiveInfo(part.getAbsolutePath(),flags),local=context.getPackageManager().getPackageInfo(context.getPackageName(),flags);
   if(next==null||!local.packageName.equals(next.packageName)||next.versionCode!=m.getInt("versionCode")||next.versionCode<=local.versionCode||!signers(local).equals(signers(next)))throw new IOException("Update signature or package did not match this installation.");
   if(apk.exists()&&!apk.delete())throw new IOException("Could not replace old download.");if(!part.renameTo(apk))throw new IOException("Could not prepare update.");return apk;
  }finally{c.disconnect();part.delete();}
 }
 private static Set<String> signers(PackageInfo p)throws Exception{Signature[] signatures=Build.VERSION.SDK_INT>=28?p.signingInfo.getApkContentsSigners():p.signatures;Set<String> out=new HashSet<>();if(signatures==null||signatures.length==0)throw new IOException("Missing APK signer.");for(Signature s:signatures)out.add(hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())));return out;}
 private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
 private static byte[] readLimited(InputStream in,int max)throws IOException{try(in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("Metadata too large.");out.write(b,0,n);}return out.toByteArray();}}
 private static HttpURLConnection connect(URL url)throws IOException{for(int i=0;i<6;i++){if(!"https".equals(url.getProtocol())||url.getUserInfo()!=null)throw new IOException("HTTPS required.");String host=url.getHost();if(!(host.equals("github.com")||host.equals("release-assets.githubusercontent.com")||host.equals("objects.githubusercontent.com")))throw new IOException("Untrusted download host.");HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","FREQ-Home");int code=c.getResponseCode();if(code>=300&&code<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("Missing redirect.");url=new URL(url,location);continue;}return c;}throw new IOException("Too many redirects.");}
}
