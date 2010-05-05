package com.phonegap;

import java.util.Collection;
import java.util.HashMap;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import android.util.Log;
import android.webkit.WebView;

public class ChatHandler {

	XMPPConnection mConn = null;
	Roster mRoster = null;
	ChatManager mChatManager = null;
	HashMap<String, Chat> openChat;
	
	WebView mView;
	
	ChatHandler(WebView view)
	{
		mView = view;
		openChat = new HashMap<String,Chat>();
	}
	
	public void connect(String uri, String username, String password, String resource, int port)
	{
		ConnectionConfiguration config = new ConnectionConfiguration(uri, port);
		mConn = new XMPPConnection(config);
		try {
			mView.loadUrl("javascript:navigator.xmppClient._xmppClientConnecting()");
			mConn.connect();
			mConn.login(username, password, resource);
		} catch (XMPPException e) {
			e.printStackTrace();
			mView.loadUrl("javascript:navigator.xmppClient._xmppClientDidNotConnect()");
		}
		finally
		{
			mView.loadUrl("javascript:navigator.xmppClient._xmppClientDidConnect()");
			mChatManager = mConn.getChatManager();
			setupListeners();
			setupRosterListener();
		}
	}
	
	private void setupListeners()
	{
		/*
		 * This is the actual code that handles what happens with XMPP users
		 */
		PacketListener msgListener = new PacketListener() {

			public void processPacket(Packet packet) {
				Message message = (Message) packet;
				String origin = message.getFrom().split("/")[0];
				Chat chat = openChat.get(origin);
				if(chat == null)
					setupChat(message.getFrom());
                if (message.getBody() != null) {
                	mView.loadUrl("javascript:alert('" + message.getBody() + "');");
                	getRoster();
                }
				
			}
			
		};
		PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
		mConn.addPacketListener(msgListener, filter);
	}	
	
	public void getRoster()
	{
		if (mRoster != null)
		{
			Collection<RosterEntry> entries = mRoster.getEntries();
			for(RosterEntry entry: entries){
				//Access the WebView and pass the entries back to the Javascript
				//Most likely to the EventBroadcaster
				Log.d("Jabber", entry.getName());
			}
		}
	}
	
	public void disconnect()
	{
	}
	
	public void findServices()
	{
	
	}
	
	/*
	 * This handles changes in the roster, and all presence information
	 * 
	 */
	private void setupRosterListener()
	{
		if (mRoster == null)
			mRoster = mConn.getRoster();
		RosterListener rListen = new RosterListener() {

			public void entriesAdded(Collection<String> arg0) {
				// TODO Auto-generated method stub
				for (String str : arg0)
				{
					//Send the changes to the Javascript
				}
			}

			public void entriesDeleted(Collection<String> arg0) {
				for (String str : arg0)
				{
				
				}
				
			}

			public void entriesUpdated(Collection<String> arg0) {
				for(String str: arg0)
				{
					
				}
			}

			public void presenceChanged(Presence arg0) {
				String presence = arg0.getFrom();
			}
			
		};
		mRoster.addRosterListener(rListen);
	}
	
	public Chat setupChat(final String person)
	{
		MessageListener listener = new MessageListener() {
			public void processMessage(Chat chat, Message message)
			{
				// TODO: Fix this so that this actually does something!
			}
		};
		
		Chat chat = mChatManager.createChat(person, listener);
		openChat.put(person, chat);
		return chat;
	}
	
	public void sendMessage(String person, String message)
	{
		Chat chat = openChat.get(person);
		if(chat == null)
			chat = setupChat(person);
		try {
			chat.sendMessage(message);
		} catch (XMPPException e) {
			mView.loadUrl("javascript:navigator.xmppClient._didReceiveError()");
			e.printStackTrace();
		}
	}
	
}
