package com.larphoid.lanchatexample;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.R.color;
import android.R.drawable;
import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.text.Editable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.larphoid.lanudpcomm.ClientEventHandler;
import com.larphoid.lanudpcomm.ClientInviteHandler;
import com.larphoid.lanudpcomm.LanUDPComm;

public class ActivityMain extends Activity implements OnItemClickListener, ClientInviteHandler, ClientEventHandler, OnClickListener, OnItemLongClickListener {
	// private static final String TAG = "com.larphoid.LanGameExample";
	private static final int DISCOVERY_PORT = 21371;
	private static final int COMM_PORT = 1234;
	private static final int MAXPACKAGESIZE = 0x100;
	private static final String PREFS_NAME = "prefs";
	private static final String PREF_NAME = "name";
	private static final String imgStartTag = "<img src=\"";
	private static final String imgEndTag = "\">";
	private static final String[] EMOTIONS = new String[] {
		"emoticon_evilgrin",
		"emoticon_grin",
		"emoticon_happy",
		"emoticon_heart",
		"emoticon_heart_break",
		"emoticon_smile",
		"emoticon_surprised",
		"emoticon_tongue",
		"emoticon_unhappy",
		"emoticon_waii",
		"emoticon_wink",
		"smiley_confuse",
		"smiley_cool",
		"smiley_cry",
		"smiley_fat",
		"smiley_mad",
		"smiley_red",
		"smiley_roll",
		"smiley_slim",
		"smiley_yell"
	};
	private static final String[] EMOTICHARS = new String[] {
		"\uff01",
		"\uff02",
		"\uff03",
		"\uff04",
		"\uff05",
		"\uff06",
		"\uff07",
		"\uff08",
		"\uff09",
		"\uff0a",
		"\uff0b",
		"\uff0c",
		"\uff0d",
		"\uff0e",
		"\uff0f",
		"\uff10",
		"\uff11",
		"\uff12",
		"\uff13",
		"\uff14"
	};

	private static final int MENU_NAME = Menu.FIRST + 0;
	private static final int MENU_REFRESH = Menu.FIRST + 1;

	private String myName;
	private LanUDPComm lanUdpComm;
	private ListView clientList;
	private ClientsAdapter clientsAdapter;
	private LinearLayout chatWindow;
	private TextView tvChatClientName;
	private ListView chatmessageList;
	private ChatMessageAdapter chatmessageAdapter;
	private EditText etMessage;
	private ImageButton btEmoticons, btSend;
	private LinearLayout inputContainer;
	private GridView emoticonsList;
	private GridView emoticharsList;
	private List<Client> clients = new ArrayList<Client>();
	private Client chatclient;
	private List<String> clientschattingwithme = new ArrayList<String>();
	private LayoutInflater inflater;
	private Handler mHandler = new Handler();
	private MyImageGetter imageGetter = new MyImageGetter();
	private InputMethodManager inputMethodManager;
	private Point displaySize = new Point();

	private class MyImageGetter implements ImageGetter {
		@Override
		public Drawable getDrawable(String source) {
			Drawable drawFromPath;
			int path = ActivityMain.this.getResources().getIdentifier(source, "drawable", ActivityMain.this.getPackageName());
			try {
				drawFromPath = (Drawable) ActivityMain.this.getResources().getDrawable(path);
			} catch (Resources.NotFoundException e) {
				drawFromPath = (Drawable) ActivityMain.this.getResources().getDrawable(drawable.checkbox_on_background);
			}
			drawFromPath.setBounds(0, 0, drawFromPath.getMinimumWidth(), drawFromPath.getMinimumHeight());
			return drawFromPath;
		}
	}

	private class ClientsAdapter extends BaseAdapter {
		@Override
		public final int getCount() {
			return lanUdpComm.getNumClients();
		}

