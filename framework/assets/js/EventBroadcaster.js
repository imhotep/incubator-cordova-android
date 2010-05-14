

function EventBroadcaster()
{
	this._eventMap = {};
}

// Adds a listener to the queue, 
EventBroadcaster.prototype.addListener = function( eventName, responder, callback, params)
{
	// eventnames are stored case insensitively
	var evtName = eventName.toLowerCase(); 
	if(this._eventMap[evtName] == null)
	{
		this._eventMap[evtName] = [];
	}
	
	var evtObj = {responder:responder,callback:callback};
	if(params)
	{
		evtObj.params = params;
	}
	
	this._eventMap[evtName].push(evtObj);
}

EventBroadcaster.prototype.broadcastEvent = function(eventName)
{
	var evtName = arguments[0];
	
	var evtArgs = [];
	for(var n = 1; n < arguments.length; n++)
	{
		evtArgs.push(arguments[n]);
	}
	
	var eventObj = {name:evtName,target:this,args:evtArgs};
	
	var listeners = this._eventMap[evtName.toLowerCase()];
	if(listeners == null)
		return;
	
	// iterate in reverse order
	for(var v = listeners.length; v > 0; v--)
	{
		var lob = listeners[v - 1];
		// if we don't have a scope to callback from ... just callback
		if(lob.responder == undefined)
		{
			lob.callback(eventObj,lob.params);
		}
		else // responder is valid
		{
			// is callback defined ?
			if(lob.callback != undefined)
			{
				if(typeof lob.callback == "function")
				{
					lob.callback.apply(lob.responder,[eventObj,lob.params]);
				}
				else if(typeof lob.responder[lob.callback] == "function")
				{
					lob.responder[lob.callback].apply(lob.responder,[eventObj,lob.params]);
				}
				else
				{
					// wtf now ?
				}
			}
			else // responder is valid, but listener was not supplied ...
			{
				// search for a method, of the pattern responder.onEventName
				// note we use the Mixed case version of EventName
				if(typeof lob.responder["on" + evtName] == "function")
				{
					lob.responder["on" + evtName].apply(lob.responder,[eventObj,lob.params]);
				}
			}
		}
	}
}

EventBroadcaster.prototype.hasListener = function(eventName, responder)
{
	var evtName = eventName.toLowerCase();
	if(this._eventMap[evtName] == null || this._eventMap[evtName].length < 1 )
	{
		return false;
	}
	return true;
}

EventBroadcaster.prototype.removeListener = function(eventName,responder)
{
	var evtName = eventName.toLowerCase();
	var responders = this._eventMap[evtName];
	if(responders != null)
	{
		for(var n = responders.length; n > 0; n--)
		{
			if(responders[n-1].responder == responder)
			{
				return responders.splice(n-1,1);
			}
		}
	}
	return null;
}

EventBroadcaster.prototype.willTrigger = function(eventName)
{
	return this._eventMap[eventName.toLowerCase()] != null;
}


// Static method for attaching this functionality to another object
// install EB Methods on an instance:
// var myEB = {}; EventBroadcaster.initialize(myEB);
EventBroadcaster.initialize = function(targ)
{
	targ._eventMap = {};
	targ.addListener    = this.prototype.addListener;
	targ.broadcastEvent = this.prototype.broadcastEvent;
	targ.hasListener    = this.prototype.hasListener;
	targ.removeListener = this.prototype.removeListener;
	targ.willTrigger    = this.prototype.willTrigger;
}



