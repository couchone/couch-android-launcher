package org.couchdb.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.json.JSONException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.ase.Exec;

public class CouchService extends Service {

	// For the generated passwords
	private static final String charset = "!0123456789abcdefghijklmnopqrstuvwxyz";

	private final String adminUser = "admin";
	private String adminPass;

	// TODO: read from config file
	private final String couchHost = "127.0.0.1";
	private final int couchPort = 5984;

	private NotificationManager mNM;
	private Boolean couchStarted = false;

	private static CouchProcess couch;

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

			if (couchStarted) {
				couchStarted();
			}
		}

		@Override
		public void initDatabase(ICouchClient callback, String tag,
				boolean cmdDb) throws RemoteException {

			String packageName = packageNameFromUid(Binder.getCallingUid());
			String userName = packageName.replace(".", "_");
			String dbName = tag + "-" + userName;
			String pass = readOrGeneratePass(userName);

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
		public void adminCredentials(ICouchClient callback)
				throws RemoteException {
			callback.adminCredentials(adminUser, adminPass);
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
			CouchCtrlListener temp = new CouchCtrlListener(couchUrl(), dbName,
					adminUser, adminPass);
			tmp.databases.put(dbName, temp);
			listeners.put(packageName, tmp);
			return temp;
		}
	}

	private void createUser(String user, String pass) {
		try {

			String url = couchUrl() + "/_users";
			String salt = generatePassword(10);
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

			String url = couchUrl() + dbName;
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
		for (Entry<String, ICouchClient> entry : couchClients.entrySet()) {
			ICouchClient client = entry.getValue();
			client.couchStarted(couchHost, couchPort);
			couchClients.remove(entry.getKey());
		}
	}

	private String packageNameFromUid(int uid) {
		PackageManager pm = getPackageManager();
		String[] packages = pm.getPackagesForUid(Binder.getCallingUid());
		return packages[0];
	};

	private void ensureAdmin() throws JSONException {
		adminPass = readOrGeneratePass(adminUser);
		Log.v(CouchDB.TAG, "admin passsword is " + adminPass);
		// TODO: only works because I cant overwrite, check if exists in future
		String url = couchUrl() + "_config/admins/" + adminUser;
		HTTPRequest.httpRequest("PUT", url, "\"" + adminPass + "\"",
				new String[][] {});
	};

	private String readFile(String filePath) {
		String contents = "";
		try {
			File file = new File(filePath);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			contents = reader.readLine();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contents;
	};

	private void writeFile(String filePath, String data) {
		try {
			FileWriter writer = new FileWriter(filePath);
			writer.write(data);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	};

	private String readOrGeneratePass(String username) {
		File couchSDCard = Environment.getExternalStorageDirectory();
		String passwordFile = couchSDCard.getPath() + "/couch/" + username
				+ ".passwd";
		File f = new File(passwordFile);
		if (!f.exists()) {
			String pass = generatePassword(8);
			writeFile(passwordFile, username + ":" + pass);
			return pass;
		} else {
			return readFile(passwordFile).split(":")[1];
		}
	}

	private String generatePassword(int length) {
		Random rand = new Random(System.currentTimeMillis());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int pos = rand.nextInt(charset.length());
			sb.append(charset.charAt(pos));
		}
		return sb.toString();
	}

	public class CouchProcess {

		public Integer pid;
		public PrintStream out;
		public BufferedReader in;

		boolean notify;

		public void start(String binary, String arg1, String arg2,
				boolean donotify) {

			notify = donotify;
			int[] pidbuffer = new int[1];
			final FileDescriptor fd = Exec.createSubprocess(binary, arg1, arg2,
					pidbuffer);
			pid = pidbuffer[0];
			out = new PrintStream(new FileOutputStream(fd), true);
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					fd)));

			new Thread(new Runnable() {
				public void run() {
					Log.v(CouchDB.TAG, "PID: " + pid);
					while (fd.valid()) {
						String line;
						try {
							line = in.readLine();
						} catch (IOException e) {
							break;
						}
						Log.v(CouchDB.TAG, line);
						if (line.contains("has started on")) {
							couchStarted = true;
							try {
								ensureAdmin();
							} catch (JSONException e1) {
								e1.printStackTrace();
							}
							try {
								couchStarted();
							} catch (RemoteException e) {
								e.printStackTrace();
							}
							Log.v(CouchDB.TAG, "Couch has started.");
							int icon = R.drawable.icon;
							CharSequence tickerText = "CouchDB Running";
							long when = System.currentTimeMillis();
							if (notify) {
								Notification notification = new Notification(
										icon, tickerText, when);
								notification.flags = Notification.FLAG_ONGOING_EVENT;
								Intent i = new Intent(CouchService.this,
										CouchDB.class);
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
						}
					}
				}
			}).start();

		}
	}

	public static void stopCouchDB() {
		try {
			couch.out.close();
			android.os.Process.killProcess(couch.pid);
			couch.in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		couch = new CouchProcess();
		int icon = R.drawable.icon;
		CharSequence tickerText = "CouchDB Starting";
		long when = System.currentTimeMillis();
		if (true) {
			Notification notification = new Notification(icon, tickerText, when);
			Intent notificationIntent = new Intent(this, CouchDB.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					notificationIntent, 0);
			notification.setLatestEventInfo(getApplicationContext(),
					"CouchDB Starting", "Please Wait...", contentIntent);
			mNM.notify(1, notification);
		}
		couch.start("/system/bin/sh", "/sdcard/couch/bin/couchdb", "", true);
	}

	@Override
	public void onDestroy() {
		stopCouchDB();
		mNM.cancelAll();
	}

	@Override
	public IBinder onBind(Intent intent) {
		IBinder client = new CouchServiceImpl();
		return client;
	}

	private String couchUrl() {
		return "http://" + couchHost + ":" + Integer.toString(couchPort) + "/";
	}

	private String[][] adminHeaders() {
		String auth = Base64Coder.encodeString(adminUser + ":" + adminPass);
		String[][] headers = { { "Authorization", "Basic " + auth } };
		return headers;
	}
}
