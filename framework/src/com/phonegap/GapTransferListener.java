package com.phonegap;

import java.io.File;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferNegotiator;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;

import android.webkit.WebView;

public class GapTransferListener implements FileTransferListener {

	FileTransferRequest mRequest;
	boolean mPrompt;
	XMPPConnection mConn;
	String mDirectory;
	WebView mView;
	
	GapTransferListener(XMPPConnection conn, WebView view, boolean prompt, String directory)
	{
		mPrompt = prompt;
		mDirectory = directory;
		mConn = conn;
		mView = view;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.jivesoftware.smackx.filetransfer.FileTransferListener#fileTransferRequest(org.jivesoftware.smackx.filetransfer.FileTransferRequest)
	 * 
	 * I'm aware that I repeat myself here, but we may not want to fire the same events for non-prompted FileTrasfers, so we'll just leave it
	 * open for now.
	 * 
	 */
	
    public void fileTransferRequest(FileTransferRequest request) {
    	FileTransferNegotiator.setServiceEnabled(mConn, true);
        // Check to see if the request should be accepted
  	  	mRequest = request;
        if(mPrompt)
        {
        	mView.loadUrl("javascript:navigator.xmppClient._didReceiveFileRequest()");
        }
        else
        {
      	  try {
			String path = this.receiveFile();
			mView.loadUrl("javascript:navigator.xmppClient._didReceiveFile('" + path + "')");
      	  } catch (XMPPException e) {
			mView.loadUrl("javascript:navigator.xmppClient._fileError()");
      	  }
        }
    }
    
    public String receiveFile() throws XMPPException 
    {
        IncomingFileTransfer transfer = mRequest.accept();
        String path = mDirectory + mRequest.getFileName();
		transfer.recieveFile(new File(path));
		return path;
    }
    
    public void rejectFile()
    {
    	mRequest.reject();
    }
	
}
