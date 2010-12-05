package org.couchdb.android;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CouchDB extends Activity {
	public final static String TAG = "CouchDB";
    Button startButton;
    Button stopButton;
    
	@Override
	public void onResume()
	{
		super.onResume();
		if(startButton == null) return;
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

		List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);

		if (!(serviceList.size() > 0)) {
			return;
		}

		for (int i = 0; i < serviceList.size(); i++) {
			RunningServiceInfo serviceInfo = serviceList.get(i);
			ComponentName serviceName = serviceInfo.service;

			if (serviceName.getClassName().equals("org.couchdb.android.CouchService")) {
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
				return;
			}
		}
		startButton.setEnabled(true);
		stopButton.setEnabled(false);
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
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        boolean installed = CouchInstaller.checkInstalled();
        if(!installed)
        	startActivity(new Intent(this, CouchInstallActivity.class));
        startButton = (Button) findViewById(R.id.StartButton);
        stopButton = (Button) findViewById(R.id.StopButton);
        startButton.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				startService(new Intent("org.couchdb.android.COUCHDB_SERVICE"));
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
			}
        	
        });
        
        stopButton.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				stopService(new Intent("org.couchdb.android.COUCHDB_SERVICE"));
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
			}   	
        });
	}
}