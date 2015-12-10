package telegram;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import utils.MultipartUtility;

// TODO keep correctly updated List<User> users and type.
// to do this, you will have to listen for special messages 

public class Chat
{
	public Chat(int id, String tok)
	{
		this.id = id;
		this.token = tok;
		messages = new ArrayList<Message>();
	}
	private String token;
	public String name; 
	public int id;
	public List<Message> messages;
	public List<User> users;
	
	public enum Action {
		Typing,
		UploadingPhoto,
		RecordingVideo,
		UploadingVideo,
		RecordingAudio,
		UploadingAudio,
		UploadingDocument,
		FindingLocation
	};
	public enum Type {Single, Group};
	public Type type;
	
	private ActionThread at = null;
	// Method names are pretty much self explanatory
	public void setChatAction(Action action)
	{
		if(at != null)
			resetChatAction();
		at = new ActionThread(id, action, token);
		at.start();
	}
	public void resetChatAction()
	{
		if(at == null)
			return;
		at.end();
		at = null;
	}
	public void sendMessage(String message)
	{
		sendMessage(message,false);
	}
	// Markdown enables font styles
	public void sendMessage(String message, boolean markdown)
	{
		try
		{
			String url = "https://api.telegram.org/bot" 
				+ token + "/sendMessage?chat_id="
				+ id + "&text=" + URLEncoder.encode(message,"UTF-8")
				+ (markdown ? "&parse_mode=markdown" : "");
			sendHTTPRequest(url);
		}
		catch(UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
	}
	public void sendSticker(String sticker_id)
	{
		String url = "https://api.telegram.org/bot" 
				+ token + "/sendSticker?chat_id=" + id
				+ "&sticker=" + sticker_id;
		sendHTTPRequest(url);
	}
	// TODO let sendSticker return sticker_id
	public void sendSticker(File sticker)
	{
		String charset = "UTF-8";
		File uploadFile = sticker;
		String chatID = String.valueOf(id);
		String requestURL = "https://api.telegram.org/bot"
				+ token + "/sendSticker?chat_id=" + chatID;
		MultipartUtility multipart;
		try {
			multipart = new MultipartUtility(requestURL, charset);
			multipart.addFilePart("sticker", uploadFile);
			multipart.finish();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	// TODO let sendPhoto return photo_id
	public void sendPhoto(File photo)	
	{
		String charset = "UTF-8";
		File uploadFile = photo;
		String chatID = String.valueOf(id);
		String requestURL = "https://api.telegram.org/bot" + token + "/sendPhoto?chat_id=" + chatID;
		MultipartUtility multipart;
		try {
			multipart = new MultipartUtility(requestURL, charset);
			multipart.addFilePart("photo", uploadFile);
			multipart.finish();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void SendPhoto(String photo_id)
	{
		String url = "https://api.telegram.org/bot" 
				+ token + "/sendSticker?chat_id=" + id
				+ "&photo=" + photo_id;
		sendHTTPRequest(url);
	}
	// TODO add the other methods
	
	private void sendHTTPRequest(String url)
	{
		try
		{
			(new URL(url)).openStream();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}

// In telegram you can't just set a state (writing, sending photo...) and reset
// it when you wish, you have to send multiple request in a 'stay alive' manner.
// The natural way to implement this is by using a thread.
class ActionThread implements Runnable
{
	private Thread t;
	private boolean alive = true;
	private String urlStr;
	
	public ActionThread(int chatId, Chat.Action action, String token)
	{
		String actionStr = "";
		switch(action)
		{
		case Typing:
			actionStr = "typing";
			break;
		case UploadingPhoto:
			actionStr = "upload_photo";
			break;
		case RecordingAudio:
			actionStr = "record_audio";
			break;
		case UploadingAudio:
			actionStr = "upload_audio";
			break;
		case RecordingVideo:
			actionStr = "record_video";
			break;
		case UploadingVideo:
			actionStr = "upload_video";
			break;
		case UploadingDocument:
			actionStr = "upload_document";
			break;
		case FindingLocation:
			actionStr = "find_location";
			break;
		default:
			alive = false;
			return;
		}
		
		urlStr = "https://api.telegram.org/bot" 
				+ token + "/sendChatAction?chat_id=" + chatId
				+ "&action=" + actionStr;
	}	
	public void run()
	{
		while(alive)
		{  
			// Send action to the chat
			// *************************************************************************************
			try {
				URL url = new URL(urlStr);
				url.openStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Sleep 3 seconds
			// *************************************************************************************
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}	
	public void start()
	{
		t = new Thread(this);
		t.start();
	}
	public void end()
	{
		alive = false;
	}
}