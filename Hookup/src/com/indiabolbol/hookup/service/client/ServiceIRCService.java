package com.indiabolbol.hookup.service.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCConstants;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.indiabolbol.hookup.GFindsterPreferences;
import com.indiabolbol.hookup.GFindsterUser;

public class ServiceIRCService extends Service implements IRCEventListener {

	private static String host = "irc.freenode.net";

	private static String nick = "bolbol";
	private static String user = "1.0.10@indiabolbol.com";

	private static String name = "indiabolbol";
	private static String LOCATIONDISABLED_CHANNEL = "#gfindster";
	public static String chan = LOCATIONDISABLED_CHANNEL;
	private static int port = 6667;
	private static String pass;

	private static final String PROVIDER_NAME = LocationManager.GPS_PROVIDER;

	/** The current default target of PRIVMSGs (a channel or nickname). */
	private String target;

	public static Context ircServiceContext;
	// private static Thread locationupdateThread;

	public static IRCConnection ircConn;

	public static IRCEventListener ircListener;

	public static int state = 0;

	public static final int MSG_UPDATECHAN = 0;
	public static final int MSG_CLEARPROGRESS = 1;
	public static final int MSG_CHANGECHAN = 2;
	public static final int MSG_DISCONNECT = 3;
	public static final int MSG_CHANGEWINDOW = 4;
	public static final int MSG_RECVD = 5;
	public static final int MSG_NICKINUSE = 6;
	public static final int MSG_CONNECT = 7;
	public static final int MSG_PVTCHAT = 8;
	public static final int MSG_SVCDOWN = 9;
	public static final int MSG_CHANCHAT = 10;
	public static final int MSG_JOIN = 11;
	public static final int MSG_PART =12;
	

	public static final String AC_VERSION = "0.1";

	private static List<String> currentUsers = Collections
			.synchronizedList(new ArrayList());
	// private static List<String> currentUserLines = Collections
	// .synchronizedList(new ArrayList());

	private static List<GFindsterUser> currentGFUsers = Collections
			.synchronizedList(new ArrayList());
	private static List<String> currentWhoisLines = Collections
			.synchronizedList(new ArrayList());

	// private static Hashtable<String, String> users = new Hashtable<String,
	// String>();
	public static Handler channelViewHandler;

	public static String lastwindow = "~status";
	public static String curwindow = "~status";

	public static String LOG_TAG = "ServiceIRCService";

	public static Boolean shownChanListConnect = false;
	public static double latitude;
	public static double longitude;
	protected LocationManager myLocationManager = null;

	private static boolean listingWhoisCompleted = false;

	private static Location curLoc;

	private static SharedPreferences preferences;
	private final LocationListener onLocationChange = new LocationListener() {
		public void onLocationChanged(Location loc) {
			if (loc != null) {
				curLoc = loc;
				Log.d(LOG_TAG, "LocationListener GPS  onLocationChanged ");
			}
		}

		public void onProviderDisabled(String provider) {
			Log.d(LOG_TAG, "LocationListener GPS Provider disabled for "
					+ provider);
		}

		public void onProviderEnabled(String provider) {
			// required for interface, not used
			Log.d(LOG_TAG, "LocationListener GPS Provider enabled for "
					+ provider);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// required for interface, not used
			Log.d(LOG_TAG, "LocationListener GPS Provider status changed for "
					+ provider + " status: " + status);
		}
	};

	public static void setViewHandler(Handler what) {
		channelViewHandler = what;
		Message
				.obtain(ServiceIRCService.channelViewHandler,
						ServiceIRCService.MSG_CHANGEWINDOW,
						ServiceIRCService.curwindow).sendToTarget();

	}

	public static void joinChan() {
		if (ircConn != null && ircConn.isConnected()) {
			ircConn.doJoin(chan);
		} else {
			sendMessage("Not connected to server", MSG_DISCONNECT);
		}
	}

	public static void ignoreUser(String nick) {
		if (ircConn != null && ircConn.isConnected()) {
			// ircConn.do
			ircConn.send("/IGNORE " + nick);
		} else {
			sendMessage("Not connected to server", MSG_DISCONNECT);
		}
	}