		@Override
		public final long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) convertView = inflater.inflate(R.layout.clientitem, parent, false);
			int numMessages = 0;
			final String lookup = lanUdpComm.getClientIPfromPosition(position).getHostAddress();
			for (Client client : clients) {
				if (client.ip.getHostAddress().equals(lookup)) {
					for (Long timestamp : client.timestamps) {
						if (timestamp.longValue() > client.lastRead) {
							numMessages++;
						}
					}
				}
			}
			final TextView t = (TextView) convertView.findViewById(R.id.clientname);
			final TextView n = (TextView) convertView.findViewById(R.id.newmessages);
			if (numMessages > 0) {
				t.setText(lanUdpComm.getClientName(position));
				n.setText(" (" + numMessages + ")");
			} else {
				t.setText(lanUdpComm.getClientName(position));
				n.setText("");
			}
			return convertView;
		}

		@Override
		public Object getItem(int position) {
			return lanUdpComm.getClientsItemAtPosition(position);
		}
	}

	private class ChatMessageAdapter extends BaseAdapter {
		final LayoutParams arrowLeft = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		final LayoutParams arrowRight = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		public ChatMessageAdapter() {
			arrowLeft.addRule(RelativeLayout.ALIGN_LEFT, R.id.messagecontainer);
			arrowRight.addRule(RelativeLayout.ALIGN_RIGHT, R.id.messagecontainer);
		}

		@Override
		public int getCount() {
			if (chatclient != null) return chatclient.messages.size();
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (chatclient != null) {
				final boolean fromSender = chatclient.isFromSender.get(position).booleanValue();
				if (convertView == null) convertView = inflater.inflate(R.layout.chatmessage, parent, false);
				final ImageView arrow = (ImageView) convertView.findViewById(R.id.arrow);
				if (fromSender) {
					arrow.setImageResource(R.drawable.arrow_left_top);
					arrow.setLayoutParams(arrowLeft);
				} else {
					arrow.setImageResource(R.drawable.arrow_right_top);
					arrow.setLayoutParams(arrowRight);
				}
				TextView tv;
				final LinearLayout ll = (LinearLayout) convertView.findViewById(R.id.messagecontainer);
				final String message = chatclient.messages.get(position);
				tv = (TextView) convertView.findViewById(R.id.messagetext);
				tv.setText(Html.fromHtml(message, imageGetter, null));
				tv = (TextView) convertView.findViewById(R.id.timestamp);
				tv.setText("" + lanUdpComm.getTimeString(chatclient.timestamps.get(position).longValue(), LanUDPComm.TIMESTRING_SHORT));
				if (fromSender) {
					ll.setGravity(Gravity.LEFT);
				} else {
					ll.setGravity(Gravity.RIGHT);
				}
				final RelativeLayout rl = (RelativeLayout) convertView;
				if (fromSender) rl.setGravity(Gravity.LEFT);
				else rl.setGravity(Gravity.RIGHT);
				if (fromSender) convertView.setPadding(0, 0, 40, 0);
				else convertView.setPadding(40, 0, 0, 0);
				convertView.requestLayout();
				return convertView;
			}
			return null;
		}
	}

	private class EmoticonsAdapter extends BaseAdapter {
		public EmoticonsAdapter() {
			emoticonsList.setColumnWidth(getResources().getDrawable(R.drawable.emoticon_evilgrin).getMinimumWidth());
			emoticonsList.setHorizontalSpacing(2);
			emoticonsList.setVerticalSpacing(2);
		}

		@Override
		public int getCount() {
			return EMOTIONS.length;
		}

		@Override
		public Object getItem(int position) {
			return Html.fromHtml(imgStartTag + EMOTIONS[position] + imgEndTag, imageGetter, null);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) convertView = inflater.inflate(R.layout.emoticonitem, parent, false);
			((TextView) convertView).setText((Spanned) getItem(position));
			return convertView;
		}
	}

	private class EmoticharsAdapter extends BaseAdapter {
		public EmoticharsAdapter() {
			emoticharsList.setColumnWidth(getResources().getDrawable(R.drawable.emoticon_evilgrin).getMinimumWidth());
			emoticharsList.setHorizontalSpacing(2);
			emoticharsList.setVerticalSpacing(2);
		}

		@Override
		public int getCount() {
			return 0x3ff;
		}

		@Override
		public Object getItem(int position) {
			if (position >= 0x100) {
				return String.valueOf((char) (0x2400 + position - 0x100));
			}
			return String.valueOf((char) (0x2100 + position));
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) convertView = inflater.inflate(R.layout.emoticharitem, parent, false);
			((TextView) convertView).setText((String) getItem(position));
			return convertView;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		loadPreferences();
		inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inflater = LayoutInflater.from(this);
		clientList = (ListView) findViewById(R.id.clientlist);
		clientList.setOnItemClickListener(this);
		clientList.setOnItemLongClickListener(this);
		chatWindow = (LinearLayout) findViewById(R.id.chatwindow);
		inputContainer = (LinearLayout) findViewById(R.id.inputcontainer);
		emoticonsList = (GridView) findViewById(R.id.emoticonslist);
		emoticonsList.setAdapter(new EmoticonsAdapter());
		emoticonsList.setOnItemClickListener(this);
		emoticharsList = (GridView) findViewById(R.id.emoticharslist);
		emoticharsList.setAdapter(new EmoticharsAdapter());
		emoticharsList.setOnItemClickListener(this);
		tvChatClientName = (TextView) findViewById(R.id.chatclientname);
		tvChatClientName.setText("");
		chatmessageList = (ListView) findViewById(R.id.messagelist);
		chatmessageAdapter = new ChatMessageAdapter();
		chatmessageList.setAdapter(chatmessageAdapter);
		etMessage = (EditText) findViewById(R.id.textmessage);
		btEmoticons = (ImageButton) findViewById(R.id.emopopup);
		btEmoticons.setOnClickListener(this);
		btSend = (ImageButton) findViewById(R.id.sendbutton);
		btSend.setOnClickListener(this);
		lanUdpComm = new LanUDPComm(this, null, DISCOVERY_PORT, COMM_PORT, MAXPACKAGESIZE, this, this, myName, false);
		clientsAdapter = new ClientsAdapter();
		clientList.setAdapter(clientsAdapter);
		lanUdpComm.setClientsAdapter(clientsAdapter);
		resize();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		resize();
		super.onConfigurationChanged(newConfig);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final Editable edt = etMessage.getText();
		final int start = etMessage.getSelectionStart();
		final int end = etMessage.getSelectionEnd();
		switch (parent.getId()) {
		case R.id.clientlist:
			chatWindow.setBackgroundResource(R.drawable.bg_messages_active);
			final Map<String, String> item = (Map<String, String>) clientsAdapter.getItem(position);
			final Client prevClient = chatclient;
			tvChatClientName.setText(item.get(LanUDPComm.FROM_CLIENTS[0]));
			lanUdpComm.inviteClientForConnection(position, null, new String[] {
				myName
			});
			chatclient = null;
			final InetAddress lookupAddr = lanUdpComm.getClientIPfromPosition(position);
			for (Client client : clients) {
				final String addr = lookupAddr.getHostAddress();
				if (client.ip.getHostAddress().equals(addr)) {
					chatclient = client;
					break;
				}
			}
			if (chatclient != null) {
				chatclient.lastRead = System.currentTimeMillis();
				clientsAdapter.getView(position, clientList.getChildAt(position - clientList.getFirstVisiblePosition()), clientList);
				if (prevClient == null || !prevClient.ip.getHostAddress().equals(chatclient.ip.getHostAddress())) {
					mHandler.post(scrollToBottom);
				}
				chatmessageAdapter.notifyDataSetChanged();
			} else {
				final Client client = new Client();
				client.ip = lookupAddr;
				clients.add(client);
				chatclient = client;
				chatmessageAdapter.notifyDataSetChanged();
			}
			inputContainer.setVisibility(View.VISIBLE);
			etMessage.requestFocus();
			if (clientschattingwithme.contains(chatclient.ip.getHostAddress())) {
				tvChatClientName.setTextColor(getResources().getColor(color.primary_text_dark));
				tvChatClientName.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable.presence_online, 0);
			} else {
				tvChatClientName.setTextColor(getResources().getColor(color.darker_gray));
				tvChatClientName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
			break;
		case R.id.emoticonslist:
			edt.replace(start, end, (Spanned) emoticonsList.getItemAtPosition(position));
			etMessage.setSelection(start + 1);
			break;
		case R.id.emoticharslist:
			edt.replace(start, end, emoticharsList.getItemAtPosition(position).toString());
			etMessage.setSelection(start + 1);
			break;
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
		return true;
	}

	@Override
	public void onClick(View view) {
		final Editable edt = etMessage.getText();
		switch (view.getId()) {
		case R.id.emopopup:
			if (inputMethodManager.isActive()) {
				inputMethodManager.hideSoftInputFromWindow(etMessage.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}
			if (emoticonsList.getVisibility() == View.VISIBLE) {
				btEmoticons.setImageResource(R.drawable.ic_close);
				emoticonsList.setVisibility(View.GONE);
				emoticharsList.setVisibility(View.VISIBLE);
			} else if (emoticharsList.getVisibility() == View.VISIBLE) {
				emoticonsList.setVisibility(View.GONE);
				emoticharsList.setVisibility(View.GONE);
				btEmoticons.setImageResource(R.drawable.ic_emoticons);
			} else {
				btEmoticons.setImageResource(R.drawable.ic_symbols);
				emoticonsList.setVisibility(View.VISIBLE);
			}
			break;
		case R.id.sendbutton:
			if (chatclient != null) {
				String message = Html.toHtml(edt).trim();
				if (message.length() == 0) return;
				final int pos = message.indexOf("<p");
				if (pos != -1) {
					final String p = message.substring(pos, message.indexOf(">") + 1);
					message = message.replace(p, "");
					message = message.replace("</p>", "");
				}
				final long timestamp = System.currentTimeMillis();
				chatclient.lastRead = timestamp;
				chatclient.timestamps.add(Long.valueOf(timestamp));
				chatclient.messages.add(message);
				chatclient.isFromSender.add(Boolean.FALSE);
				for (int i = 0; i < EMOTIONS.length; i++) {
					message = message.replaceAll(imgStartTag + EMOTIONS[i] + imgEndTag, EMOTICHARS[i]);
				}
				Log.i(this.getPackageName(), "[" + message + "]");
				lanUdpComm.sendClientPacket(message);
				chatmessageAdapter.notifyDataSetChanged();
				mHandler.post(scrollToBottom);
				edt.clear();
			}
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_NAME, 0, R.string.menu_name);
		menu.add(0, MENU_REFRESH, 0, R.string.menu_refresh).setIcon(drawable.ic_popup_sync);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_NAME:
			changeMyDisplayName();
			break;
		case MENU_REFRESH:
			lanUdpComm.sendDiscoveryToAllIps(myName);
			break;
		}
		return true;
	}

	@Override
	public boolean onSearchRequested() {
		lanUdpComm.sendDiscoveryRequest(myName);
		return false;
	}
 
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (emoticonsList.getVisibility() == View.VISIBLE || emoticharsList.getVisibility() == View.VISIBLE) {
				emoticonsList.setVisibility(View.GONE);
				emoticharsList.setVisibility(View.GONE);
				btEmoticons.setImageResource(R.drawable.ic_emoticons);
				return true;
			}
			if (chatclient != null) {
				endChat();
				return true;
			}
			lanUdpComm.cleanup();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		System.exit(0);
		super.onDestroy();
	}

	@Override
	public String[] onInviteAccept() {
		return null;
	}

	@Override
	public void onStartConnection(final String data[], final int offset, final DatagramPacket pack) {
		clientschattingwithme.add(pack.getAddress().getHostAddress());
		if (chatclient != null && clientschattingwithme.contains(chatclient.ip.getHostAddress())) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					tvChatClientName.setTextColor(getResources().getColor(color.primary_text_dark));
					tvChatClientName.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable.presence_online, 0);
				}
			});
		} else {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					tvChatClientName.setTextColor(getResources().getColor(color.darker_gray));
					tvChatClientName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				}
			});
		}
	}

	@Override
	public void onClientAccepted(final String[] data, final int offset, final DatagramPacket pack) {
		final String addr = pack.getAddress().getHostAddress();
		clientschattingwithme.add(addr);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tvChatClientName.setTextColor(getResources().getColor(color.primary_text_dark));
				tvChatClientName.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable.presence_online, 0);
			}
		});
	}

	@Override
	public void onClientEvent(final byte[] data, final int offset, final int dataLength, final DatagramPacket pack) {
		Client client = null;
		final String lookupAddr = pack.getAddress().getHostAddress();
		for (Client c : clients) {
			if (lookupAddr != null && lookupAddr.equals(c.ip.getHostAddress())) {
				client = c;
				break;
			}
		}
		if (client == null) {
			client = new Client();
			client.ip = pack.getAddress();
			clients.add(client);
		}
		String message = new String(data, offset, dataLength - offset);
		for (int i = 0; i < EMOTIONS.length; i++) {
			message = message.replaceAll(EMOTICHARS[i], imgStartTag + EMOTIONS[i] + imgEndTag);
		}
		final long timestamp = System.currentTimeMillis();
		client.timestamps.add(Long.valueOf(timestamp));
		client.messages.add(message);
		client.isFromSender.add(Boolean.TRUE);
		if (chatclient != null && chatclient.ip.getHostAddress().equals(lookupAddr)) {
			chatclient.lastRead = timestamp;
			runOnUiThread(new Runnable() {
				public void run() {
					chatmessageAdapter.notifyDataSetChanged();
					if (chatmessageList.getLastVisiblePosition() >= chatmessageAdapter.getCount() - 2) mHandler.post(scrollToBottom);
				}
			});
		} else {
			final int position = lanUdpComm.getClientPositionFromIP(client.ip);
			if (position != -1) {
				runOnUiThread(new Runnable() {
					public void run() {
						clientsAdapter.getView(position, clientList.getChildAt(position - clientList.getFirstVisiblePosition()), clientList);
					}
				});
			}
		}
	}

	@Override
	public void onClientEndConnection(final DatagramPacket pack) {
		final String addr = pack.getAddress().getHostAddress();
		do {
		} while (clientschattingwithme.remove(addr));
		if (chatclient != null && addr.equals(chatclient.ip.getHostAddress())) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					tvChatClientName.setTextColor(getResources().getColor(color.darker_gray));
					tvChatClientName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				}
			});
		}
	}

	@Override
	public void onClientNotResponding(final DatagramPacket pack) {
		/* not used */
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ------------------------------------------------------- CLASSES and METHODS -------------------------------------------------
	// -----------------------------------------------------------------------------------------------------------------------------

	public void resize() {
		displaySize.x = getResources().getDisplayMetrics().widthPixels;
		displaySize.y = getResources().getDisplayMetrics().heightPixels;
		emoticonsList.getLayoutParams().height = (int) (displaySize.y * 0.4f);
		emoticharsList.getLayoutParams().height = (int) (displaySize.y * 0.4f);
	}

	private void loadPreferences() {
		final SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		myName = preferences.getString(PREF_NAME, Build.MODEL + "_" + Secure.getString(getContentResolver(), Secure.ANDROID_ID));
	}

	private void savePreferences() {
		final SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefs = preferences.edit();
		prefs.putString(PREF_NAME, myName);
		prefs.commit();
	}

	private void changeMyDisplayName() {
		final EditText edit = new EditText(this);
		edit.setSingleLine(true);
		edit.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		edit.setPadding(5, 5, 5, 5);
		edit.setText(myName);
		edit.setHint(R.string.menu_name);
		edit.selectAll();
		new AlertDialog.Builder(this).setCancelable(true).setIcon(drawable.ic_dialog_info).setTitle(R.string.menu_name).setView(edit).setNegativeButton(string.cancel, null).setPositiveButton(string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				myName = edit.getText().toString();
				savePreferences();
				lanUdpComm.sendDiscoveryRequest(myName);
			}
		}).show();
	}

	private void endChat() {
		lanUdpComm.sendClientByeBye();
		chatclient = null;
		chatWindow.setBackgroundResource(R.drawable.bg_messages_inactive);
		chatmessageAdapter.notifyDataSetChanged();
		inputContainer.setVisibility(View.GONE);
		tvChatClientName.setText(" ");
		tvChatClientName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		etMessage.setText("");
	}

	private Runnable scrollToBottom = new Runnable() {
		@Override
		public void run() {
			chatmessageList.setSelection(chatclient.messages.size() - 1);
		}
	};
}
