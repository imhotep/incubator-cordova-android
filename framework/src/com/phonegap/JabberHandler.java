package com.phonegap;

import java.util.HashMap;

import android.webkit.WebView;

public class JabberHandler {

	private HashMap<String, ChatHandler> connectionMap;
	WebView mView;
	
	JabberHandler(WebView view)
	{
		mView = view;
	}
	
	public String createConnection(String key, String uri)
	{
		ChatHandler chat = new ChatHandler(uri, mView);
		connectionMap.put(key, chat);
		return key;
	}
	
	
}
