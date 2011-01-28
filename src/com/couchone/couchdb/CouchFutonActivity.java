package com.couchone.couchdb;

import com.couchone.libcouch.ICouchClient;
import com.couchone.libcouch.ICouchService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CouchFutonActivity extends Activity {

	private ProgressDialog loading;
	private ICouchService couchService;
	private WebView webView;
	
	private String couchHost;
	private int couchPort;
	
	private boolean couchStarted = false;
	
	private static final int COUCH_STARTED = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		attemptLaunch();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		attemptLaunch();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (webView != null) {
			webView.destroy();
		}
		if (couchService != null) {
			unbindService(mConnection);
		}
	}
	
	/*
	 * Checks to see if Couch is fully installed, if not prompt to complete
	 * installation otherwise start the couchdb service
	 */
	private void attemptLaunch() {
		if (!CouchInstaller.checkInstalled()) {
			startActivity(new Intent(this, CouchInstallActivity.class));
		} else if (!couchStarted) {
			String msg = this.getString(R.string.loading_dialog);
			loading = ProgressDialog.show(this, "", msg, true);
			bindService(new Intent(ICouchService.class.getName()), mConnection, Context.BIND_AUTO_CREATE);
		}
	}
	
	/* 
	 * This holds the connection to the CouchDB Service
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			try {
				couchService = ICouchService.Stub.asInterface(service);
				couchService.initCouchDB(mCallback);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			couchService = null;
		}
	};
	
	/*
	 * Implement the callbacks that allow CouchDB to talk to this app
	 */
	private ICouchClient mCallback = new ICouchClient.Stub() {
		@Override
		public void couchStarted(String host, int port) throws RemoteException {
			couchHost = host;
			couchPort = port;
			couchStarted = true;
			mHandler.sendMessage(mHandler.obtainMessage(COUCH_STARTED));
		}

		@Override
		public void databaseCreated(String name, String user, String pass, String tag) 
			throws RemoteException {}
	};

	/*
	 * Because the service communication happens in a seperate thread, we need
	 * a message handler to control the ui in this thread
	 */
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case COUCH_STARTED:
				loading.dismiss();
				launchFuton();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};

	/*
	 * Creates the menu items
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	};

	/*
	 * Handles the menu item callbacks
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.stop:
			unbindService(mConnection);
			finish();
			return true;
		case R.id.adminpass:
			showPassword();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/*
	 * 
	 */
	private void showPassword() { 
		String pass = CouchProcess.readOrGeneratePass("admin");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(Html.fromHtml("Password for user <b>admin</b> is <b>" + pass + "</b>"))
		       .setCancelable(false)
		       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		
		alert.show();
	}
	
	private void launchFuton() {
		String pass = CouchProcess.readOrGeneratePass("admin");
		webView = new WebView(CouchFutonActivity.this);
		webView.setWebChromeClient(new WebChromeClient());
		webView.setWebViewClient(new CustomWebViewClient());
		webView.setHttpAuthUsernamePassword(couchHost, "administrator", "admin", pass);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		setContentView(webView);
		String url = "http://" + couchHost + ":" + Integer.toString(couchPort) + "/";
		webView.loadUrl(url + "_utils/");
	};

	private class CustomWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {
			String[] up = view.getHttpAuthUsernamePassword(host, realm);
			if( up != null && up.length == 2 ) { 
				handler.proceed(up[0], up[1]);
			}
		}
	}
}