package com.phonegap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;


import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.BytestreamsProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.IBBProviders;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import android.util.Log;
import android.webkit.WebView;

public class ChatHandler {

	ProviderManager mPm = null;
	XMPPConnection mConn = null;
	Roster mRoster = null;
	ChatManager mChatManager = null;
	HashMap<String, Chat> openChat;
	boolean debug = false;

	WebView mView;
	
	ChatHandler(WebView view)
	{
		mView = view;
		openChat = new HashMap<String,Chat>();
		mPm = ProviderManager.getInstance();
		configure(mPm);
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public void connect(ConnectionConfiguration config, String username, String password, String resource) {
		config.setDebuggerEnabled(debug);
		mConn = new XMPPConnection(config);
		try {
			mView.loadUrl("javascript:navigator.xmppClient._xmppClientConnecting()");
			mConn.connect();
			mConn.login(username, password, resource);
		} catch (XMPPException e) {
			e.printStackTrace();
			mView.loadUrl("javascript:navigator.xmppClient._xmppClientDidNotConnect()");
			return;
		}
		finally
		{
			mView.loadUrl("javascript:navigator.xmppClient._xmppClientDidConnect()");
			mChatManager = mConn.getChatManager();
			setupListeners();
			setupRosterListener();
		}
	}

	public void connect(String uri, String username, String password, String resource, int port)
	{
		connect(new ConnectionConfiguration(uri, port), username, password, resource);
	}

	public void connect(String jid, String password, String resource)
	{
		int at = jid.lastIndexOf('@');
		String domain = jid.substring(at + 1);
		String username = jid.substring(0, at);
		connect(new ConnectionConfiguration(domain), username, password, resource);
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
	
    private DiscoverItems discoverItems(String entityID) throws XMPPException {
        return discoverItems(entityID, null);
    }


	private DiscoverItems discoverItems(String entityId, String node) throws XMPPException
	{
	       // Discover the entity's info
	       // Discover the entity's items
        DiscoverItems disco = new DiscoverItems();
        disco.setType(IQ.Type.GET);
        disco.setTo(entityId);
        disco.setNode(node);

        // Create a packet collector to listen for a response.
        PacketCollector collector =
            mConn.createPacketCollector(new PacketIDFilter(disco.getPacketID()));

        mConn.sendPacket(disco);

        // Wait up to 5 seconds for a result.
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from the server.");
        }
        if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
        return (DiscoverItems) result;
        
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
	
	// This epic fails currently 
	public void discoverServices(String resource)
	{
		ServiceDiscoveryManager discoStu = ServiceDiscoveryManager.getInstanceFor(mConn);
		if(discoStu == null)
		{
			//Disco Stu is coming back baby!!!!
			discoStu = new ServiceDiscoveryManager(mConn);
		}
		DiscoverItems discoItems;
		try {
			discoItems = discoverItems(resource);
			Iterator it = discoItems.getItems();
			
			while(it.hasNext())
			{
				DiscoverItems.Item item = (DiscoverItems.Item) it.next();
				String params = item.getEntityID() + "','" + item.getNode() + "','" + item.getName();
				// Take this data, and send it to a collector
			}
		} catch (XMPPException e) {
			// Discovery failed, call JS Fail
		}
		
	}
	
	/* This was grabbed from the Ignite Realtime Board */
	public void configure(ProviderManager pm) {
		 
        //  Private Data Storage
        pm.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());
 
 
        //  Time
        try {
            pm.addIQProvider("query","jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            Log.w("TestClient", "Can't load class for org.jivesoftware.smackx.packet.Time");
        }
 
        //  Roster Exchange
        pm.addExtensionProvider("x","jabber:x:roster", new RosterExchangeProvider());
 
        //  Message Events
        pm.addExtensionProvider("x","jabber:x:event", new MessageEventProvider());
 
        //  Chat State
        pm.addExtensionProvider("active","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
 
        pm.addExtensionProvider("composing","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
 
        pm.addExtensionProvider("paused","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
 
        pm.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
 
        pm.addExtensionProvider("gone","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
 
        //  XHTML
        pm.addExtensionProvider("html","http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());
 
        //  Group Chat Invitations
        pm.addExtensionProvider("x","jabber:x:conference", new GroupChatInvitation.Provider());
 
        //  Service Discovery # Items    
        pm.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
 
        //  Service Discovery # Info
        pm.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
 
        //  Data Forms
        pm.addExtensionProvider("x","jabber:x:data", new DataFormProvider());
 
        //  MUC User
        pm.addExtensionProvider("x","http://jabber.org/protocol/muc#user", new MUCUserProvider());
 
        //  MUC Admin    
        pm.addIQProvider("query","http://jabber.org/protocol/muc#admin", new MUCAdminProvider());
 
 
        //  MUC Owner    
        pm.addIQProvider("query","http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());
 
        //  Delayed Delivery
        pm.addExtensionProvider("x","jabber:x:delay", new DelayInformationProvider());
 
        //  Version
        try {
            pm.addIQProvider("query","jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException e) {
            //  Not sure what's happening here.
        }
 
        //  VCard
        pm.addIQProvider("vCard","vcard-temp", new VCardProvider());
 
        //  Offline Message Requests
        pm.addIQProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());
 
        //  Offline Message Indicator
        pm.addExtensionProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());
 
        //  Last Activity
        pm.addIQProvider("query","jabber:iq:last", new LastActivity.Provider());
 
        //  User Search
        pm.addIQProvider("query","jabber:iq:search", new UserSearch.Provider());
 
        //  SharedGroupsInfo
        pm.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());
 
        //  JEP-33: Extended Stanza Addressing
        pm.addExtensionProvider("addresses","http://jabber.org/protocol/address", new MultipleAddressesProvider());
 
        //   FileTransfer
        pm.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());
 
        pm.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
 
        pm.addIQProvider("open","http://jabber.org/protocol/ibb", new IBBProviders.Open());
 
        pm.addIQProvider("close","http://jabber.org/protocol/ibb", new IBBProviders.Close());
 
        pm.addExtensionProvider("data","http://jabber.org/protocol/ibb", new IBBProviders.Data());
 
        //  Privacy
        pm.addIQProvider("query","jabber:iq:privacy", new PrivacyProvider());
 
        pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
        pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
        pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
        pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
        pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
        pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());
    }
	
}
