package com.phonegap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket.ItemStatus;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;


import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.XHTMLManager;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferNegotiator;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
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
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.FormType;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider;
import org.jivesoftware.smackx.search.UserSearch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Log;
import android.util.Xml;
import android.webkit.WebView;

public class ChatHandler {

	ProviderManager mPm = null;
	XMPPConnection mConn = null;
	Roster mRoster = null;
	ChatManager mChatManager = null;
	HashMap<String, Chat> openChat;
	boolean debug = false;
	ServiceDiscoveryManager discoStu;
	PubSubManager mPubSubMan;
	FileTransferManager mFileMan;
	GapTransferListener mFileListener;
	String mJid;

	WebView mView;
	Context mCtx;
	
	ChatHandler(WebView view, Context ctx)
	{
		mView = view;
		mCtx = ctx;
		openChat = new HashMap<String,Chat>();
		mPm = ProviderManager.getInstance();
		configure(mPm);
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
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
	
	public void connect(ConnectionConfiguration config, String username, String password, String resource) {
		config.setDebuggerEnabled(debug);
		mConn = new XMPPConnection(config);
		try {
			mView.loadUrl("javascript:navigator.xmppClient._xmppClientConnecting()");
			mConn.connect();
			mConn.login(username, password, resource);
			discoStu = new ServiceDiscoveryManager(mConn);
			discoProperties();
		} catch (XMPPException e) {
			e.printStackTrace();
			mView.loadUrl("javascript:navigator.xmppClient._xmppClientDidNotConnect()");
			return;
		}
		finally
		{
			if(mConn != null)
			{
				mView.loadUrl("javascript:navigator.xmppClient._xmppClientDidConnect('" + config.getHost() + "');");
				mChatManager = mConn.getChatManager();
				setupListeners();
				setupRosterListener();
				mJid = username + "@" + config.getHost();
			}
		}
	}
	
	/*
	 * Add features to our XMPP client
	 * We do support Data forms, XHTML-IM, Service Discovery
	 * 
	 */

	private void discoProperties() {
		discoStu.addFeature("http://jabber.org/protocol/xhtml-im");
		discoStu.addFeature("jabber:x:data");
		discoStu.addFeature("http://jabber.org/protocol/disco#info");
		discoStu.addFeature("jabber:iq:privacy");
		discoStu.addFeature("http://jabber.org/protocol/si");
		discoStu.addFeature("http://jabber.org/protocol/bytestreams");
		discoStu.addFeature("http://jabber.org/protocol/ibb");
 	}

	/*
	 * This handles the chat listener.  We can't simply listen to chats for some reason, and intead have to grab the chats
	 * from the packets.  The other listeners work properly in SMACK
	 * 
	 */
	
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
				if (XHTMLManager.isXHTMLMessage(message))
				{
					processXHTML(message, origin);                			
				}
		        Form msgForm = Form.getFormFrom(message);
		        if (msgForm != null)
		        {
		        	processForm(msgForm);
		        }
                if (message.getBody() != null) {
                	mView.loadUrl("javascript:navigator.xmppClient._didReceiveMessage('" + message.getBody() + "','" + origin 
                			+ "','" + message.getPacketID() + "');");           
                }
				
			}
			
			private void processXHTML(Message message, String origin)
			{
				String xmlMessage = message.toXML();
				Log.d("XMPPTest", xmlMessage);
														
				//Let's do this quick and dirty.
				int start = xmlMessage.indexOf("<body xmlns");
				int end = xmlMessage.indexOf("</html>");
				if (start > 0 && end > 0)
				{
					String data = URLEncoder.encode(xmlMessage.substring(start, end));
					String cmd = "javascript:navigator.xmppClient._didReceiveHtmlMessage('" + data + "','" + origin 
								+ "','" + message.getPacketID() + "');";
					mView.loadUrl(cmd);
				}			
			}
			