	public IBinder onBind(Intent intent) {
		if (ircConn == null || !ircConn.isConnected()) {
			Log.i(LOG_TAG, "ircConnection is NULL or NOT CONNECTED state::"
					+ state);
			// if (state < 1) {
			// connectToServer();
			// Log.i(LOG_TAG, "calling connectToServer onStart ");
			//
			// } else {
			// Log.i(LOG_TAG,
			// "onBind NOT calling connectToServer onStart state is "
			// + state);
			// }
		}
		return mBinder;
	}

	// send a location to the server.
	public static void updateLocation(double lat, double lng) {
		longitude = lng;
		latitude = lat;
		name = "LOC " + latitude + " " + longitude;
		Log.i(LOG_TAG, "calling connectToServer onREBIND connection ");

	}

	@Override
	public void onRebind(Intent intent) {
		// TODO Auto-generated method stub
		super.onRebind(intent);
		// if (ircConn == null || !ircConn.isConnected()) {
		// Log.i(LOG_TAG, "ircConnection is NULL or NOT CONNECTED");
		// if (state < 1) {
		// connectToServer();
		// Log.i(LOG_TAG, "calling connectToServer onREBIND connection ");
		//
		// } else {
		// Log.i(LOG_TAG,
		// "onRebind NOT calling connectToServer onStart state is "
		// + state);
		// }
		// }
	}

	@Override
	public void onLowMemory() {
		// TODO Auto-generated method stub
		super.onLowMemory();
		sendMessage("Stopping Service !!! Your battery is low", MSG_RECVD);
		stopSelf();
	}

	public static void listUsers() {
		listingWhoisCompleted = false;
		currentUsers.clear();
		// currentUserLines.clear();
		currentGFUsers.clear();
		currentWhoisLines.clear();
		// Log.d(LOG_TAG, "In LIST USERS OF CHAN ::new ArrayList()" + chan);
		if (ircConn != null && ircConn.isConnected()) {
			// Log.d(LOG_TAG, "In LIST USERS OF CHAN OUT OF while (true)" +
			// chan);

			Log.d(LOG_TAG, "In listUsers WHOIS OUT OF while (true)" + chan);

			// query users in channel
			ircConn.doWho(chan);
			int seconds = 20;
			String seconds1 = preferences.getString("lagtime", "20");
			Log.d(LOG_TAG, "listUsers>>>>>>>>>>>>>>>>>Lag time is " + seconds1);
			try {
				seconds = Integer.parseInt(seconds1);
			} catch (Exception e) {
				// TODO: handle exception
				Log.e(LOG_TAG, "listUsers ERrro in CHAT NAME " + e);
			}
			Log.d(LOG_TAG,
					"listUsers>>>>>>>>>>>>>>>>>AFTER Parsing Lag time is "
							+ seconds);
			long totalWaitingWho = 1000 * seconds;
			Log.d(LOG_TAG, "listUsers>>>>>>>>>>>>>>>>>Total waiting time "
					+ totalWaitingWho);
			long waitWho = 500;
			long waitedforWho = 0;
			while (true && waitedforWho < totalWaitingWho) {

				Log.d(LOG_TAG,
						"listUsersIn LIST currentWhoisLines OF CHAN while (true)"
								+ chan);
				if (!listingWhoisCompleted) {
					try {
						Log.d(LOG_TAG, "In listUsers sleeping for " + waitWho
								+ " milli seconds");
						Thread.sleep(waitWho);
						waitedforWho = waitedforWho + waitWho;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						// e.printStackTrace();
						Log.d(LOG_TAG, "listUsersInterupted while waiting ");
					}
				} else {
					for (int i = 0; i < currentWhoisLines.size(); i++) {
						parseWho(currentWhoisLines.get(i));
					}

					break;
				}
			}
			Log.d(LOG_TAG, "listUsers>>>>>>>>>>>>>>>>>Total waited time >>"
					+ waitedforWho / 1000 + "seconds");

		}
	}

