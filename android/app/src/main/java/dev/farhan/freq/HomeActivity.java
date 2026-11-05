package dev.farhan.freq;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Base64;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Bundled launcher. Native messages are accepted only from the asset origin main frame. */
public final class HomeActivity extends Activity {
 private static final String ORIGIN="https://appassets.androidplatform.net";
 private static final String HOME=ORIGIN+"/assets/ui/index.html";
 private static final int PICK_FOLDER=10;
 private WebView web;
 private ReleaseUpdates updates;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private androidx.webkit.JavaScriptReplyProxy folderReply;
 private String folderRequest;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private Runnable watchdog;
 private boolean ready=false;

 @Override public void onCreate(Bundle state){super.onCreate(state);updates=new ReleaseUpdates(this);buildWeb();}
 private void buildWeb(){
  web=new WebView(this);setContentView(web);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
  s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSupportMultipleWindows(false);
  WebView.setWebContentsDebuggingEnabled(false);
  WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){
    Uri u=req.getUrl();if(isAsset(u))return loader.shouldInterceptRequest(u);
    return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
   }
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){return !isAsset(req.getUrl());}
   @Override public void onPageFinished(WebView v,String url){/* Readiness is acknowledged by React over the native bridge. */}
   @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){v.destroy();ready=false;showRecovery();return true;}
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onJsConfirm(WebView view,String url,String message,JsResult result){new AlertDialog.Builder(HomeActivity.this).setMessage(message).setPositiveButton("Delete",(d,w)->result.confirm()).setNegativeButton("Cancel",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;}
  });
  if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)){showRecovery();return;}
  WebViewCompat.addWebMessageListener(web,"FreqNative",Collections.singleton(ORIGIN),(v,msg,origin,main,reply)->{
   if(!main||!ORIGIN.equals(origin.toString()))return;
   try{JSONObject m=new JSONObject(msg.getData());String id=m.getString("id"),action=m.getString("action");handle(id,action,m,reply);}catch(Exception e){/* Reject malformed bridge messages. */}
  });
  watchdog=()->{if(!ready&&!isFinishing())showRecovery();};handler.postDelayed(watchdog,10000);
  web.loadUrl(HOME);
 }
 private static boolean isAsset(Uri u){return "https".equals(u.getScheme())&&"appassets.androidplatform.net".equals(u.getHost())&&u.getPath()!=null&&u.getPath().startsWith("/assets/");}
 private void handle(String id,String action,JSONObject m,androidx.webkit.JavaScriptReplyProxy reply){
  try{switch(action){
   case "openReleases": startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(ReleaseUpdates.RELEASES)));respond(reply,id,true,null);break;
   case "checkUpdate": worker.execute(()->{try{respond(reply,id,updates.check(),null);}catch(Exception e){respond(reply,id,null,"Could not check releases. Check your connection and retry.");}});break;
   case "installUpdate":
    if(!getPackageManager().canRequestPackageInstalls()){startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));respond(reply,id,null,"Allow FREQ to install updates, return, then tap Install update again.");break;}
    worker.execute(()->{try{File apk=updates.downloadVerified();handler.post(()->{try{Uri content=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".updates",apk);Intent install=new Intent(Intent.ACTION_VIEW).setDataAndType(content,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(install);respond(reply,id,true,null);}catch(Exception e){respond(reply,id,null,"Android could not open the installer.");}});}catch(Exception e){respond(reply,id,null,e.getMessage()==null?"Update could not be verified.":e.getMessage());}});break;
   case "apps": worker.execute(()->{try{JSONArray result=appList(true);respond(reply,id,result,null);}catch(Exception e){respond(reply,id,null,"Could not load applications.");}});break;
   case "ready": ready=true;if(watchdog!=null)handler.removeCallbacks(watchdog);respond(reply,id,true,null);routeHome();break;
   case "device": respond(reply,id,device(),null);break;
   case "launch": launch(m.getString("component"));respond(reply,id,true,null);break;
   case "defaultHome": requestHome();respond(reply,id,true,null);break;
   case "homeSettings": startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));respond(reply,id,true,null);break;
   case "systemSettings": startActivity(new Intent(Settings.ACTION_SETTINGS));respond(reply,id,true,null);break;
   case "appSettings": startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));respond(reply,id,true,null);break;
   case "openSite": LiveWorkspaceActivity.openBrowser(this);respond(reply,id,true,null);break;
   case "liveWorkspace": openLive(m.optString("view","Home"));respond(reply,id,true,null);break;
   case "setLiveHome": getPreferences(0).edit().putBoolean("liveHome",m.optBoolean("enabled",false)).apply();respond(reply,id,true,null);break;
   case "pickFolder":
    if(folderReply!=null){respond(reply,id,null,"A folder picker is already open.");break;}
    folderReply=reply;folderRequest=id;
    startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),PICK_FOLDER);break;
   case "folder":
    String uri=getPreferences(0).getString("folder",null);
    if(uri==null){respond(reply,id,null,"Select a folder first.");break;}
    readFolderAsync(Uri.parse(uri),reply,id);break;
   default: respond(reply,id,null,"Unsupported action.");
  }}catch(Exception e){if("pickFolder".equals(action)){folderReply=null;folderRequest=null;}respond(reply,id,null,"Action unavailable: "+e.getClass().getSimpleName());}
 }
 private void respond(androidx.webkit.JavaScriptReplyProxy reply,String id,Object value,String error){handler.post(()->{try{JSONObject r=new JSONObject();r.put("id",id);if(error!=null)r.put("error",error);else r.put("result",value==null?JSONObject.NULL:value);reply.postMessage(r.toString());}catch(Exception ignored){}});}
 private JSONArray appList(boolean icons)throws Exception{
  Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
  List<ResolveInfo> list=getPackageManager().queryIntentActivities(launcher,0);
  list.sort((a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
  JSONArray out=new JSONArray();Set<String> seen=new HashSet<>();
  for(ResolveInfo r:list){String pkg=r.activityInfo.packageName;if(pkg.equals(getPackageName()))continue;
   String id=new ComponentName(pkg,r.activityInfo.name).flattenToString();if(!seen.add(id))continue;
   JSONObject a=new JSONObject();a.put("id",id);a.put("name",r.loadLabel(getPackageManager()).toString());
   if(icons){try{Drawable icon=r.loadIcon(getPackageManager());Bitmap bm=Bitmap.createBitmap(72,72,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bm);icon.setBounds(0,0,72,72);icon.draw(c);ByteArrayOutputStream bytes=new ByteArrayOutputStream();bm.compress(Bitmap.CompressFormat.PNG,100,bytes);a.put("icon",Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP));bm.recycle();}catch(Exception ignored){a.put("icon","");}}
   out.put(a);
  }return out;
 }
 private void launch(String id)throws Exception{
  ComponentName c=ComponentName.unflattenFromString(id);if(c==null)throw new IllegalArgumentException();
  List<ResolveInfo> allowed=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
  boolean ok=false;for(ResolveInfo r:allowed)if(r.activityInfo.packageName.equals(c.getPackageName())&&r.activityInfo.name.equals(c.getClassName())){ok=true;break;}
  if(!ok)throw new SecurityException();
  startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
 }
 private JSONObject device()throws Exception{
  Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));int pct=-1;boolean charging=false;
  if(battery!=null){int scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE,100);pct=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,0)*100/Math.max(1,scale);int status=battery.getIntExtra(BatteryManager.EXTRA_STATUS,-1);charging=status==BatteryManager.BATTERY_STATUS_CHARGING||status==BatteryManager.BATTERY_STATUS_FULL;}
  ResolveInfo home=getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY);
  JSONObject d=new JSONObject();d.put("model",Build.MODEL);d.put("android",Build.VERSION.RELEASE);d.put("battery",pct);d.put("charging",charging);d.put("version",getPackageManager().getPackageInfo(getPackageName(),0).versionName);d.put("liveHome",getPreferences(0).getBoolean("liveHome",false));d.put("defaultHome",home!=null&&home.activityInfo.packageName.equals(getPackageName()));return d;
 }
 private void requestHome(){if(Build.VERSION.SDK_INT>=29){RoleManager rm=getSystemService(RoleManager.class);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_HOME)&&!rm.isRoleHeld(RoleManager.ROLE_HOME)){startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),20);return;}}startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}
 private void readFolderAsync(Uri uri,androidx.webkit.JavaScriptReplyProxy reply,String id){worker.execute(()->{try{respond(reply,id,listFolder(uri),null);}catch(Exception e){respond(reply,id,null,"Folder permission unavailable. Select the folder again.");}});}
 private JSONObject listFolder(Uri tree)throws Exception{
  String docId=DocumentsContract.getTreeDocumentId(tree);Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,docId);String name="Selected folder";
  try(Cursor c=getContentResolver().query(parent,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0);}
  Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,docId);JSONArray files=new JSONArray();
  try(Cursor c=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){while(c!=null&&c.moveToNext()&&files.length()<500){JSONObject f=new JSONObject();f.put("name",c.getString(0));f.put("directory",DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(1)));files.put(f);}}
  return new JSONObject().put("name",name).put("files",files);
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==PICK_FOLDER&&folderReply!=null){var reply=folderReply;String id=folderRequest;folderReply=null;folderRequest=null;if(result!=RESULT_OK||data==null||data.getData()==null){respond(reply,id,null,"Folder selection cancelled.");return;}try{Uri tree=data.getData();if((data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)==0)throw new SecurityException("Read permission missing");getContentResolver().takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION);getPreferences(0).edit().putString("folder",tree.toString()).apply();readFolderAsync(tree,reply,id);}catch(Exception e){respond(reply,id,null,"Could not remember folder permission.");}}}
 private void openLive(String view){startActivity(new Intent(this,LiveWorkspaceActivity.class).putExtra("view",view));}
 private void routeHome(){
  if(!ready||web==null)return;
  String route=getIntent().getStringExtra("route");getIntent().removeExtra("route");
  if("apps".equals(route)){web.evaluateJavascript("window.freqOpenApps&&window.freqOpenApps()",null);return;}
  web.evaluateJavascript("window.freqHome&&window.freqHome()",null);
  if(!"local".equals(route)&&getPreferences(0).getBoolean("liveHome",false))openLive("Home");
 }
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);routeHome();}
 @Override protected void onResume(){super.onResume();if(web!=null&&ready)web.evaluateJavascript("window.freqResume&&window.freqResume()",null);}
 @Override public void onBackPressed(){if(web!=null&&ready)web.evaluateJavascript("window.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape'}))",null);else showRecovery();}
 @Override public boolean onKeyDown(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_VOLUME_UP&&event.isLongPress()){showRecovery();return true;}return super.onKeyDown(key,event);}
 private void showRecovery(){new AlertDialog.Builder(this).setTitle("FREQ recovery").setMessage("Open an app, change your Home launcher, or reload FREQ.").setPositiveButton("Home settings",(d,w)->startActivity(new Intent(Settings.ACTION_HOME_SETTINGS))).setNeutralButton("Apps",(d,w)->nativeDrawer()).setNegativeButton("Reload",(d,w)->{ready=false;buildWeb();}).show();}
 private void nativeDrawer(){worker.execute(()->{try{JSONArray a=appList(false);String[] names=new String[a.length()];for(int i=0;i<a.length();i++)names[i]=a.getJSONObject(i).getString("name");handler.post(()->new AlertDialog.Builder(this).setTitle("Applications").setItems(names,(d,which)->{try{launch(a.getJSONObject(which).getString("id"));}catch(Exception e){Toast.makeText(this,"App unavailable",Toast.LENGTH_SHORT).show();}}).setNegativeButton("Home settings",(d,w)->startActivity(new Intent(Settings.ACTION_HOME_SETTINGS))).show());}catch(Exception ignored){}});}
 @Override protected void onDestroy(){if(watchdog!=null)handler.removeCallbacks(watchdog);worker.shutdownNow();if(web!=null)web.destroy();super.onDestroy();}
}
