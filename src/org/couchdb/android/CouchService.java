package org.couchdb.android;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

public class CouchService extends Service {

	private NotificationManager mNM;

	private CouchProcess couchProcess = CouchProcess.getInstance();
	
	// A list of couchClients that awaiting notifications of couch starting
	private Map<String, ICouchClient> couchClients = new HashMap<String, ICouchClient>();

	// Contains a mapping of database names to their listeners
	public class dbListeners {
		private Map<String, CouchCtrlListener> databases = new HashMap<String, CouchCtrlListener>();
	}

	// Contains a mapping of package names to map of database names + listeners
	private Map<String, dbListeners> listeners = new HashMap<String, dbListeners>();

	public class CouchServiceImpl extends ICouchService.Stub {

		@Override
		public void initCouchDB(ICouchClient cb) throws RemoteException {

			String packageName = packageNameFromUid(Binder.getCallingUid());
			couchClients.put(packageName, cb);

			if (couchProcess.couchStarted) {
				couchStarted();
			}
		}

		@Override
		public void initDatabase(ICouchClient callback, String tag,
				boolean cmdDb) throws RemoteException {

			String packageName = packageNameFromUid(Binder.getCallingUid());
			String userName = packageName.replace(".", "_");
			String dbName = tag + "-" + userName;
			String pass = couchProcess.readOrGeneratePass(userName);

			createIfNotExists(dbName, userName, pass);

			if (cmdDb) {

				createIfNotExists(dbName + "-ctrl", userName, pass);

				dbListeners tmp = listeners.containsKey(packageName) ? listeners
						.get(packageName) : new dbListeners();

				final CouchCtrlListener listener = getOrCreateListener(tmp,
						packageName, dbName);

				new Thread(new Runnable() {
					public void run() {
						listener.start();
					}
				}).start();
			}

			callback.databaseCreated(dbName, userName, pass, tag);
		}

		@Override
		public void quitCouchDB() throws RemoteException {
			String packageName = packageNameFromUid(Binder.getCallingUid());
			if (listeners.containsKey(packageName)) {
				dbListeners tmp = listeners.get(packageName);
				for (Map.Entry<String, CouchCtrlListener> temp : tmp.databases
						.entrySet()) {
					temp.getValue().cancel();
				}
			}
		}
	};

	private CouchCtrlListener getOrCreateListener(dbListeners tmp,
			String packageName, String dbName) {
		if (tmp.databases.containsKey(dbName)) {
			return tmp.databases.get(dbName);
		} else {
			CouchCtrlListener temp = new CouchCtrlListener(couchProcess.couchUrl(), dbName,
					couchProcess.adminUser, couchProcess.adminPass);
			tmp.databases.put(dbName, temp);
			listeners.put(packageName, tmp);
			return temp;
		}
	}

	private void createUser(String user, String pass) {
		try {

			String url = couchProcess.couchUrl() + "/_users";
			String salt = couchProcess.generatePassword(10);
			String hashed = AeSimpleSHA1.SHA1(pass + salt);
			String json = "{\"_id\":\"org.couchdb.user:" + user + "\","
					+ "\"type\":\"user\"," + "\"name\":\"" + user + "\","
					+ "\"roles\":[]," + "\"password_sha\":\"" + hashed + "\", "
					+ "\"salt\":\"" + salt + "\"}";

			HTTPRequest.httpRequest("POST", url, json, adminHeaders());

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	};

	private void createIfNotExists(String dbName, String user, String pass) {
		try {

			String url = couchProcess.couchUrl() + dbName;
			HTTPRequest res = HTTPRequest.httpRequest("GET", url, null,
					adminHeaders());

			if (res.status == 404) {
				createUser(user, pass);
				HTTPRequest.httpRequest("PUT", url, null, adminHeaders());
				String sec = "{\"admins\":{\"names\":[\""
						+ user
						+ "\"],\"roles\":[]},\"readers\":{\"names\":[],\"roles\":[]}}";
				HTTPRequest.httpRequest("PUT", url + "/_security", sec,
						adminHeaders());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	};

	void couchStarted() throws RemoteException {
		
		int icon = R.drawable.icon;
		CharSequence tickerText = "CouchDB Running";
		long when = System.currentTimeMillis();
		if (true) { //(notify) {
			Notification notification = new Notification(icon, tickerText, when);
			notification.flags = Notification.FLAG_ONGOING_EVENT;
			Intent i = new Intent(CouchService.this,
					CouchFutonActivity.class);
			notification.setLatestEventInfo(
					getApplicationContext(),
					"CouchDB Running",
					"Press to open Futon", PendingIntent
							.getActivity(CouchService.this,
									0, i, 0));
			mNM.cancel(1);
			mNM.notify(2, notification);
			startForeground(2, notification);
		}
		
		for (Entry<String, ICouchClient> entry : couchClients.entrySet()) {
			ICouchClient client = entry.getValue();
			client.couchStarted(couchProcess.host, couchProcess.port);
			couchClients.remove(entry.getKey());
		}
	}

	private String packageNameFromUid(int uid) {
		PackageManager pm = getPackageManager();
		String[] packages = pm.getPackagesForUid(Binder.getCallingUid());
		return packages[0];
	};

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		int icon = R.drawable.icon;
		CharSequence tickerText = "CouchDB Starting";
		long when = System.currentTimeMillis();
		if (true) {
			Notification notification = new Notification(icon, tickerText, when);
			Intent notificationIntent = new Intent(this, CouchFutonActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					notificationIntent, 0);
			notification.setLatestEventInfo(getApplicationContext(),
					"CouchDB Starting", "Please Wait...", contentIntent);
			mNM.notify(1, notification);
		}
		couchProcess.registerService(this);
		couchProcess.start("/system/bin/sh", "/sdcard/couch/bin/couchdb", "", true);
	}

	@Override
	public void onDestroy() {
		couchProcess.stopCouchDB();
		mNM.cancelAll();
	}

	@Override
	public IBinder onBind(Intent intent) {
		IBinder client = new CouchServiceImpl();
		return client;
	}

	private String[][] adminHeaders() {
		String auth = Base64Coder.encodeString(couchProcess.adminUser + ":" + couchProcess.adminPass);
		String[][] headers = { { "Authorization", "Basic " + auth } };
		return headers;
	}
}
