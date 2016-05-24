package telegram;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import utils.MultipartUtility;


public class Chat
{
   private String token;			// Token of the Bot
   public String name;              // Title of this chat
   public long id;                   // ID of this chat
   public List<Message> messages;   // List of messages received in this chat
   public List<User> users;         // List of users currently members of this chat
   public Type type;                // Type of this chat (private, group, supergroup)
   private TeleBot bot;			    // Reference to the bot (to use the database)
	  
   public boolean addUser(User user)
   {
	   for( User usr : users )
		   if( usr.id == user.id )
			   return false;
	   users.add(user);
	   // Update the database
	   try
	   {
		   Statement stmt = bot.sqlc.createStatement();
		   String sql = "INSERT INTO ChatUsers (UserID,ChatID) VALUES " +
				        "(" + user.id + "," + id +")";
		   stmt.executeUpdate(sql);
		   stmt.close();
	   }
	   catch( SQLException e )
	   {
		   e.printStackTrace();
	   }
	   
	   return true;
   }
   public void removeUser(User leftMember)
   {
	   if(users.remove(leftMember))
	   {
		   try
		   {
			   Statement stmt = bot.sqlc.createStatement();
			   String sql = "DELETE FROM ChatUsers WHERE UserID = " +
			                Long.toString(leftMember.id) + "AND ChatID =" + Long.toString(id) + ";";
			   stmt.executeUpdate(sql);
			   stmt.close();
		   }
		   catch( SQLException e )
		   {
			   e.printStackTrace();
		   }
	   }
   }

   public Chat(TeleBot bot)
   {
	  this.bot = bot;
      token = bot.token;
      messages = new ArrayList<Message>();
      users    = new ArrayList<User>();
   }
   
   public enum Type {Private, Group, Supergroup};
   
   // Chat Action **********************************************************************************
   // This controls the action text displayed in the chat bar for the bot (i.e. "writing...") ******
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
   private ActionThread at = null;
   // Set a chat action for the Bot.
   // action: the action
   // [keepAlive]: if set to true the action will persist untill resetChatAction is called 
   public void setChatAction(Action action)
   {
	   setChatAction(action, false);
   }
   public void setChatAction(Action action, boolean keepAlive)
   {
      if(at != null)
         resetChatAction();
      at = new ActionThread(id, action, token, keepAlive);
      at.start();
   }
   public void resetChatAction()
   {
      if(at == null)
         return;
      at.end();
      at = null;
   }
   
   // Send methods *********************************************************************************
   // These methods can be used to interact inside this chat ***************************************
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
   public void addMessage(User from, JsonObject message) 
   {
	   Message msg = new Message();
	   msg.content = message.getString("text");
	   msg.sender  = from;
	   messages.add(msg);
	   try
	   {
		   Statement stmt = bot.sqlc.createStatement();
		   String sql = "INSERT INTO Messages ( Text,ChatID,UserID,Date ) VALUES ( '" +
				   		msg.content + "'," + Long.toString(id) + "," + Long.toString(msg.sender.id)
				   		+ "," + message.getInt("date") + ");";
		   stmt.executeUpdate(sql);
		   stmt.close();
	   }
	   catch( SQLException e )
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
   private boolean keepAlive;
   
   public ActionThread(long chatId, Chat.Action action, String token, boolean keepAlive)
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
      
      this.keepAlive = keepAlive;
   }   
   public void run()
   {
      while(alive)
      {  
         // Send action to the chat
         // ****************************************************************************************
         try {
            URL url = new URL(urlStr);
            url.openStream();
         } catch (IOException e) {
            e.printStackTrace();
         }
         if( !keepAlive )
        	 break; // break out of this while to end the thread
         // Sleep 3 seconds
         // ****************************************************************************************
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