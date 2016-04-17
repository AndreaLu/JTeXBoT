package telegram;

//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
//import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


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
    	 System.out.println("Starting Telegram Bot " + bot.token);
    	 if( updatesMode == UpdatesMode.MANUAL )
    		 // No need for a thread seeking updates 
    		 return;
    	 
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
   
   public void sendUpdates( String jsonUpdate )
   {
	   System.out.println("Updates received: '" + jsonUpdate + "'");
   }
   private void cycle() throws IOException
   { 
         switch( updatesMode )
         {
		    case LOCAL_POLLING:
		    	// Updates are stored in a local file called updates
		    	// updates is a text file. The contents is like '{jsonUpdate0}{jsonUpdate1}...'
		    	File file = new File("updates");
		    	if( !file.exists() )
		    		break;
		    	byte[] encoded = Files.readAllBytes(Paths.get("updates"));
		    	if( encoded.length <= 0 )
		    		break;
		    	String updates = new String(encoded, Charset.defaultCharset());
		    	file.delete();
		    	
		    	// Extract jsonUpdates
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