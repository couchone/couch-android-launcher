package org.couchdb.android;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CouchDB extends Activity {

	public final static String TAG = "CouchDB";
	public final static String FUTON = "http://127.0.0.1:5984/_utils/";

	private ProgressDialog loading;

	private String adminUser;
	private String adminPass;

	private ICouchService couchService;
	public Boolean serviceStarted = false;

	private static final int COUCH_STARTED = 1;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			try {
				serviceStarted = true;
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

	private void setFutonView() {
		WebView webView = new WebView(CouchDB.this);
		webView.setWebChromeClient(new WebChromeClient());
		webView.setWebViewClient(new CustomWebViewClient());
		webView.setHttpAuthUsernamePassword("127.0.0.1", "administrator",
				adminUser, adminPass);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		setContentView(webView);
		webView.loadUrl(FUTON);
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
			handler.proceed(up[0], up[1]);
		}
	}

	private void showLoading() {
		loading = ProgressDialog.show(this, "", "CouchDB is Loading...", true);
	}

	private Boolean deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDirectory(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

	private void deleteDatabases() {
		unbindService(mConnection);
		CouchService.stopCouchDB();
		Log.v(TAG, "DELETING EVERYTHING");
		File couchDir = new File(Environment.getExternalStorageDirectory(),
				"couch");
		// ARG THIS IS TOTALLY SCARY AND WRONG
		deleteDirectory(couchDir);
		finish();
	}

	private void confirmDelete() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Are you sure you want to delete, this will delete all of your existing data?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								CouchDB.this.deleteDatabases();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.stop:
			unbindService(mConnection);
			finish();
			return true;
		case R.id.delete:
			confirmDelete();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (couchService != null) {
			unbindService(mConnection);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boot();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		boot();
	};

	private void boot() {
		boolean installed = CouchInstaller.checkInstalled();
		if (!installed && !serviceStarted) {
			startActivity(new Intent(this, CouchInstallActivity.class));
		} else if (!serviceStarted) {
			showLoading();
			bindService(new Intent(ICouchService.class.getName()), mConnection,
					Context.BIND_AUTO_CREATE);
		}
	};

	private ICouchClient mCallback = new ICouchClient.Stub() {
		@Override
		public void couchStarted(String host, int port) throws RemoteException {
			couchService.adminCredentials(mCallback);
		}

		@Override
		public void databaseCreated(String name, String user, String pass,
				String tag) throws RemoteException {
		}

		@Override
		public void adminCredentials(String user, String pass)
				throws RemoteException {
			adminUser = user;
			adminPass = pass;
			mHandler.sendMessage(mHandler.obtainMessage(COUCH_STARTED));
		}
	};

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case COUCH_STARTED:
				loading.dismiss();
				setFutonView();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};

}