	private static void parseWho(String whoString) {
		// Log.d(LOG_TAG, "parseWho::location code::>>" + whoString);
		// StringTokenizer st = new StringTokenizer(whoString);
		String[] splitWho = whoString.split(" ");
		// for (String who : splitWho) {
		// Log.d(LOG_TAG, "---------PARSE WHO---------" + who);
		// }
		GFindsterUser gf = new GFindsterUser();
		gf.setNick(splitWho[5]);
		Log.d(LOG_TAG, "__PARSE WHO LOC "+splitWho[8]);
		if ("LOC".equalsIgnoreCase(splitWho[8])) {
			gf.setLatitude(Double.parseDouble(splitWho[9]));
			gf.setLongitude(Double.parseDouble(splitWho[10]));
		}
		currentGFUsers.add(gf);
		Log.d(LOG_TAG, "__PARSE WHO NICK getNick()"+gf.getNick());
		Log.d(LOG_TAG, "__PARSE WHO getLatitude()"+gf.getLatitude());
		Log.d(LOG_TAG, "__PARSE WHO getLongitude()"+gf.getLongitude());
		// Log.d(LOG_TAG, "--------------------PARSE WHO END
		// ------------------");

	}

	private static void parseUsers(String userline) {
		if (userline != null) {
			// Log.d(LOG_TAG, "USERS RAW::" + userline);
			StringTokenizer st = new StringTokenizer(userline);
			while (st.hasMoreTokens()) {
				String user = st.nextToken();
				// Log.d(LOG_TAG, "USER::>>>" + user);
				if (user.startsWith("@", 0)) {
					Log.d(LOG_TAG, "USER:starts with @:>>>" + user);
					user = user.replace("@", "");
				}

				currentUsers.add(user);

			}
			Log.d(LOG_TAG, "Number of users::" + currentUsers.size());

		}

	}

	private static void parseChanUsers(String chanuserwho) {
		if (chanuserwho != null) {
			// Log.d(LOG_TAG, "USERS RAW::" + userline);
			StringTokenizer st = new StringTokenizer(chanuserwho);
			while (st.hasMoreTokens()) {
				String user = st.nextToken();
				// Log.d(LOG_TAG, "USER::>>>" + user);
				if (user.startsWith("@", 0)) {
					Log.d(LOG_TAG, "USER:starts with @:>>>" + user);
					user = user.replace("@", "");
				}
				GFindsterUser gfuser = new GFindsterUser();
				gfuser.setNick(user);
				currentGFUsers.add(gfuser);

			}
			Log.d(LOG_TAG, "Number of users::" + currentUsers.size());

		}

	}

	public static void quitServer() {
		if (ircConn != null && ircConn.isConnected()) {
			ircConn.doQuit("http://www.indiabolbol.com/");
			Log.i(LOG_TAG, "Quiting Server");
		}
	}

	public synchronized static List<String> getCurrentUsers() {
		return currentUsers;

	}

	public synchronized static List<GFindsterUser> getCurrentGFUsers() {
		return currentGFUsers;

	}

	public static void sendToChan(String what) {
		if (ircConn != null && ircConn.isConnected()) {
			ircConn.doPrivmsg(chan, what);
		} else {
			sendMessage("Not connected to server!!", MSG_DISCONNECT);
		}

	}

	public static void clearCurrentUsers1() {
		currentUsers.clear();
	}

	public static void sendToUser(String nick, String what) {
		if (ircConn != null && ircConn.isConnected()) {
			ircConn.doPrivmsg(nick, what);
		} else {
			sendMessage("Not connected to server!!", MSG_DISCONNECT);
		}
	}

	@Override
	public void onCreate() {
		Log.i(LOG_TAG, "IRCService on CREATE-------");
		// mNM = (NotificationManager)
		// this.getSystemService(NOTIFICATION_SERVICE);
		Log.i(LOG_TAG, "IRCService on CREATE---11111111111----");
		// This is who should be launched if the user selects our persistent
		// notification.
		ircServiceContext = this;
		// Intent intent = new Intent();
		// intent.setClass(this, HookupMapView.class);

		// channels = new HashMap<String, ClassChannelContainer>();
		// channel_list = new HashMap<String, ClassChannelDescriptor>();
		// temp_user_locs = new HashMap<String, Location>();

		preferences = ircServiceContext.getSharedPreferences(
				GFindsterPreferences.PREFS_NAME, 0);

		Log.d(LOG_TAG, "bEFORE Nick from preferences is " + nick);
		Log.d(LOG_TAG, "BEFORE Host from preferences is " + host);
		host = preferences.getString("serverText", host);
		nick = preferences.getString("defNick", nick);

		Log.d(LOG_TAG, "Nick from preferences is " + nick);
		Log.d(LOG_TAG, "Host from preferences is " + host);

		myLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		curLoc = myLocationManager.getLastKnownLocation(PROVIDER_NAME);
		Log.i(LOG_TAG, "Started ON CREATE........22222222222222222222....");
		initLocation();




	}