			/*
			 * Take a form and convert it to JSON, then send it!
			 * Note: I could probably do something better than manual JSON
			 */
			private void processForm(Form msgForm)
			{
				// Take the form we got, convert to XML and URLEncode it
				String xmlForm = URLEncoder.encode(msgForm.getDataFormToSend().toXML());
				mView.loadUrl("javascript:navigator.xmppClient._didReceiveForm('" + xmlForm + "')");
			}
		};

		PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
		mConn.addPacketListener(msgListener, filter);
	}	
	
	
	private void setInformation()
	{
		ServiceDiscoveryManager.getInstanceFor(mConn).setNodeInformationProvider(
				"http://jabber.org/protocol/muc#rooms", 
				new NodeInformationProvider() {

					public List<String> getNodeFeatures() {
						// TODO Auto-generated method stub
						return null;
					}

					public List<Identity> getNodeIdentities() {
						// TODO Auto-generated method stub
						return null;
					}

					public List<Item> getNodeItems() {
						ArrayList answer = new ArrayList();
						Iterator rooms = MultiUserChat.getJoinedRooms(mConn, null);
						while (rooms.hasNext())
						{
							answer.add(new DiscoverItems.Item((String)rooms.next()));
						}
						return null;
					}
					
				});
		
	}
	
	/*
	 * PubSub Methods
	 * 
	 */
	
	public void publish(String resource, String name, String xmlns, String xmlPayload, String nodeType, boolean persist)
	{
		ConfigureForm form = new ConfigureForm(FormType.submit);
		form.setPersistentItems(persist);
		form.setDeliverPayloads(true);			

		form.setAccessModel(AccessModel.open);
		
		if (mPubSubMan == null)
			mPubSubMan = new PubSubManager(mConn, resource);
		
		// This appears to have changed since I last wrote this!
		LeafNode myNode;
		try {
			myNode = (LeafNode) mPubSubMan.getNode(nodeType);
		}
		catch (XMPPException e)
		{
			// Let's just set it to null.  If there's STILL a problem, we'll catch it again later.
			myNode = null;
		}
		
		try {
			if (myNode == null)
				myNode = (LeafNode) mPubSubMan.createNode(nodeType, form);
			SimplePayload payload = new SimplePayload(name,xmlns, xmlPayload);
			PayloadItem<SimplePayload> item = new PayloadItem<SimplePayload>(null, payload);

			// Publish item
			myNode.publish(item);
			mView.loadUrl("javascript:navigator.xmppClient._didPublish()");
		} catch (XMPPException e) {
			// Handle Exception
		}
	}
	
	public void subscribe(String resource, String node, final String event_key)
	{
		if (mPubSubMan == null)
			mPubSubMan = new PubSubManager(mConn, resource);
		try {
			Node eventNode = mPubSubMan.getNode(node);
			if(eventNode != null)
			{	
				eventNode.addItemEventListener(new ItemEventListener() {
					public void handlePublishedItems(ItemPublishEvent items) {
						List<PayloadItem> payloads = items.getItems();
						for(PayloadItem item : payloads)
						{
							String payloadXml = URLEncoder.encode(item.toXML());
							mView.loadUrl("javascript:navigator.xmppClient._xmppRecvSub('" + payloadXml + "'," + event_key + ")");
						}
						
					}
				});
				// My JID

				eventNode.subscribe(mJid);
				
				mView.loadUrl("javascript:navigator.xmppClient._xmppSubWin()");
			}
		} catch (XMPPException e) {
			mView.loadUrl("javascript:navigator.xmppClient._didReceiveError()");
		}
		
	}
	
	public void unsubscribe(String resource, String node)
	{
		if (mPubSubMan == null)
			mPubSubMan = new PubSubManager(mConn, resource);
		try {
			Node eventNode = mPubSubMan.getNode(node);
			eventNode.unsubscribe(mJid);
		}
		catch (XMPPException e)
		{
			//TODO: Throw an exception somewhere?
		}
	}
	
	/*
	 * File Transfer
	 */
	
	public void sendFile(String file, String jid, String message)
	{
		if (mFileMan == null)
		 mFileMan = new FileTransferManager(mConn);
		FileTransferNegotiator.setServiceEnabled(mConn, true);
		
		OutgoingFileTransfer trans = mFileMan.createOutgoingFileTransfer(jid);
		try {
			File source = new File(file);
			if (source.exists())
				trans.sendFile(source, message);
			else
			{
				//TODO: Add FileNotFound Exception throwing back to WebView
			}
			
		} catch (XMPPException e) {			
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	public void enableTransfer(final String directory, final boolean prompt)
	{
				
		// Create the listener
		mFileListener = new GapTransferListener(mConn, mView, prompt, directory);
		
        if (mFileMan == null)
    	{
    		 mFileMan = new FileTransferManager(mConn);
    		 mFileMan.addFileTransferListener(mFileListener);
    	}
	}
	
	public void confirmTransfer(boolean accept)
	{
		if(mFileListener != null)
		{
			if (accept)
			{
				try {
					String path = mFileListener.receiveFile();
					mView.loadUrl("javascript:document.xmppClient._didReceiveFile('" + path + "')");
				} catch (XMPPException e) {
					mView.loadUrl("javascript:document.xmppClient._fileError()");
				}
			}
			else
			{
				mFileListener.rejectFile();
			}
		}
	}
	
	/*
	 * Roster Code
	 */
	
	public void getRoster()
	{
		if (mRoster != null)
		{
			Collection<RosterEntry> entries = mRoster.getEntries();
			for(RosterEntry entry: entries){
				//Access the WebView and pass the entries back to the Javascript
				//Most likely to the EventBroadcaster
				String name = entry.getName();
				String user = entry.getUser();
				ItemStatus entry_status = entry.getStatus();
				String status = "unknown";
				if(entry_status != null)
					status = entry_status.toString();
				mView.loadUrl("javascript:navigator.xmppClient._addToRoster('" + name + "','"+ user + "','" + status + "');");
			}
			mView.loadUrl("javascript:navigator.xmppClient._xmppClientDidUpdateRoster()");
		}
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
	}
	
	/*
	 * Conversation setup!
	 * 
	 * Messages should be moved here once we get this working properly
	 * 
	 */
	
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
	
	/*
	 * This is the code that handles HTML messages
	 */
	
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

	public void sendHtmlMessage(String person, String htmlMessage, String plaintext)
	{
		Chat chat = openChat.get(person);
		if(chat == null)
			chat = setupChat(person);
		Message msg = new Message();
		msg.setBody(plaintext);

		XHTMLManager.addBody(msg, htmlMessage);
		try {
			chat.sendMessage(msg);
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
	}
	
	/*
	 * Have no idea how to test this!!!
	 * 
	 */
	
	public void sendFormMessage(String person, String data) 
	{
		Chat chat = openChat.get(person);
		if(chat == null)
			chat = setupChat(person);
		Message msg = new Message();
		
		String xmlData = URLDecoder.decode(data);
		
		XmppFormHandler xmlHandler = new XmppFormHandler();
		try {
			//We have to explicitly use SAX because Xml.parse sucks and breaks!
			/*
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader reader = sp.getXMLReader();
		    reader.setContentHandler(xmlHandler);
		    reader.parse(new InputSource(new ByteArrayInputStream(xmlData.getBytes())));
		    */
			Xml.parse(new ByteArrayInputStream(xmlData.getBytes()), Xml.Encoding.UTF_8, xmlHandler);
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DataForm output = xmlHandler.myForm.getDataFormToSend();
		
		msg.addExtension(output);
		
		try {
			chat.sendMessage(msg);
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
	}
	

	/*
	 * This discovers services that are available and sends them to the Javascript.
	 * 
	 */
	
	
	public void discoverServices(String resource)
	{
		boolean fail = false;
		if(discoStu == null)
		{
			//Disco Stu is coming back baby!!!!
			discoStu = new ServiceDiscoveryManager(mConn);
		}
		DiscoverItems discoItems;
		try {
			discoItems = discoStu.discoverItems(resource);
			Iterator it = discoItems.getItems();
			
			while(it.hasNext())
			{
				DiscoverItems.Item item = (DiscoverItems.Item) it.next();
				String params = item.getEntityID() + "','" + item.getNode() + "','" + item.getName();
				// Take this data, and send it to a collector
				mView.loadUrl("javascript:navigator.xmppClient._xmppServiceFound('" + params + "');");
			}
			mView.loadUrl("javascript:navigator.xmppClient._xmppDiscoveryWin()");
		} catch (XMPPException e) {
			// Discovery failed, call JS Fail:
			mView.loadUrl("javascript:navigator.xmppClient._xmppDiscoveryFail(" + e.getMessage() + ")");
			fail = true;
		}

		
	}
	
	/* 
	 * This was grabbed from the Ignite Realtime Board, and is the META-INF file that is  
	 * ignored by Android.  That's why this is here.
	 */
	private void configure(ProviderManager pm) {
		 
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
    
        // PubSub
        pm.addIQProvider(
                "query", "http://jabber.org/protocol/disco#items",
                 new org.jivesoftware.smackx.provider.DiscoverItemsProvider()
            );
            
            pm.addIQProvider("query",
                    "http://jabber.org/protocol/disco#info",
                    new org.jivesoftware.smackx.provider.DiscoverInfoProvider());
            
            pm.addIQProvider("pubsub",
                    "http://jabber.org/protocol/pubsub",
                    new org.jivesoftware.smackx.pubsub.provider.PubSubProvider());

            pm.addExtensionProvider("subscription", PubSubNamespace.BASIC.getXmlns() , new SubscriptionProvider());
            
            pm.addExtensionProvider(
                    "create",
                    "http://jabber.org/protocol/pubsub",
                    new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
            
            pm.addExtensionProvider("items",
                    "http://jabber.org/protocol/pubsub",
                    new org.jivesoftware.smackx.pubsub.provider.ItemsProvider());
            
            pm.addExtensionProvider("item",
                    "http://jabber.org/protocol/pubsub",
                    new org.jivesoftware.smackx.pubsub.provider.ItemProvider());
            
            pm.addExtensionProvider("item", "",
                    new org.jivesoftware.smackx.pubsub.provider.ItemProvider());
            
            pm.addExtensionProvider(
                            "subscriptions",
                            "http://jabber.org/protocol/pubsub",
                            new org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider());

            pm.addExtensionProvider(
                            "subscriptions",
                            "http://jabber.org/protocol/pubsub#owner",
                            new org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider());

            pm.addExtensionProvider(
                            "affiliations",
                            "http://jabber.org/protocol/pubsub",
                            new org.jivesoftware.smackx.pubsub.provider.AffiliationsProvider());
            
            pm.addExtensionProvider(
                            "affiliation",
                            "http://jabber.org/protocol/pubsub",
                            new org.jivesoftware.smackx.pubsub.provider.AffiliationProvider());
            
            pm.addExtensionProvider("options",
                    "http://jabber.org/protocol/pubsub",
                    new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
            
            pm.addIQProvider("pubsub",
                    "http://jabber.org/protocol/pubsub#owner",
                    new org.jivesoftware.smackx.pubsub.provider.PubSubProvider());
            
            pm.addExtensionProvider("configure",
                    "http://jabber.org/protocol/pubsub#owner",
                    new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
            
            pm.addExtensionProvider("default",
                    "http://jabber.org/protocol/pubsub#owner",
                    new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());


            pm.addExtensionProvider("event",
                    "http://jabber.org/protocol/pubsub#event",
                    new org.jivesoftware.smackx.pubsub.provider.EventProvider());
            
            pm.addExtensionProvider(
                            "configuration",
                            "http://jabber.org/protocol/pubsub#event",
                            new org.jivesoftware.smackx.pubsub.provider.ConfigEventProvider());
            
            pm.addExtensionProvider(
                            "delete",
                            "http://jabber.org/protocol/pubsub#event",
                            new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
            
            pm.addExtensionProvider("options",
                    "http://jabber.org/protocol/pubsub#event",
                    new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
            
            pm.addExtensionProvider("items",
                    "http://jabber.org/protocol/pubsub#event",
                    new org.jivesoftware.smackx.pubsub.provider.ItemsProvider());
            
            pm.addExtensionProvider("item",
                    "http://jabber.org/protocol/pubsub#event",
                    new org.jivesoftware.smackx.pubsub.provider.ItemProvider());

            pm.addExtensionProvider("headers",
                    "http://jabber.org/protocol/shim",
                    new org.jivesoftware.smackx.provider.HeaderProvider());

            pm.addExtensionProvider("header",
                    "http://jabber.org/protocol/shim",
                    new org.jivesoftware.smackx.provider.HeadersProvider());
            
            
            pm.addExtensionProvider(
                            "retract",
                            "http://jabber.org/protocol/pubsub#event",
                            new org.jivesoftware.smackx.pubsub.provider.RetractEventProvider());
            
            pm.addExtensionProvider(
                            "purge",
                            "http://jabber.org/protocol/pubsub#event",
                            new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
            
         

            SmackConfiguration.setKeepAliveInterval(-1);
     
	
	}
	
}
