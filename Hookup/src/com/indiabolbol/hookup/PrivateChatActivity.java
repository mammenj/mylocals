package com.indiabolbol.hookup;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.indiabolbol.hookup.service.client.ServiceIRCService;

/**
 * Demonstrates the using a list view in transcript mode
 * 
 */
public class PrivateChatActivity extends Activity {

	private EditText mUserText;
	private TextView tv;

	private ScrollView sv;

	private String LOG_TAG = "PrivateChatActivity";

	StringBuilder temp;

	String target;
	String tempNick;

	private static final int PVT_CHAT = 1;

	// gtalk requires this or your messages bounce back as errors
	// ConnectionConfiguration connConfig = null;
	// XMPPConnection connection = null;
	//
	// ChatManager chatmanager = null;
	// Chat newChat = null;

	// private ListView mList;

	private ServiceIRCService mBoundService;
	private boolean mIsBound = false;

	public Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case ServiceIRCService.MSG_PVTCHAT:
				Log.i(LOG_TAG, "MSG UPDATE CHANGER " + msg.obj.toString());
				String[] temp = parseNickMsg(msg.obj.toString());
				if (!temp[1].equalsIgnoreCase(target)) {
					Log.d(LOG_TAG, "Current chat nick is " + target);
					tempNick = temp[1];
					Log.d(LOG_TAG, ".........New chat nick is " + tempNick);
					showDialog(PVT_CHAT);
				} else {
					updateView(msg.obj.toString());
				}
				break;

			case ServiceIRCService.MSG_NICKINUSE:
				Log.i(LOG_TAG, "MSG UPDATE CHANGER " + msg.obj.toString());

				showToast("CHAT NAME in USE, please change");
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setClass(getBaseContext(),
						com.indiabolbol.hookup.GFindsterPreferences.class);
				startActivity(intent);
				break;