	private void initLocation() {
		Log.i(LOG_TAG, "Initializing location initLocation()");
		// mLocationListener = new HookupLocationListener();
		// myLocationManager.requestLocationUpdates(PROVIDER_NAME, 0, 0,
		// mLocationListener);
		try {
			Log.i(LOG_TAG, "Initializing location requestLocationUpdates()");
			myLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 1000L * 60 * 60 * 5 /*
																		 * 60 5
																		 * sec
																		 * between
																		 * update
																		 */,
					500.0f/* receive update if greater than 500m change */,
					onLocationChange);
			Log.i(LOG_TAG, "Initializing location requestLocationUpdates()");
			// myLocationManager.requestLocationUpdates(
			// LocationManager.NETWORK_PROVIDER, 1000L * 60 * 60 * 5 /*
			// * 60 5
			// * sec
			// * between
			// * update
			// */,
			// 500.0f/* receive update if greater than 500m change */,
			// onLocationChange);
		} catch (Exception e) {
			Log.e(LOG_TAG, "Error in getting location updates " + e);
		}

		latitude = curLoc.getLatitude();
		longitude = curLoc.getLongitude();

		// lon = 17.429843;
		// lat = 78.418865;

		name = "LOC " + latitude + " " + longitude;

		Log.d(LOG_TAG, "???????????Channel is " + chan);
		Log.d(LOG_TAG, "???????Name/Location is " + name);

		if (preferences.getBoolean("enableLocChat", true)) {
			Log.d(LOG_TAG, "Getting address from geocoder::chan is:" + chan);
			Geocoder gc = new Geocoder(ServiceIRCService.this);
			List<Address> addresses = null;
			Address curadress = null;
			try {
				addresses = gc.getFromLocation(latitude, longitude, 1);
				if (addresses != null && addresses.size() > 0
						&& addresses.get(0) != null) {
					curadress = addresses.get(0);

					String temp2 = curadress.getAdminArea();
					if (temp2 != null) {
						String[] temp3 = temp2.split(" ");
						String temp4 = "";
						for (int i = 0; i < temp3.length; i++) {

							temp4 = temp4 + temp3[i];
						}
						if (temp4.length() > 15) {
							temp4 = temp4.substring(0, 15);
						}
						chan = "#" + temp4;
					}else{
						chan = "#" + Math.round(latitude) + "_"
						+ Math.round(longitude);
				Log.d(LOG_TAG,
						"Address is NULL from GEOCODE, so setting to LOCATION LAT+LON::"
								+ chan);
					}

				} else {
					chan = "#" + Math.round(latitude) + "_"
							+ Math.round(longitude);
					Log.d(LOG_TAG,
							"Address is NULL from GEOCODE, so setting to LOCATION LAT+LON::"
									+ chan);
				}
			} catch (IOException e) {
				Log.e(LOG_TAG, "Error getting location" + e);
				chan = "#" + Math.round(latitude) + "_" + Math.round(longitude);
				Log.d(LOG_TAG,
						"Error from GEOCODE, so setting to LOCATION LAT+LON::"
								+ chan);
			}

			Log.d(LOG_TAG, "Channel is " + chan);

		} else {
			chan = LOCATIONDISABLED_CHANNEL;
			Log.d(LOG_TAG, "Disabled location chat, so Channel is " + chan);
		}

	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		Log
				.i(LOG_TAG,
						"IRCService on onStart---4444444444444444444444444444444444444----");
		if (getSharedPreferences("androidChatPrefs", 0).getBoolean("sendLoc",
				true)) {
			Log
					.i(LOG_TAG,
							"IRCService on onStart---55555555555555555555555555555555555----");
			// locationupdateThread = new Thread(new ThreadUpdateLocThread(
			// ircServiceContext));
			Log
					.i(LOG_TAG,
							"IRCService on onStart---66666666666666666666666666666666666----");
			// locationupdateThread.start();

		}
		Log.d(LOG_TAG, "Before ON Start Nick from preferences is " + nick);
		Log.d(LOG_TAG, "Before ON Start Host from preferences is " + host);
		host = preferences.getString("serverText", host);
		nick = preferences.getString("defNick", nick);
		Log.d(LOG_TAG, "ON Start Nick from preferences is " + nick);
		Log.d(LOG_TAG, "ON Start Host from preferences is " + host);
		if (ircConn != null && ircConn.isConnected() && state == 10) {
			sendMessage("Network connected", MSG_CONNECT);
		} else {
			sendMessage("Network is disconnected!!", MSG_DISCONNECT);
		}

