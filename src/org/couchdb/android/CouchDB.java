package org.couchdb.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;

public class CouchDB extends Activity {
	
	public final static String TAG = "CouchDB";
	public final static String FUTON = "http://127.0.0.1:5984/_utils/";
    
    Messenger mService = null;
    
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CouchService.MSG_COUCH_STARTED:
                	Log.v(TAG, "Received COUCH_STARTED Message");
                	// TODO: WebView screws up layout, launching browser
                	// makes Resume etc annoying
                	WebView webview = new WebView(CouchDB.this);
                	setContentView(webview);
                	webview.loadUrl(FUTON);
                	//launchFuton();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	mService = new Messenger(service);
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null, CouchService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) { }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
        }
    };

    private void launchFuton() { 
    	Uri uri = Uri.parse(FUTON);
    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    	startActivity(intent);    
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	//launchFuton();
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
	    	stopService(new Intent("org.couchdb.android.COUCHDB_SERVICE"));
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
        	bindService(new Intent("org.couchdb.android.COUCHDB_SERVICE"), mConnection, Context.BIND_AUTO_CREATE);
        }
	}
}