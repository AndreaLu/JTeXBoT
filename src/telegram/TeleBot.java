package telegram;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// TODO Keep an updated database of chats locally so you can spam info on updates, for instance

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
      offset       =  0;                         // Offset and lastTime are used to ignore
      lastTime     =  0;                         // old messages.
      this.token   = token;                      // Token of this bot
      chats        = new ArrayList<Chat>();      // List of the chats
      runnable     = new BotThread(this);        // Create a thread for this bot
      commands     = new ArrayList<Command>();   // List of the avaialbe commands
      newMessages  = new ArrayList<Message>();   // List of the new messages to process
      users        = new ArrayList<User>();      // List of the known users
      updatesMode  = mode;
      
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
    	 if( updatesMode == UpdatesMode.MANUAL )
    		 return;
    	 
         while(true)
         {  
            try {
               bot.cycle();
               Thread.sleep(1000);
            } catch (IOException e) {
               e.printStackTrace();
            } catch (InterruptedException e) {
               e.printStackTrace();
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
   
   public void sendUpdates( String jsonUpdate )
   {
	   
   }
   private void cycle() throws IOException, InterruptedException
   {   
	  try
	  {
         switch( updatesMode )
         {
		    case LOCAL_POLLING:
		    	// updates are stored in a local file
		    break;
		    case LONG_POLLING:
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
	  catch( Exception e )
	  {
		  
	  }
   }
}