package telegram;

//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
//import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;


public class TeleBot 
{
   // Public stuff
   // **********************************************************************************************
   public enum UpdatesMode 
   { 
	   LONG_POLLING,    // Automatically retrieve updates from telegram server
	   LOCAL_POLLING,   // Automatically detect updates from a local file
	   MANUAL           // Send updates to the bot by using the method sendUpdates
   };
   public List<Command> commands;
   public TeleBot(String token)
   {
	   this(token, UpdatesMode.LONG_POLLING);
   }
   public TeleBot(String token, UpdatesMode mode)
   {
      // Initialize variables
      offset          =  0;                         // Offset and lastTime are used to ignore
      lastTime        =  0;                         // old messages.
      this.token      = token;                      // Token of this bot
      chats           = new ArrayList<Chat>();      // List of the chats
      runnable        = new BotThread(this);        // Create a thread for this bot
      commands        = new ArrayList<Command>();   // List of the avaialbe commands
      newMessages     = new ArrayList<Message>();   // List of the new messages to process
      users           = new ArrayList<User>();      // List of the known users
      updatesMode     = mode;						// Updates mode(longpolling,localpollign,manual)
      suppressInfoMsg = false;                      // Send info messages to system.io
      me              = new User();
      
      // Retrieve informations about this bot
      try 
 	  {
 		 String url = "https://api.telegram.org/bot" + token + "/getMe";
 		 rdr = Json.createReader( new URL(url).openStream() );
		 JsonObject meObj = rdr.readObject().getJsonObject("result");
		 
		 me.id        = meObj.getInt("id");
		 if( meObj.containsKey("first_name") )
			me.firstName = meObj.getString("first_name");
		 if( meObj.containsKey("last_name") )
	        me.lastName  = meObj.getString("last_name");
		 if( meObj.containsKey("username") )
		    me.username  = meObj.getString("username");
		 
		 rdr.close();
		 
		 if( !suppressInfoMsg )
		    System.out.println("username: " + me.username + "\nid: " + me.id);
	  } 
 	  catch (Exception e)
 	  {
 	     System.err.println("Error during bot thread initialization:");
	     System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	     System.exit(1);
	  }
      runnable.start();                          // Start the thread for this bot
   }
   
   public void setDefaultAction(Command cmd)
   {
      this.defaultAction = cmd;
   }
      
   // Private stuff (mind your own business)
   // **********************************************************************************************
   private UpdatesMode updatesMode;
   private Command defaultAction = null;
   private JsonReader rdr;
   // Define a thread for the Bot
   private class BotThread implements Runnable
   {
      private TeleBot bot;
      public BotThread( TeleBot bot )
      {
         this.bot = bot;
      }
      public void run()
      {
    	 if( !suppressInfoMsg )
    	 System.out.println("Starting Telegram Bot " + bot.token);
    	 
    	 
    	 if( updatesMode == UpdatesMode.MANUAL )
    		 // No need for a thread seeking updates 
    		 return;
    	 
    	 // Thread seeking updates
         while(true)
         {  
        	try
        	{
        		bot.cycle();
        		if( bot.updatesMode == UpdatesMode.LOCAL_POLLING)
        			Thread.sleep(1000);
        		else
        			Thread.sleep(5000);
        	}
        	catch( Exception e )
        	{
        		System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        	}
         }
      }
      public void start()
      {
         Thread t = new Thread(this, "TelegramBot");
         t.start();
      }
   }
   
   private BotThread runnable;
   private List<User> users;
   private String token;
   private int lastTime;
   private int offset;
   private List<Message> newMessages;
   private List<Chat> chats;
   private User me;
   private boolean suppressInfoMsg;
   
   // Converts the JsonObject to a User. If the user is not present, add it to users and return it
   // otherwise return the existing User
   private User addUser(JsonObject user)
   {
	   User newUser = new User();
	   newUser.id        = user.getInt("id");
	   newUser.username  = user.getString("username");
	   newUser.firstName = user.getString("first_name");
	   newUser.lastName  = user.getString("last_name");
	   // If the user is this Bot, do nothing
	   if( newUser.id == me.id )
		   return me;
	   // If the user already exists, do nothing
	   for( User usr : users )
		   if( usr.id == user.getInt("id") )
			   return usr;
	   // The user is new, add it to users and return the new user
	   users.add( newUser );
	   return newUser;
   }
   // Retrieves the user with the specified id. If no user exists, return null.
   private User findUserByID(int id)
   {
	   for( User usr : users )
		   if( usr.id == id )
			   return usr;
	   return null;
   }
   // Converts the JsonObject to a Chat. If the chat does not exist, this adds it to the chats and
   // returns it it, otherwise this returns the existing chat.
   private Chat addChat(JsonObject chat)
   {
	   Chat newChat = new Chat(token);
	   newChat.id = chat.getInt("id");
	   String type = chat.getString("type");
	   if( type.equals("private") )
	   {
		   newChat.type = Chat.Type.Private;
		   newChat.name = chat.getString("username");
	   }
	   else if( type.equals("group") )
	   {
		   newChat.type = Chat.Type.Group;
		   newChat.name = chat.getString("title");
	   }
	   else if( type.equals("supergroup") )
	   {
		   newChat.type = Chat.Type.Supergroup;
		   newChat.name = chat.getString("title");
	   }
	   for( Chat ch : chats )
		   if( ch.id == newChat.id )
			   return ch;
	   chats.add(newChat);
	   return newChat;
   }
   private void removeChat(Chat chat)
   {
	   chats.remove(chat);
   }

