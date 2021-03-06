package telegram;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;


import java.sql.*;

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
      this.token      = token;                      // Token of this bot
      chats           = new ArrayList<Chat>();      // List of the chats
      runnable        = new BotThread(this);        // Create a thread for this bot
      commands        = new ArrayList<Command>();   // List of the avaialbe commands
      users           = new ArrayList<User>();      // List of the known users
      updatesMode     = mode;						// Updates mode(longpolling,localpollign,manual)
      suppressInfoMsg = false;                      // Send info messages to system.io
      me              = new User();
      
      
      try 
 	  {
    	 // Retrieve informations about this bot ---------------------------------------------------
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
		 
		 // Sqlite initialization ------------------------------------------------------------------
		 File file = new File("telebot.db");
		 if( !file.exists() )
		 {
			 // Create the database
			 sqlc = DriverManager.getConnection("jdbc:sqlite:telebot.db");
			 Statement stmt = sqlc.createStatement();
			 String sql = " CREATE TABLE Users ( " +
                          " ID INTEGER PRIMARY KEY NOT NULL, " +
                          " FirstName TEXT, LastName TEXT, UserName TEXT) ";
			 stmt.executeUpdate(sql);
			 sql =        " CREATE TABLE Chats ( " +
		                  " ID INTEGER PRIMARY KEY NOT NULL, " +
					      " Name TEXT, Type INT) ";
			 stmt.executeUpdate(sql);
			 sql =        " CREATE TABLE Messages " +
			              " ( Text TEXT, ChatID INTEGER, UserID INTEGER, Date INTEGER ) ";
			 stmt.executeUpdate(sql);
			 sql =        " CREATE TABLE OldMessages " +
			              " ( Text TEXT, ChatID INTEGER, UserID INTEGER, Date INTEGER ) ";
			 stmt.executeUpdate(sql);
			 sql =        " CREATE TABLE ChatUsers " +
			              " ( UserID INTEGER, ChatID INTEGER, PRIMARY KEY (UserID, ChatID) ) ";
			 stmt.executeUpdate(sql);
			 stmt.close();
		 }
		 else
		 {
			 // Load data from the database
			 sqlc = DriverManager.getConnection("jdbc:sqlite:telebot.db");
			 
			 // Load users
			 Statement stmt = sqlc.createStatement();
			 ResultSet rs = stmt.executeQuery("SELECT * FROM Users");
			 while( rs.next() )
			 {
				 User newUser = new User();
				 newUser.id = rs.getLong("ID");
				 newUser.firstName = rs.getString("FirstName");
				 newUser. lastName = rs.getString("LastName");
				 newUser. username = rs.getString("username");
				 users.add(newUser);
			 }
			 rs.close();
			 // Load chats
			 rs = stmt.executeQuery("SELECT * FROM Chats");
			 while( rs.next() )
			 {
				 Chat newChat = new Chat(this);
				 newChat.id = rs.getLong("ID");
				 newChat.name = rs.getString("Name");
				 int type = rs.getInt("Type");
				 newChat.type = ( type == 0 ? Chat.Type.Private : 
					            ( type == 1 ? Chat.Type.Group : Chat.Type.Supergroup ));
				 chats.add(newChat);
			 }
			 rs.close();
			 
			 // Load chats users and messages
			 for( Chat c : chats )
			 {
				 String chatID = Long.toString(c.id);
				 rs = stmt.executeQuery("SELECT * FROM ChatUsers WHERE ChatID = " + chatID + ";");
				 while( rs.next() )
				    c.addUser( findUserByID(rs.getLong("UserID")) );
				 rs.close();
				 rs = stmt.executeQuery("SELECT * FROM Messages WHERE ChatID = " + chatID + ";");
				 while( rs.next() )
				 {
					 Message newMessage = new Message();
					 newMessage.sender  = findUserByID(rs.getLong("UserID"));
					 newMessage.content = rs.getString("Text");
					 c.messages.add(newMessage);
				 } 
			 }
		 }
	  } 
 	  catch (Exception e)
 	  {
 	     System.err.println("Error during bot thread initialization:");
	     System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	     System.exit(1);
	  }
      
      // Start the thread for this bot
      runnable.start();
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
   public String token;
   private int offset;
   private List<Chat> chats;
   private User me;
   private boolean suppressInfoMsg;
   public Connection sqlc;
   
   // Converts the JsonObject to a User. If the user is not present, add it to users and return it
   // otherwise return the existing User
   private User addUser(JsonObject user)
   {
	   User newUser = new User();
	   newUser.id = Long.parseLong(user.getJsonNumber("id").toString());//user.getInt("id");
	   if( user.containsKey("username") )
	      newUser.username  = user.getString("username");
	   if( user.containsKey("first_name") )
	      newUser.firstName = user.getString("first_name");
	   if( user.containsKey("last_name") )
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
	   // Add the user to the database
	   String sql = "";
	   try {
		Statement stmt = sqlc.createStatement();
		sql = "INSERT INTO Users (ID,FirstName,LastName,UserName) VALUES (" +
		             newUser.id + "," + 
		             ((newUser.firstName == null) ? "NULL ," : ("'" + newUser.firstName + "',")) +
		             ((newUser. lastName == null) ? "NULL ," : ("'" + newUser. lastName + "',")) +
		             ((newUser. username == null) ? "NULL);" : ("'" + newUser. username + "');"));
		stmt.executeUpdate(sql);
		stmt.close();
	} catch (SQLException e) {
		System.err.println("Sql query failed: " + sql);
		e.printStackTrace();
	}
	   if( !suppressInfoMsg )
		   System.out.println("Added user: " + newUser.firstName + "," + newUser.lastName + "," +
				   newUser.id );
	   return newUser;
   }
   // Retrieves the user with the specified id. If no user exists, return null.
   @SuppressWarnings("unused")
   private User findUserByID(long id)
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
	   Chat newChat = new Chat(this);
	   newChat.id = Long.parseLong(chat.getJsonNumber("id").toString());
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
	   // Add the chat to the database
	   String sql = "";
	   try
	   {
		   Statement stmt = sqlc.createStatement();
		   sql = "INSERT INTO Chats (ID,Name,Type) VALUES (" +
				        Long.toString(newChat.id) + "," 
				        + (newChat.name != null ? ("'" + newChat.name + "'") : "NULL") + "," + 
				        (newChat.type == Chat.Type.Private ? "0" : 
				           (newChat.type == Chat.Type.Group ? "1" : "2")
				        ) + ");";
		   stmt.executeUpdate(sql);
		   stmt.close();
	   }
	   catch( SQLException e )
	   {
		   System.err.println("SQL query failed: " + sql);
		   e.printStackTrace();
	   }
	   return newChat;
   }
   private void removeChat(Chat chat)
   {
	   // Garbage collector will take care to delete chat contents after it is removed from the list
	   if(chats.remove(chat))
	   {
		   try
		   {
			   Statement stmt = sqlc.createStatement();
			   String chatID = Long.toString(chat.id);
			   stmt.executeUpdate("DELETE FROM Chats WHERE ID = " + chatID + ";");
			   stmt.executeUpdate("DELETE FROM ChatUsers WHERE ChatID = " + chatID + ";");
			   // Move messages to oldmessages
			   stmt.executeUpdate(
					 "INSERT INTO OldMessages SELECT * FROM Messages WHERE ChatID = " + chatID + ";");
			   stmt.executeUpdate("DELETE FROM Messages WHERE ChatID = " + chatID + ";");
			   stmt.close();
		   }
		   catch( SQLException e )
		   {
			   e.printStackTrace();
		   }
	   }
   }

   public void sendUpdates( String update )
   {
	   rdr = Json.createReader( new ByteArrayInputStream(update.getBytes()));
	   JsonObject updObj = rdr.readObject();
	   rdr.close();
	   sendUpdates( updObj );
   }
   // Use this method to send new updates to the bot. Updates must be in a json-formatted string
   public void sendUpdates( JsonObject update )
   {
	   String infoMsg = "Received update ";
	   // Get the message
	   
	   if( !update.containsKey("message") )
		   return;
	   
	   JsonObject message = update.getJsonObject("message");
	   
	   Chat chat = addChat(message.getJsonObject("chat"));
	   
	   User from = null;
	   if( message.containsKey("from"))
	   {
		   from = addUser(message.getJsonObject("from"));
		   chat.addUser(from);
		   infoMsg += "from:("+from.firstName+","+from.lastName+","+from.username+") ";
	   }
	   
	   // new_chat_member
	   if( message.containsKey("new_chat_member") )
	   {
		   User newComer = addUser(message.getJsonObject("new_chat_member"));
		   chat.addUser( newComer );
		   infoMsg += "new_chat_member:("+newComer.firstName+","+newComer.lastName+","+newComer.username+") ";
		   if( !suppressInfoMsg )
			   System.out.println(infoMsg);
		   return;
	   }
	   // left_chat_member
	   else if( message.containsKey("left_chat_member") )
	   {
		   User leftMember = addUser(message.getJsonObject("left_chat_member"));
		   infoMsg += "new_chat_member:("+leftMember.firstName+","+leftMember.lastName+","+leftMember.username+") ";
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
			   String[] params = text.split(" ");
			   String cmd = params[0];
			   params = Arrays.copyOfRange(params, 1, params.length);
			   
			   infoMsg += "command:("+cmd;
			   for( String par : params )
				   infoMsg += ","+par;
			   infoMsg += ")";
			   
			   // Run the command
			   for( Command command : commands )
			   {
				   if( command.getName().equals(cmd) )
				   {
					   command.execute(from, chat, params, this);
					   break;
				   }
			   }
		   }
		   else
			   infoMsg += "message:("+message.getString("text")+") ";
	   }
	   if( !suppressInfoMsg )
		   System.out.println(infoMsg);
   }
   
   // Main thread cycle seeking updates
   private void cycle() throws IOException
   { 
     switch( updatesMode )
     {
	    case LOCAL_POLLING:
	    	// Updates are stored in a local file called updates
	    	// which contanins a json string containing an array of updates called "updates"
	    	File file = new File("updates");
	    	if( !file.exists() )
	    		break;
	    	
	    	// If updates file exists, read it
	    	InputStream fis = new FileInputStream("updates");
	        rdr = Json.createReader(fis);
	        JsonObject updObj = rdr.readObject();
	        JsonArray updates = updObj.getJsonArray("updates");
	        for( int i = 0; i < updates.size(); i++ )
	        {
	        	sendUpdates( updates.getJsonObject(i) );
	        }
	        
	        fis.close();
	        rdr.close();
	    	file.delete();
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