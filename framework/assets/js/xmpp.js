

////////////////////////////////////// XMPPMessage //////////////////////////////////////////

function XMPPMessage(id,body,senderJid,receiverJid,isread,timeStamp, html)
{
	this.id = id;
	this.body = body;
	this.senderJid = senderJid;
	this.receiverJid = receiverJid;
	this.isread = isread ? true : false;

	this.html = html ? true : false;
	this.timeStamp = (timeStamp &&  timeStamp.constructor == Date ) ? timeStamp : new Date();
}


XmppService = function(entityId, node, name)
{
  this.name = name;
  this.node = node;
  this.entityId = entityId;
}

RosterItem = function(name, user, status)
{
  this.name = name;
  this.user = user;
  this.status = status;
}



////////////////////////////////////// XMPPClient //////////////////////////////////////////


function XMPPClient()
{
	EventBroadcaster.initialize(this);
	this.roster = [];
	this.messageMap = {};
	this.unreadCount = 0;
	this.subs = [];
	this.fileListeners = [];
  	this.services = [];
  	this.server = "";
}

// username, password, domain are required
// resource and port are optional
XMPPClient.prototype.connect = function(domain, username,password,resource,port)
{
	XmppHook.connect(domain, username, password, resource, port);
}

XMPPClient.prototype.sendMessageToJID = function(jid,message)
{
  XmppHook.sendMessage(jid, message);
}

XMPPClient.prototype.sendHtmlMessageToJID = function (jid, plaintext, html)
{
  XmppHook.sendHtmlMessage(jid, html, plaintext);
}

XMPPClient.prototype.getRoster = function() {
  XmppHook.getRoster();
}

XMPPClient.prototype.publish = function(resource, name, xmlns, xmlPayload, nodeTitle, persist)
{
  XmppHook.publish(resource, name, xmlns, xmlPayload, nodeTitle, persist);
}

XMPPClient.prototype.subscribe = function(resource, node, win)
{
  var key = this.subs.push(win);
  XmppHook.subscribe(resource, node, key);
}

XMPPClient.prototype.unsubscribe = function(resource, node)
{
	XmppHook.unsubscribe(resource, node);
}

XMPPClient.prototype.discoverServices = function(resource)
{
  XmppHook.discoverServices(resource);
}

XMPPClient.prototype.sendFile = function(file, user, message)
{
  XmppHook.sendFile(file, user, message);
}

XMPPClient.prototype.enableFileTransfer = function(location, prompt)
{ 
  XmppHook.enableTransfer(location, prompt);
}

XMPPClient.prototype.confirmTransfer = function(boolean)
{
  XmppHook.confirmTransfer(boolean);
}

XMPPClient.prototype.sendForm = function(resource, form)
{
	XmppHook.sendFormMessage(resource, form);
}

XMPPClient.prototype._xmppServiceFound = function(entityId, node, name)
{
  var service = new XmppService(entityId, node, name);
  this.services.push(service);
}

XMPPClient.prototype._xmppDiscoveryWin = function()
{
  this.broadcastEvent('DiscoveryWin');
}

XMPPClient.prototype._xmppDiscoveryFail = function(err)
{
  this.broadcastEvent('DiscoveryFail');
}

XMPPClient.prototype._xmppClientConnecting = function()
{
	this.broadcastEvent("Connecting");
}

XMPPClient.prototype._xmppClientDidConnect = function(hostname)
{
  this.hostname = hostname;  
  this.broadcastEvent("ConnectSuccess");
}

XMPPClient.prototype._xmppClientDidNotConnect = function()
{
	this.broadcastEvent("ConnectFail");
}

XMPPClient.prototype._xmppClientDidDisconnect = function()
{
	this.broadcastEvent("Disconnect");
}

XMPPClient.prototype._xmppClientDidRegister = function()
{
	this.broadcastEvent("Register");
}

XMPPClient.prototype._xmppClientDidAuthenticate = function()
{
	this.broadcastEvent("AuthenticateSuccess");
}

XMPPClient.prototype._didNotAuthenticate = function(err)
{
	this.broadcastEvent("AuthenticateFail");
}