   // Use this method to send new updates to the bot. Updates must be in a json-formatted string
   public void sendUpdates( String jsonUpdate )
   {
	   // Alert output
	   if( !suppressInfoMsg )
		   System.out.println("Updates received: '" + jsonUpdate + "'");
	   
	   // Get the message
	   JsonReader rdr = Json.createReader( new ByteArrayInputStream(jsonUpdate.getBytes()) );
	   JsonObject update = rdr.readObject();
	   if( !update.containsKey("message") )
		   return;
	   
	   JsonObject message = update.getJsonObject("message");
	   
	   Chat chat = addChat(message.getJsonObject("chat"));
	   
	   User from = null;
	   if( message.containsKey("from"))
	   {
		   from = addUser(message.getJsonObject("from"));
		   chat.addUser(from);
	   }
	   
	   // new_chat_member
	   if( message.containsKey("new_chat_member") )
	   {
		   User newComer = addUser(message.getJsonObject("new_chat_member"));
		   chat.addUser( newComer );
		   return;
	   }
	   // left_chat_member
	   else if( message.containsKey("left_chat_member") )
	   {
		   User leftMember = addUser(message.getJsonObject("left_chat_member"));
		   if( leftMember.id == me.id )
			   removeChat(chat);
		   else
			   chat.removeUser(leftMember);
	   }
	   // simple text message
	   else if( message.containsKey("text") )
	   {
		   chat.addMessage(from, message);
		   // If this messages starts with '/', a command was called
		   String text = message.getString("text");
		   if( text.startsWith("/") )
		   {
			   // Separate command and arguments
			   text = text.substring(1);
			   String cmd = "", args = "";
			   int p = 0;
			   while( Character.isLetterOrDigit(text.charAt(p)) )
			   {
				   cmd += text.charAt(p);
				   p++;
			   }
			   args = text.substring(p+1);
			   
			   if( !suppressInfoMsg )
				   System.out.println("Command received: '"+cmd+"':'"+args+"'");
		   }
	   }
   }
   
   // Main thread cycle seeking updates
   private void cycle() throws IOException
   { 
     switch( updatesMode )
     {
	    case LOCAL_POLLING:
	    	// Updates are stored in a local file called updates
	    	// The contents is like '{jsonUpdate0}{jsonUpdate1}...'
	    	File file = new File("updates");
	    	if( !file.exists() )
	    		break;
	    	
	    	byte[] encoded = Files.readAllBytes(Paths.get("updates"));
	    	if( encoded.length <= 0 )
	    		break;
	    	String updates = new String(encoded, Charset.defaultCharset());
	    	file.delete();
	    	
	    	// Extract jsonUpdates (extract strings inside '{' and '}', and send via sendUpdates)
	    	int i = 0, f = 0, p = 0;
	    	int parenthesisCount = 0;
	    	while( true )
	    	{
	    		if( p >= updates.length() )
	    			break;
	    		if( updates.charAt(p) == '{' )
	    		{
	    			if( parenthesisCount == 0 )
	    				i = p;
	    			parenthesisCount++;
	    		}
	    		else if( updates.charAt(p) == '}' )
	    			parenthesisCount--;
	    		if( parenthesisCount == 0 )
	    		{
	    			// Extract jsonUpdate
	    			String jsonUpdate = updates.substring(i+1, p);
	    			sendUpdates(jsonUpdate);
	    			updates = updates.substring(p+1);
	    			p = -1;
	    		}
	    		p++;
	    	}
	        break;
	        
	    case LONG_POLLING:
	    	// TODO: debug this
	    	URL url = new URL(
	           offset != -1 ?
	           ("https://api.telegram.org/bot" + token + "/getupdates?offset=" + offset)
	           :
	           ("https://api.telegram.org/bot" + token + "/getupdates")
	        );
	    	InputStream is = url.openStream();
	    	byte b[] = new byte[5000];
	    	is.read(b); // TODO: what if read data size exceeds 5KB?
	    	sendUpdates(b.toString());
	    break;
	    default:
	    break;
     }
   }
}