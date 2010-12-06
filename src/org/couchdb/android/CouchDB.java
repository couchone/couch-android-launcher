package org.couchdb.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CouchDB extends Activity {
	
	public final static String TAG = "CouchDB";
	public final static String FUTON = "http://127.0.0.1:5984/_utils/";
    
    ICouchService mService = null;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {        	
        	mService = ICouchService.Stub.asInterface(service);
            try {
            	mService.startCouchDB(mCallback);
            } catch (RemoteException e) { }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private void setFutonView() { 
    	WebView webView = new WebView(CouchDB.this);
    	webView.setWebChromeClient(new WebChromeClient());
    	webView.setWebViewClient(new WebViewClient());
    	webView.getSettings().setJavaScriptEnabled(true); 
    	webView.getSettings().setBuiltInZoomControls(true);
    	setContentView(webView);
    	webView.loadUrl(FUTON);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.stop:
	    	unbindService(mConnection);
	    	finish();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);

		boolean installed = CouchInstaller.checkInstalled();
        if (!installed) { 
        	startActivity(new Intent(this, CouchInstallActivity.class));
        } else { 
        	bindService(new Intent(ICouchService.class.getName()), mConnection, Context.BIND_AUTO_CREATE);
        }
	}
	
    private ICouchClient mCallback = new ICouchClient.Stub() {
    	@Override
        public void couchStarted() {
            mHandler.sendMessage(mHandler.obtainMessage(COUCH_STARTED));
        }
    };

    private static final int COUCH_STARTED = 1;

    private Handler mHandler = new Handler() {
        @Override 
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case COUCH_STARTED:
                	setFutonView();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };
	
}