		initLocation();
		// curLoc = myLocationManager.getLastKnownLocation(PROVIDER_NAME);
		// latitude = curLoc.getLatitude();
		// longitude = curLoc.getLongitude();
		// name = "LOC " + latitude + " " + longitude;
		//
		// if (ircConn == null || !ircConn.isConnected()) {
		// Log.i(LOG_TAG, "ircConnection is NULL or NOT CONNECTED state::"
		// + state);
		// if (state < 1) {
		// connectToServer();
		// Log.i(LOG_TAG, "calling connectToServer onStart ");
		//
		// } else {
		// Log
		// .i(LOG_TAG, "calling connectToServer onStart state "
		// + state);
		// }
		// }

	}

	public void stopService() {
		Log.d(LOG_TAG, "<<<<<<< Stopiing service ServiceIRC >>>>");
		stopSelf();
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		Log.i(LOG_TAG, "IRCService on onDestroy---1111111111----");
		quitServer();
		Log.i(LOG_TAG, "IRCService on onDestroy---222222222222222----");
		state = 0;
		if (channelViewHandler != null) {
			Log.i(LOG_TAG, "IRCService on onDestroy---3333333333333333333----");
			// channels.get("~status").addLine("*** Disconnected");
			sendMessage("Service is going down", MSG_SVCDOWN);

		}

	}

	public IBinder getBinder() {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		// state = 0;
		return super.onUnbind(intent);
	}

	public class IRCChatServiceBinder extends Binder {
		public ServiceIRCService getService() {
			Log.i(LOG_TAG, "getService ");
			return ServiceIRCService.this;
		}
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new IRCChatServiceBinder();

	//public static NotificationManager mNM;

	public void onRegistered() {
		print("Connected");
		state = 10;
		sendMessage("Connected to server..." + host, MSG_CONNECT);
	}

	public void onDisconnected() {
		print("Disconnected");
		state = 0;
		sendMessage("Disonnected from server..." + host, MSG_DISCONNECT);

	}

	public void onError(String msg) {
		print("Error: " + msg);
		// sendMessage("Error occurred " + host + " Erro:" + msg);
	}

	public void onError(int num, String msg) {
		print("Error #" + num + ": " + msg);
		if (num == 433) {
			sendMessage("Your NICK(" + nick + ") is already in use",
					MSG_NICKINUSE);
		}
	}

	public void onInvite(String chan, IRCUser u, String nickPass) {
		print(chan + "> " + u.getNick() + " invites " + nickPass);
		// sendMessage(chan + "> " + u.getNick() + " invites " + nickPass);
	}

	public void onJoin(String chan, IRCUser u) {
		print(chan + "> " + u.getNick() + " joins");
		if(preferences.getBoolean("enableLocChat", false)){
			sendMessage(u.getNick() + " is nearby..", MSG_JOIN);
		}else{
			sendMessage(u.getNick() + " is in chat..", MSG_JOIN);
		}
		
	}

	public void onKick(String chan, IRCUser u, String nickPass, String msg) {
		print(chan + "> " + u.getNick() + " kicks " + nickPass);
		// sendMessage(chan + "> " + u.getNick() + " kicks " + nickPass);
	}

	public void onMode(IRCUser u, String nickPass, String mode) {
		print("Mode: " + u.getNick() + " sets modes " + mode + " " + nickPass);
		// sendMessage("Mode: " + u.getNick() + " sets modes " + mode + " "+
		// nickPass);
	}

	public void onMode(String chan, IRCUser u, IRCModeParser mp) {
		print(chan + "> " + u.getNick() + " sets mode: " + mp.getLine());
		// sendMessage(chan + "> " + u.getNick() + " sets mode: " +
		// mp.getLine());
	}

	public void onNick(IRCUser u, String nickNew) {
		print("Nick: " + u.getNick() + " is now known as " + nickNew);
		// sendMessage("Nick: " + u.getNick() + " is now known as " + nickNew);
	}

	public void onNotice(String target, IRCUser u, String msg) {
		print(target + "> " + u.getNick() + " (notice): " + msg);
		// sendMessage(target + "> " + u.getNick() + " (notice): " + msg);
	}

	public void onPart(String chan, IRCUser u, String msg) {
		print(chan + "> " + u.getNick() + " parts");
		// sendMessage(chan + "> " + u.getNick() + " parts");
		if(preferences.getBoolean("enableLocChat", false)){
			sendMessage(chan + "> " + u.getNick() + " is moving..",MSG_PART);
		}else{
			sendMessage(chan + "> " + u.getNick() + " is leaving..",MSG_PART);
		}
	}

	public void onPrivmsg(String chan, IRCUser u, String msg) {
		print(chan + "> " + u.getNick() + ": " + msg);
		print(chan + "|" + u.getNick() + "|" + msg);
		String msgstr = chan + "|" + u.getNick() + "|" + msg;
		String[] nickMsg = parseNickMsg(msgstr);
		if (nickMsg != null && nickMsg.length > 1) {
			if (nickMsg[0].startsWith("#")) {
				sendMessage(msgstr, MSG_CHANCHAT);
				Log.d(LOG_TAG, "Channel chat ! " + msgstr);
			} else {
				Log.d(LOG_TAG, "Private chat ! " + msgstr);
				sendMessage(msgstr, MSG_PVTCHAT);
			}
		} else {
			Log.d(LOG_TAG, "Invalid pvt msg");
		}

	}

	public void onQuit(IRCUser u, String msg) {
		print("Quit: " + u.getNick());
		sendMessage("Quit: " + u.getNick(), MSG_RECVD);
	}

	public void onReply(int num, String value, String msg) {
		// print("Reply #" + num + ": " + value + " " + msg);
		// if (num == IRCConstants.RPL_NAMREPLY) {
		// // Log.d(LOG_TAG, "---REPLY FOR LIST USERS IN CHANNEL" + msg);
		// // parseUsers(msg);
		// //currentUserLines.add(msg);
		// }
		// if (num == RPL_ENDOFNAMES) {
		// Log.d(LOG_TAG, "END REPLY FOR LIST USERS IN CHANNEL----");
		// }
		// this is for who
		if (num == IRCConstants.RPL_WHOREPLY) {
			listingWhoisCompleted = false;
			currentWhoisLines.add(value + " " + msg);

		}
		if (num == IRCConstants.RPL_ENDOFWHO) {
			// end of whois
			// Log.d(LOG_TAG, "END REPLY FOR WHOIS---");
			listingWhoisCompleted = true;
		}

	}

	public void onTopic(String chan, IRCUser u, String topic) {
		print(chan + "> " + u.getNick() + " changes topic into: " + topic);
		// sendMessage(chan + "> " + u.getNick() + " changes topic into: " +
		// topic);
	}

	public void onPing(String p) {

	}

	public void unknown(String a, String b, String c, String d) {
		print("UNKNOWN: " + a + " b " + c + " " + d);
		sendMessage("UNKNOWN: " + a + " b " + c + " " + d, MSG_RECVD);
	}

	private static void print(Object o) {
		Log.i(LOG_TAG, o.toString());
	}

	private static void sendMessage(Object o, int which) {
		if (channelViewHandler != null) {
			Message.obtain(channelViewHandler, which, o).sendToTarget();
		}
	}

	public void connect() {
		if (ircConn == null || !ircConn.isConnected()) {
			Log.i(LOG_TAG, "ircConnection is NULL or NOT CONNECTED state::"
					+ state);
			if (state < 1) {
				connectToServer();
				Log.i(LOG_TAG, "calling connectToServer connect " + state);

			} else {
				Log.i(LOG_TAG, "calling connectToServer connect state" + state);
			}
		}

	}

	private synchronized void connectToServer() {
		Thread connThread = new Thread() {
			public void run() {
				// if (ircConn != null && ircConn.isConnected()
				// && ServiceIRCService.state == 10) {
				// Log.i(LOG_TAG,
				// "ircConnection is NOT NULL and IS CONNECTED--");
				// return;
				// }
				host = preferences.getString("serverText", host);
				nick = preferences.getString("defNick", nick);

				ServiceIRCService.state = 1; // logging in
				initLocation();
				Log.i(LOG_TAG,
						"INSIDE connectToServer THREAD ircConnection is NULL or IS NOT CONNECTED::"
								+ Thread.currentThread().getName());
				ircConn = new IRCConnection(host, new int[] { port }, pass,
						nick, user, name);
				ircConn.addIRCEventListener(ServiceIRCService.this);
				ircConn.setEncoding("UTF-8");
				ircConn.setPong(true);
				ircConn.setColors(false);
				try {
					ServiceIRCService.state = 2; // logging in
					Log.i(LOG_TAG,
							"ThreadConnThread going to connect---t000000----"
									+ nick);
					ircConn.connect();
					// ircConn.doNames(chan);
					ServiceIRCService.state = 10; // 
					joinChan();

					Log.i(LOG_TAG, "ThreadConnThread CONNECTED---t1111----"
							+ nick);

					// // while (true) {
					//
					//
					// if (ircConn != null && ircConn.isConnected()) {
					//
					// Log.i(LOG_TAG,
					// "ThreadConnThread JOIN CHANNEL command
					// sent---t33333333----"
					// + nick);
					// sendMessage("Connected to " + host, MSG_CONNECT);
					//
					// } else {
					// sendMessage(
					// "Unable to connect to service over network..please try
					// later",
					// MSG_DISCONNECT);
					// }
					// //
					// // else {
					// // Thread.sleep(1000);
					// // }
					// // }

				} catch (IOException e) {
					// TODO Auto-generated catch block
					state = 0;
					ircConn = null;
					Log.e(LOG_TAG, "Server Connection IOException" + e);
					sendMessage("Network Unavailable", MSG_DISCONNECT);
					return;
				} catch (Exception e) {
					state = 0;
					ircConn = null;
					Log.e(LOG_TAG, "Server Connection Exception" + e);
					sendMessage("ERROR Occured !! " + e.getMessage(),
							MSG_DISCONNECT);
					return;
				}

			}
		};
		connThread.start();
	}

	private class HookupLocationListener implements LocationListener {
		public void onLocationChanged(Location loc) {
			if (loc == null) {
				Log.e(LOG_TAG, "location changed to null");
			} else {
				Log.d(LOG_TAG, "location changed : " + loc.toString());
			}
		}

		public void onStatusChanged(java.lang.String arg0, int arg1,
				Bundle extras) {
			Log.d(LOG_TAG, "Provider Status Changed : " + arg0);
		}

		public void onProviderEnabled(java.lang.String arg0) {
			Log.d(LOG_TAG, "Provider Enabled : " + arg0);
		}

		public void onProviderDisabled(java.lang.String arg0) {
			Log.d(LOG_TAG, "Provider Disabled : " + arg0);
		}
	}

	private String[] parseNickMsg(String msgstr) {
		Log.d(LOG_TAG, "PARSE NICK msgstr " + msgstr);
		StringTokenizer st = new StringTokenizer(msgstr, "|");
		String[] msg = { "", "", "" };
		int count = 0;
		while (st.hasMoreTokens() && count < 3) {
			msg[count] = st.nextToken();
			Log.d(LOG_TAG, "PARSE NICK " + msg[count]);
			count++;
		}
		return msg;
	}
}