XMPPClient.prototype._addToRoster = function(name, user, status)
{
  var person = new RosterItem(name, user, status);
  this.roster.push(person);
}

XMPPClient.prototype._xmppClientDidUpdateRoster = function()
{
	this.broadcastEvent("UpdateRoster",this.roster);
}

XMPPClient.prototype._xmppDidPublish = function()
{
  this.broadcastEvent("XmppPublish");
}

XMPPClient.prototype._xmppPublishFail = function(err)
{
  this.broadcastEvent("XmppPubFail");
}


XMPPClient.prototype._didReceiveBuddyRequest = function()
{
	this.broadcastEvent("BuddyRequest");
}

XMPPClient.prototype._didReceiveIQ = function _didReceiveIQ()
{
	this.broadcastEvent("IQReceived");
}

XMPPClient.prototype._didReceiveFileRequest = function()
{
	this.broadcastEvent("XmppFileRequest");
}

XMPPClient.prototype._didReceiveFile = function(filename)
{
	this.broadcastEvent("XmppFileReceived", filename);
}

XMPPClient.prototype._fileError = function(){
	this.broadcastEvent("XmppFileError");
	this._didReceiveError();
}

// parses date from XEP-0082 
// Date string is UTC time and formatted as CCYYMMDDThh:mm:ss
// MM is 1 based month
XMPPClient.parseXMPPDate = function parseXMPPDate(dateStr)
{	
	var datePt = dateStr.split("T")[0];
	var timePt = dateStr.split("T")[1];
	
	var yr = parseInt(datePt.substr(0,4));
	var mnth = parseInt(datePt.substr(4,2))-1; // should be zero based
	var day = parseInt(datePt.substr(6,2));
	
	var time = timePt.split(":");
	var hr = parseInt(time[0]);
	var mn = parseInt(time[1]);
	var sec= parseInt(time[2]);

	return new Date(Date.UTC(yr,mnth,day,hr,mn,sec,0));
}


XMPPClient.prototype._didReceiveMessage = function(msg,senderJid,messageId,timeStamp)
{
	var senderName = senderJid.split("@")[0];
	
	if(timeStamp != null && timeStamp.length > 0)
	{
		ts = XMPPClient.parseXMPPDate(timeStamp);
	}
	else
	{
		ts = new Date();
	}

	var message = new XMPPMessage(messageId,unescape(msg),senderJid,"","",false,ts);
	
	if(this.messageMap[senderName] == null)
	{
		this.messageMap[senderName] = [];
	}
	
	this.messageMap[senderName].push(message);
	this.unreadCount++;
	
	this.broadcastEvent("MessageReceived",senderName,message);
}


XMPPClient.prototype._didReceiveHtmlMessage = function(msg, senderJid, messageId, timeStamp)
{
  var senderName = senderJid.split('@')[0];

  if(timeStamp != null && timeStamp.length > 0)
  {
    ts = XMPPClient.parseXMPPDate(timeStamp);
  }
  else
  {
    ts = new Date();
  }

  var proper_html = unescape(msg).replace("+", " ");
  var message = new XMPPMessage(messageId,proper_html,senderJid,"","",false,ts, true);

  if(this.messageMap[senderName] == null)
  {
    this.messageMap[senderName] = [];
  }

  this.messageMap[senderName].push(message);
  this.unreadCount++;

  this.broadcastEvent("HtmlMessageReceived", senderName, message);

}


XMPPClient.prototype._didReceiveError = function(err)
{
	this.broadcastEvent("Error");
}

XMPPClient.prototype._didPublish = function()
{
	this.broadcastEvent("DidPublish");
}

XMPPClient.prototype._xmppRecvSub = function(xml, key)
{
	this.subs[key](xml);
}

XMPPClient.prototype._xmppSubWin = function(key)
{
	this.broadcastEvent("XmppSubWin");
}

XMPPClient.prototype._xmppHasForm = function(xml)
{
	this.broadcastEvent("XmppHasForm", xml);
}


XMPPClient._install = function()
{
	if(typeof navigator.xmppClient == "undefined")
		navigator.xmppClient = new XMPPClient();
}


PhoneGap.addConstructor(XMPPClient._install);




