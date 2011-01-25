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
import java.util.Random;

import org.json.JSONException;

import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;

import com.google.ase.Exec;

public class CouchProcess {

	private static volatile CouchProcess INSTANCE = null;
	
    public static synchronized CouchProcess getInstance() {
        if (INSTANCE == null) {
        	INSTANCE = new CouchProcess();
        }
        return INSTANCE;	
    }

	// For the generated passwords
	private static final String charset = "!0123456789abcdefghijklmnopqrstuvwxyz";

	public final String adminUser = "admin";
	public String adminPass;

	// TODO: read from config file
	final String couchHost = "127.0.0.1";
	final int couchPort = 5984;

    
	public Integer pid;
	public PrintStream out;
	public BufferedReader in;
	
	public CouchService service;
	
	public Boolean couchStarted;

	boolean notify;

	public void registerService(CouchService service) {
		this.service = service;
	}
	
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
				Log.v(CouchFutonActivity.TAG, "PID: " + pid);
				while (fd.valid()) {
					String line;
					try {
						line = in.readLine();
					} catch (IOException e) {
						break;
					}
					Log.v(CouchFutonActivity.TAG, line);
					if (line.contains("has started on")) {
						couchStarted = true;
						try {
							ensureAdmin();
						} catch (JSONException e1) {
							e1.printStackTrace();
						}
						try {
							service.couchStarted();
						} catch (RemoteException e) {
							e.printStackTrace();
						}
						Log.v(CouchFutonActivity.TAG, "Couch has started.");
					}
				}
			}
		}).start();
	}
	
	private void ensureAdmin() throws JSONException {
		adminPass = readOrGeneratePass(adminUser);
		Log.v(CouchFutonActivity.TAG, "admin passsword is " + adminPass);
		// TODO: only works because I cant overwrite, check if exists in future
		String url = couchUrl() + "_config/admins/" + adminUser;
		HTTPRequest.httpRequest("PUT", url, "\"" + adminPass + "\"",
				new String[][] {});
	};
	
	String readOrGeneratePass(String username) {
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

	String generatePassword(int length) {
		Random rand = new Random(System.currentTimeMillis());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int pos = rand.nextInt(charset.length());
			sb.append(charset.charAt(pos));
		}
		return sb.toString();
	}

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
	}
	
	public void stopCouchDB() {
		try {
			out.close();
			android.os.Process.killProcess(pid);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	

	String couchUrl() {
		return "http://" + couchHost + ":" + Integer.toString(couchPort) + "/";
	}

}
