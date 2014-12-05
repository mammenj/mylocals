package com.indiabolbol.hookup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class GFindsterPreferences extends Activity {
	public static final String PREFS_NAME = "gfindsterPrefs";
	private EditText nickText;
	private EditText serverText;
	private EditText lagtimeText;

	// private CheckBox listBox;
	private CheckBox disableLocChat;
	// private CheckBox pmBox;
	String LOG_TAG="GFindsterPreferences";

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.options);
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

		Button saveButton = (Button) findViewById(R.id.optSave);
		saveButton.setOnClickListener(mSaveListener);

		Button cancButton = (Button) findViewById(R.id.optCancel);
		cancButton.setOnClickListener(mCancListener);

		nickText = (EditText) findViewById(R.id.optNick);
		lagtimeText = (EditText) findViewById(R.id.etLagtime);
		serverText = (EditText) findViewById(R.id.chatserver);
		serverText.setEnabled(false);

		disableLocChat = (CheckBox) findViewById(R.id.optCbDisableLocChat);

		this.setTitle("GFindster Preferences");

		disableLocChat.setChecked(settings.getBoolean("enableLocChat", true));
		double nickrnd=Math.random();
		int nickrndInt = (int)(100* nickrnd);
		nickText.setText(settings.getString("defNick", "gfindster"+nickrndInt));
		lagtimeText.setText(settings.getString("lagtime", "30"));
		serverText.setText(settings.getString("serverText", "indiabolbol.com"));
	}

	private OnClickListener mCancListener = new OnClickListener() {
		public void onClick(View v) {
			finish();
		}
	};

	private OnClickListener mSaveListener = new OnClickListener() {
		public void onClick(View v) {
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();

			if (nickText.getText().toString().matches(
					"[A-Za-z][0-9a-zA-Z\\[\\]\\\\^\\-_`{|}]{0,29}")) {
				editor.putBoolean("enableLocChat", disableLocChat.isChecked());
				// editor.putBoolean("pmAlert", pmBox.isChecked());
				// editor.putBoolean("showList", listBox.isChecked());

				editor.putString("defNick", nickText.getText().toString());
				editor.putString("serverText", serverText.getText().toString());
				editor.putString("lagtime", lagtimeText.getText().toString());
				editor.commit();
				Log.i(LOG_TAG,">>>>>>>>>>>>defNick "+nickText.getText().toString());
				Log.i(LOG_TAG,">>>>>>>>>>>>serverText "+serverText.getText().toString());
				Log.i(LOG_TAG,">>>>>>>>>>>>disableLocChat "+disableLocChat.isChecked());
				Log.i(LOG_TAG,">>>>>>>>>>>>lagtimeText.getText().toString() "+lagtimeText.getText().toString());
				finish();
			} else {
				Log.e(LOG_TAG,"Error in CHAT NAME"+nickText.getText().toString());
//
//				 AlertDialog.show(GFindsterPreferences.this, "Invalid Nickname",
//				 "Chat Name contains invalid characters or is too long.", "Okay", true);
			}
			// startActivity(new Intent(ActivityAndroidChatMain.this,
			// ActivityOptions.class));

		}
	};
}
