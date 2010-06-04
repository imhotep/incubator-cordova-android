package com.phonegap;

import java.util.Vector;

import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class XmppFormHandler extends DefaultHandler {

	Form myForm;
	FormField currentField = null;
	String tagState;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes)
	{
		// Set the tag state so that we can grab it.
		if(localName.equals("x"))
		{
			String type = attributes.getValue(0);
			if(type != null)
				myForm = new Form(type);
		}
		if(localName.equals("title"))			
		{
			tagState = "title";
		}
		else if(localName.equals("instructions"))
		{
			tagState = "instructions";
		}
		
		processField(localName, attributes);
		
	}
	
	private void processField(String localName, Attributes attributes) {
		//This is for the field parameters!
		if(localName.equals("field"))
		{
			//Add the last field, we're on a new one
			if (currentField != null && myForm != null)
				myForm.addField(currentField);
			currentField = new FormField();
		}
		else if(localName.equals("required"))
		{
			if(currentField != null)
				currentField.setRequired(true);
		}
		else if(localName.equals("value"))
		{
			tagState = "value";
		}
		else if(localName.equals("option"))
		{
			tagState = "option";
		}
		else if(localName.equals("desc"))
		{
			tagState = "desc";
		}		
	}

	public void characters (char ch[], int start, int length)
	{
		String data = "";
		for (int i = start; i < start + length; i++) {
			data += ch[i];
		}
		if(tagState != null)
		{
			if (tagState.equals("value"))
			{
				currentField.addValue(data);
			}
			else if(tagState.equals("option"))
			{
				FormField.Option myOption = new FormField.Option(data);
				currentField.addOption(myOption);				
			}
			else if(tagState.equals("desc"))
			{
				currentField.setDescription(data);
			}
			else if(tagState.equals("title"))
			{
				myForm.setTitle(data);
			}
			else if(tagState.equals("instructions"))
			{
				myForm.setInstructions(data);
			}
		}
	}
	
}
