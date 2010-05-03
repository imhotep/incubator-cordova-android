package com.phonegap;

import java.util.Collection;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import android.webkit.WebView;

public class ChatHandler {

	XMPPConnection mConn;
	Roster mRoster = null;
	ChatManager mChatManager = null;
	Collection <Chat> chats;
	
	WebView mView;
	
	ChatHandler(String uri, String username, String password, String resource, int port, WebView view)
	{
		mView = view;
		ConnectionConfiguration config = new ConnectionConfiguration(uri, port);
		mConn = new XMPPConnection(config);
		try {
			mView.loadUrl("javascript:navigator.xmpp._xmppClientConnecting()");
			mConn.connect();
			mConn.login(username, password, resource);
		} catch (XMPPException e) {
			e.printStackTrace();
			mView.loadUrl("javascript:navigator.xmpp._xmppClientDidNotConnect()");
		}
		finally
		{
			mView.loadUrl("javascript:navigator.xmpp._xmppClientDidConnect()");
		}
	}
	
	public void getRoster()
	{
		Collection<RosterEntry> entries = mRoster.getEntries();
		for(RosterEntry entry: entries){
			//Access the WebView and pass the entries back to the Javascript
			//Most likely to the EventBroadcaster
		}
	}
	
	/*
	 * This handles changes in the roster, and all presence information
	 * 
	 */
	public void setupRosterListener()
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
	}
	
	public void setupChat()
	{
		Chat chat = mChatManager.createChat("", new MessageListener() {
			public void processMessage(Chat chat, Message message)
			{
				mView.loadUrl("javascript:navigator.xmpp._didReceiveMessage('"+ message.getBody() + "," 
						+ chat.getParticipant());
			}

		});
	}
	
}
