package org.couchdb.android;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.google.ase.Exec;

public class CouchService extends Service {
	final String TAG = "CouchDB";
	final int NO_NOTIFY = 1;
	private NotificationManager mNM;
	CouchProcess couch;
	
	public class CouchProcess {

		  public Integer pid;
		  FileDescriptor fd;
		  PrintStream out;
		  BufferedReader in;
		  boolean started = false;
		  boolean notify;
		  
		  public void start(String binary, String arg1, String arg2, boolean donotify) {
			notify = donotify;
		    int[] pidbuffer = new int[1];
		    fd = Exec.createSubprocess(binary, arg1, arg2, pidbuffer);
		    pid = pidbuffer[0];
		    out = new PrintStream(new FileOutputStream(fd), true);
		    in = new BufferedReader(new InputStreamReader(new FileInputStream(fd)));

		    new Thread(new Runnable() {
		      public void run() {
		        Log.v(TAG, "PID: " + pid);
		        while(fd.valid())
		        {
		        	String line;
					try {
						line = in.readLine();
					} catch (IOException e) {
						break; //TODO: Notify of couch death
					}
		        	Log.v(TAG, line);
		        	if(line.contains("has started on"))
		        	{
		        		Log.v(TAG, "Couch has started.");
		        		int icon = R.drawable.icon;
		                CharSequence tickerText = "CouchDB Running";
		                long when = System.currentTimeMillis();
		                if(notify)
		                {
		                	Notification notification = new Notification(icon, tickerText, when);
		                	notification.flags = Notification.FLAG_ONGOING_EVENT;
		                	Intent i = new Intent(Intent.ACTION_VIEW);
		                	i.setData(Uri.parse("http://127.0.0.1:5984/_utils"));
		                	notification.setLatestEventInfo(getApplicationContext(), "CouchDB Running", "Press to open Futon", PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_CANCEL_CURRENT));
		                	mNM.cancel(1);
		                	mNM.notify(2, notification);
		                	startForeground(2, notification);
		                }
		                started = true;
		        	}
		        }
		      }
		    }).start();
		    
		  }
		}

	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	if(couch != null) return 0;
    	boolean donotify = (flags != 1);
    	mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        couch = new CouchProcess();
        int icon = R.drawable.icon;
        CharSequence tickerText = "CouchDB Starting";
        long when = System.currentTimeMillis();
        if(donotify) {
        	Notification notification = new Notification(icon, tickerText, when);
        	Intent notificationIntent = new Intent(this, CouchDB.class);
        	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        	notification.setLatestEventInfo(getApplicationContext(), "CouchDB Starting", "Please Wait...", contentIntent);
        	mNM.notify(1, notification);
        }
        couch.start("/system/bin/sh", "/sdcard/couch/bin/couchdb", "", donotify);
        return START_STICKY;
    }
    
    
    @Override
    public void onDestroy() {
    	try {
    		couch.out.close();
			android.os.Process.killProcess(couch.pid);
			couch.in.close();
		} catch (IOException e) {
			//Failed to kill couch? 
		}
    	mNM.cancelAll();
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		this.onStartCommand(null, NO_NOTIFY, 0);
		return mBinder;
	}

	private final ICouchService.Stub mBinder = new ICouchService.Stub() 
	{
		public int getPort()
		{
			while(couch == null || !couch.started) Thread.yield(); //Horrible blocking whee!
			return 5984;
		}
	};

}
