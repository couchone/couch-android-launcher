package com.couchone.couchdb;

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
import android.util.Log;

import com.google.ase.Exec;

public class CouchProcess {

	public static String TAG = "CouchDB";

	public CouchService service;

	public String adminUser = "admin";
	public String adminPass;

	// TODO: read from config file
	public final String host = "127.0.0.1";
	public final int port = 5984;

	public boolean started = false;
	
	// We provide CouchProcess as a Singleton so the FutonActivity can access it 
	// directly
	private static volatile CouchProcess INSTANCE = new CouchProcess();

	public static synchronized CouchProcess getInstance() {
        return INSTANCE;	
    }
	    
	private Integer pid;
	private PrintStream out;
	private BufferedReader in;
	
	public void start(String binary, String arg1, String arg2) {

		int[] pidbuffer = new int[1];
		final FileDescriptor fd = Exec.createSubprocess(binary, arg1, arg2, pidbuffer);
		pid = pidbuffer[0];
		out = new PrintStream(new FileOutputStream(fd), true);
		in = new BufferedReader(new InputStreamReader(new FileInputStream(fd)));

		new Thread(new Runnable() {
			public void run() {
				try {				
					while (fd.valid()) {					
						String line = in.readLine();
						Log.v(TAG, line);
						if (line.contains("has started on")) {
							started = true;
							ensureAdmin();
							service.couchStarted();
						}
					}
				} catch (IOException e) {
					// Closed io from seperate thread
				} catch (Exception e) {
					Log.v(TAG, "CouchDB has stopped unexpectedly");
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	private void ensureAdmin() throws JSONException {
		adminPass = readOrGeneratePass(adminUser);
		// TODO: only works because I cant overwrite, check if exists in future
		String url = url() + "_config/admins/" + adminUser;
		HTTPRequest.put(url, "\"" + adminPass + "\"");
	};
	
	public String readOrGeneratePass(String username) {
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

	public String generatePassword(int length) {
		String charset = "!0123456789abcdefghijklmnopqrstuvwxyz";
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
	
	public void stop() {
		try {
			out.close();
			android.os.Process.killProcess(pid);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	public String url() {
		return "http://" + host + ":" + Integer.toString(port) + "/";
	}

}