			case ServiceIRCService.MSG_DISCONNECT:
				Log.i(LOG_TAG, "MSG_DISCONNECT " + msg.obj.toString());
				showToast(msg.obj.toString());
				Intent intentMain = new Intent(Intent.ACTION_MAIN);
				intentMain.setClass(getBaseContext(),
						com.indiabolbol.hookup.GFindster.class);
				startActivity(intentMain);
				break;

			}
		}
	};

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case PVT_CHAT:
			dialog.setTitle("PM with " + tempNick);
			break;

		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Log.i(LOG_TAG, "ON onCreateDialog............");
		if (id == PVT_CHAT) {
			String[] items = { "Accept", "Ignore", "No" };
			return new AlertDialog.Builder(PrivateChatActivity.this).setTitle(
					"Private Chat").setItems(items,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							/* User clicked so do some stuff */
							// String[] items = getResources().getStringArray(
							// R.array.private_chat_items);
							if (ServiceIRCService.ircConn != null
									&& ServiceIRCService.ircConn.isConnected()) {

								if (which == 0) {
									Intent intent = new Intent(
											Intent.ACTION_MAIN);
									intent
											.putExtra(
													"com.indiabolbol.hookup.showSelectedUser",
													tempNick);

									intent.setClass(getBaseContext(),
											PrivateChatActivity.class);
									startActivity(intent);
								} else if (which == 1) {
									showToast("Ignoring user " + tempNick);
								} else if (which == 2) {
									showToast("No Private Chat with "
											+ tempNick);
								}

							} else {
								showToast("Network Unvailabel!! Please connect again..");
							}
						}
					}).create();

		} else
			return null;
	}

	protected void showToast(String msgtoShow) {
		// create the view
		View view = inflateView(R.layout.incoming_message_panel);

		// set the text in the view
		TextView tv = (TextView) view.findViewById(R.id.message);
		tv.setText(msgtoShow);

		// show the toast
		Toast toast = new Toast(this);
		toast.setView(view);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.show();
	}

	private View inflateView(int resource) {
		LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return vi.inflate(resource, null);
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {

			mBoundService = ((ServiceIRCService.IRCChatServiceBinder) service)
					.getService();

			Log.i(LOG_TAG, "onService connected......method");
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			Log.i(LOG_TAG, "onService connected......method");
			mBoundService = null;
			// Toast.makeText(ChatActivity.this, "Disconnected to service",
			// Toast.LENGTH_SHORT).show();
		}
	};

	// private void setListAdapter() {
	// Log.i(LOG_TAG, "setListAdapter");
	// mAdapter = new ArrayAdapter<String>(this,
	// android.R.layout.simple_list_item_1, mStrings);
	// mList.setAdapter(mAdapter);
	// }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "onCreate binding servic");
		bindService(new Intent(PrivateChatActivity.this,
				ServiceIRCService.class), mConnection, Context.BIND_AUTO_CREATE);
		ServiceIRCService.setViewHandler(mHandler);
		mIsBound = true;
		if (mBoundService != null) {
			Log.i(LOG_TAG, "mBoundService is NOT NULL");
		}

		setContentView(R.layout.chat_view);

		// mList = (ListView) getListView();
		// setListAdapter();
		target = getIntent().getStringExtra(
				"com.indiabolbol.hookup.showSelectedUser");

		setTitle("Chat >> " + target);
		// setTitleColor(Color.BLUE);

		Log.i(LOG_TAG, "TARGET CHATT IS " + target);

		mUserText = (EditText) findViewById(R.id.userText);

		mUserText.setOnKeyListener(mKeyListener);

		sv = (ScrollView) findViewById(R.id.chatscroll);
		tv = (TextView) findViewById(R.id.ircdisp);
		sv.fullScroll(ScrollView.FOCUS_DOWN);

	}

	@Override
	protected void onStart() {
		super.onStart();
		temp = new StringBuilder();
		Log.i(LOG_TAG, "onStart");
		ServiceIRCService.setViewHandler(mHandler);
		if (ServiceIRCService.ircConn != null
				&& ServiceIRCService.ircConn.isConnected()
				&& ServiceIRCService.state == 10) {
			Log.i(LOG_TAG, "Is connected");

		} else {
			// Toast.makeText(ChatActivity.this, "Got connected",
			// Toast.LENGTH_LONG).show();
			showToast("Network disconnected");
			Intent intentMain = new Intent(Intent.ACTION_MAIN);
			intentMain.setClass(getBaseContext(),
					com.indiabolbol.hookup.GFindster.class);
			startActivity(intentMain);
			Log.e(LOG_TAG, "Network disconnected");
		}
	}

	// @Override
	// public void onContentChanged() {
	// Log.i(LOG_TAG, "onContentChanged");
	// // TODO Auto-generated method stub
	// super.onContentChanged();
	// }

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		unbindService(mConnection);
		// if (connection != null) {
		// Log.i(LOG_TAG, "Disconnecting from server");
		// connection.disconnect();
		// }
		super.onDestroy();
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		Log.i(LOG_TAG, "onPause");
		super.onRestart();
		temp = new StringBuilder();
	}

	@Override
	protected void onPause() {
		Log.i(LOG_TAG, "onPause");
		// if (connection != null) {
		// Log.i(LOG_TAG, "Disconnecting from server");
		// connection.disconnect();
		// }
		// TODO Auto-generated method stub
		super.onPause();
	}

	private void sendText(String msg) {
		ServiceIRCService.sendToChan(msg);
	}

	private void chatWithUser(String msg) {
		Log.i(LOG_TAG, "chatWithUser " + target + " :" + msg);
		ServiceIRCService.sendToUser(target, msg);
	}

	private void updateView(String msgstr) {
		String[] nickMsg = parseNickMsg(msgstr);
		if (nickMsg != null && nickMsg.length > 1) {
			Log.i(LOG_TAG, "<<<<<<<<<<<<<<<NICK 0+" + nickMsg[0]);
			Log.i(LOG_TAG, ">>>>>>>>>>>>>>>MSG 1+" + nickMsg[1]);
			Log.i(LOG_TAG, ">>>>>>>>>>>>>>>MSG 2+" + nickMsg[2]);
			Log.i(LOG_TAG, ">>>>>>>>>>>>Target is " + target);
			if (nickMsg[0].startsWith("#") && target.startsWith("#")) {
				temp.append(nickMsg[1] + " says: " + nickMsg[2] + "\n\n");
				tv.setGravity(0x50);
				tv.setTypeface(Typeface.MONOSPACE);
				tv.setTextColor(Color.WHITE);
				tv.setText(temp.toString());
				// tv.setTextColor(Color.GREEN);
				mUserText.setHint(new String(""));
				sv.fullScroll(ScrollView.FOCUS_DOWN);
				sv.smoothScrollBy(0, tv.getLineHeight());

			} else {
				if (target.equalsIgnoreCase(nickMsg[1])) {
					temp.append(nickMsg[1] + " : " + nickMsg[2] + "\n\n");
					tv.setTypeface(Typeface.MONOSPACE);
					tv.setGravity(0x50);
					tv.setText(temp.toString());
					tv.setTextColor(Color.WHITE);
					mUserText.setHint(new String(""));
					sv.fullScroll(ScrollView.FOCUS_DOWN);
					sv.smoothScrollBy(0, tv.getLineHeight());
					// }else if(nickMsg[0].startsWith("#")){
					// temp.append(nickMsg[1] + " says: " + nickMsg[2] +
					// "\n\n");
					// tv.setGravity(0x50);
					// tv.setText(temp.toString());
					// tv.setTextColor(Color.GREEN);
					// mUserText.setHint(new String(""));
					// sv.fullScroll(ScrollView.FOCUS_DOWN);
					// sv.smoothScrollBy(0, tv.getLineHeight());

				} else if ("me".equalsIgnoreCase(nickMsg[0])) {
					if (!nickMsg[1].equalsIgnoreCase("")) {
						temp.append("ME" + ":  " + nickMsg[1] + "\n\n");
						tv.setGravity(0x50);
						tv.setText(temp.toString());
						tv.setTextColor(Color.MAGENTA);
						tv.setTypeface(Typeface.MONOSPACE);
						mUserText.setHint(new String(""));
						sv.fullScroll(ScrollView.FOCUS_DOWN);
						sv.smoothScrollBy(0, tv.getLineHeight());
					}

				}
			}

		} else {
			Log.e(LOG_TAG, "Invalid Messages arrived heree...................");

		}
		mUserText.requestFocus();
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

	private OnKeyListener mKeyListener = new OnKeyListener() {
		public boolean onKey(View v, int i, KeyEvent k) {
			// listen for enter, clear box, send etc
			Log.d(LOG_TAG, "mKeyListener ONKEY ");
			if ((k.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || k
					.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
				// sendText(t);
				Log.d(LOG_TAG, "KEYEVEN ONKEY ");
				updateView("me|" + mUserText.getText().toString());
				chatWithUser(mUserText.getText().toString());
				mUserText.setText("");
				return true;
			}
			return false;
		}

	};
}
