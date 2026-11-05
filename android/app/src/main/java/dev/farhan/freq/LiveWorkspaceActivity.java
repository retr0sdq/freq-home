package dev.farhan.freq;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

/** Live site has a separate WebView with NO native JavaScript bridge. */
public final class LiveWorkspaceActivity extends Activity {
 static final String SITE="https://farhan-home-base.farhaansadeeq.chatgpt.site";
 private WebView web;
 private TextView status;
 private ProgressBar progress;
 private String script,pending="Home";
 private boolean failed;
 private int attempts;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable prepare=new Runnable(){public void run(){
  if(web==null||isFinishing()||!isSite(web.getUrl())||failed)return;
  if(++attempts>75){status.setText("SIGN IN / SITE NOT READY · RELOAD IN MENU");return;}
  web.evaluateJavascript(script+";window.freqSiteNavigate("+JSONObject.quote(pending)+")",result->{
   if("\"ready\"".equals(result)){pending="";status.setText("HOME BASE · LIVE");}
   else if("\"blocked\"".equals(result))status.setText("SAVE OR CLOSE OPEN DRAFTS FIRST");
   else handler.postDelayed(prepare,400);
  });
 }};
 @Override public void onCreate(Bundle state){
  super.onCreate(state);pending=getIntent().getStringExtra("view");if(pending==null)pending="Home";
  try(var in=getAssets().open("launcher-live.js");var out=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);script=new String(out.toByteArray(),StandardCharsets.UTF_8);}catch(Exception e){finish();return;}
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(20,20,25));setContentView(root);
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);root.addView(bar);
  button(bar,"‹ LOCAL",()->local("local"));
  status=new TextView(this);status.setText("HOME BASE · SIGN IN IF ASKED");status.setTextColor(0xffc5c5ce);status.setTextSize(10);status.setGravity(Gravity.CENTER);bar.addView(status,new LinearLayout.LayoutParams(0,dp(46),1));
  button(bar,"⋮",this::menu);
  LinearLayout shortcuts=new LinearLayout(this);root.addView(shortcuts);
  button(shortcuts,"GRAPH",()->navigate("graph"));button(shortcuts,"AREA TREE",()->navigate("area"));button(shortcuts,"SETTINGS",()->navigate("Settings"));
  progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(2)));
  web=new WebView(this);web.setBackgroundColor(0xff202023);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
  WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setSupportMultipleWindows(false);settings.setSafeBrowsingEnabled(true);
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
    if(!request.isForMainFrame())return false;
    Uri uri=request.getUrl();if(isInternal(uri))return false;
    external(uri);return true;
   }
   @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon){handler.removeCallbacks(prepare);attempts=0;failed=false;status.setText(isSite(url)?"HOME BASE · LOADING":"CHATGPT · SIGN IN");}
   @Override public void onPageFinished(WebView view,String url){CookieManager.getInstance().flush();if(isSite(url)&&!failed){handler.removeCallbacks(prepare);handler.post(prepare);}}
   @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())loadFailed();}
   @Override public void onReceivedHttpError(WebView view,WebResourceRequest request,WebResourceResponse response){if(request.isForMainFrame()&&response.getStatusCode()>=400)loadFailed();}
   @Override public boolean onRenderProcessGone(WebView view,RenderProcessGoneDetail detail){web=null;view.destroy();status.setText("LIVE VIEW STOPPED · USE LOCAL OR REOPEN");return true;}
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public void onProgressChanged(WebView view,int value){progress.setProgress(value);progress.setVisibility(value==100?View.GONE:View.VISIBLE);}
   @Override public boolean onJsConfirm(WebView view,String url,String message,JsResult result){new AlertDialog.Builder(LiveWorkspaceActivity.this).setMessage(message).setPositiveButton("Confirm",(d,w)->result.confirm()).setNegativeButton("Cancel",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;}
   @Override public void onPermissionRequest(PermissionRequest request){request.deny();}
  });
  LinearLayout dock=new LinearLayout(this);dock.setPadding(dp(2),dp(5),dp(2),dp(5));dock.setBackgroundColor(0xff111114);root.addView(dock);
  String[] labels={"⌂\nGRID","▤\nOPS","♫\nNOISE","▦\nAPPS","✎\nMIND","⌘\nNET","▱\nFILES"};String[] targets={"Home","Work","Studio","apps","Personal","Cyberdeck","Files"};
  for(int i=0;i<labels.length;i++){final String target=targets[i];Button b=button(dock,labels[i],()->{if("apps".equals(target))local("apps");else navigate(target);});b.setTextSize(12);b.setBackgroundColor(Color.TRANSPARENT);b.setPadding(0,0,0,0);b.setMinWidth(0);b.setMinimumWidth(0);b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(56),1));if(i==3){b.setTextColor(0xff111114);b.setBackgroundColor(0xffdfff00);b.setContentDescription("Open installed app drawer");}}
  web.loadUrl(SITE);
 }
 private Button button(LinearLayout parent,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setTextSize(10);b.setTextColor(0xffdfff00);b.setMinHeight(dp(44));b.setMinimumHeight(dp(44));parent.addView(b);b.setOnClickListener(v->action.run());return b;}
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 static boolean isSite(String url){if(url==null)return false;Uri u=Uri.parse(url);return "https".equals(u.getScheme())&&"farhan-home-base.farhaansadeeq.chatgpt.site".equals(u.getHost())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443);}
 private static boolean isInternal(Uri u){if(isSite(u.toString()))return true;if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return false;String h=u.getHost();return "chatgpt.com".equals(h)||"auth.openai.com".equals(h)||"auth0.openai.com".equals(h);}
 private void external(Uri uri){if(!"https".equals(uri.getScheme())&&!"http".equals(uri.getScheme())){Toast.makeText(this,"Open this link in your browser instead.",Toast.LENGTH_SHORT).show();return;}try{startActivity(new Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE));}catch(ActivityNotFoundException e){Toast.makeText(this,"No browser available",Toast.LENGTH_SHORT).show();}}
 static void openBrowser(Activity activity){try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(SITE)).addCategory(Intent.CATEGORY_BROWSABLE));}catch(ActivityNotFoundException e){Toast.makeText(activity,"No browser available",Toast.LENGTH_SHORT).show();}}
 private void navigate(String view){pending=view;attempts=0;if(web!=null&&isSite(web.getUrl())&&!failed){handler.removeCallbacks(prepare);handler.post(prepare);}else Toast.makeText(this,"Sign in first, then choose a workspace.",Toast.LENGTH_SHORT).show();}
 private void loadFailed(){failed=true;handler.removeCallbacks(prepare);status.setText("OFFLINE / LOAD FAILED · RETRY IN MENU");}
 private void menu(){new AlertDialog.Builder(this).setTitle("Live Home Base").setItems(new String[]{"Reload latest site","Open in browser / sign-in fallback","Offline launcher","Installed apps"},(d,which)->{if(which==0&&web!=null){failed=false;web.reload();}if(which==1)openBrowser(this);if(which==2)local("local");if(which==3)local("apps");}).show();}
 private void local(String route){startActivity(new Intent(this,HomeActivity.class).putExtra("route",route).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();}
 @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else local("local");}
 @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(web!=null){web.stopLoading();web.destroy();web=null;}super.onDestroy();}
}
