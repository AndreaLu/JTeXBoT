package telegram;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

// TODO Keep an updated database of chats locally so you can spam info on updates, for instance

public class TeleBot 
{
	// Public stuff
	// *********************************************************************************************
	public List<Command> commands;
	public TeleBot(String token, int userID)
	{
		// Initialize variables
		offset		=  0;							// Offset and lastTime are used to ignore
		lastTime	=  0;							// old messages.
		this.token	= token;						// Token of this bot
		chats		= new ArrayList<Chat>();		// List of the chats
		runnable	= new BotThread(this);			// Create a thread for this bot
		commands	= new ArrayList<Command>();		// List of the avaialbe commands
		newMessages	= new ArrayList<Message>();		// List of the new messages to process
		users		= new ArrayList<User>();		// List of the known users
		runnable.start();							// Start the thread for this bot
	}
	public void setDefaultAction(Command cmd)
	{
		this.defaultAction = cmd;
	}
		
	// Private stuff (mind your own business)
	// *********************************************************************************************
	private Command defaultAction = null;
	// Define a thread for the bot
	private class BotThread implements Runnable
	{
		private TeleBot bot;
		public BotThread( TeleBot bot )
		{
			this.bot = bot;
		}
		public void run()
		{
			while(true)
			{  
				try {
					bot.cycle();
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
	
	private void cycle() throws IOException, InterruptedException
	{	
		// Retrieve Updates as JSON string
		// Fill newMessages with all the new messages received
		URL url = new URL(
				offset != -1 ? 
				("https://api.telegram.org/bot" + token + "/getupdates?offset=" + offset)
				:
				("https://api.telegram.org/bot" + token + "/getupdates")
		);
		try( InputStream is = url.openStream();
			JsonReader rdr = Json.createReader(is))
		{
			JsonObject obj = rdr.readObject();
			JsonArray results = obj.getJsonArray("result");
			for(JsonObject result : results.getValuesAs(JsonObject.class)) 
			{
				offset = result.getInt("update_id") + 1;
				JsonObject message = result.getJsonObject("message");
				int time = message.getInt("date");
				if(time >= lastTime)
				{
					lastTime = time;
					
					Message msg = new Message();
					
					JsonObject from = message.getJsonObject("from");
					int chatID = message.getJsonObject("chat").getInt("id");
					int userID = from.getInt("id");
					
					// If the chat or the user are unknown, create them
					for(Chat c : chats)
						if(c.id == chatID)
							msg.chat = c;
					if(msg.chat == null)
					{
						Chat newChat = new Chat(chatID, token);
						chats.add(newChat);
						msg.chat = newChat;
					}
					
					for(User u : users)
						if(u.id == userID)
							msg.sender = u;
					if(msg.sender == null)
					{
						User newUser = new User(userID);
						if(from.containsKey("first_name"))
							newUser.firstName = 
								message.getJsonObject("from").getString("first_name");
						if(from.containsKey("last_name"))
							newUser.lastName  = 
								message.getJsonObject("from").getString("last_name");
						users.add(newUser);
						msg.sender = newUser;
					}
					
					if(message.containsKey("text"))
					{
						msg.content = message.getString("text");
						newMessages.add(msg);
						Date date = new Date ();
						date.setTime((long)message.getInt("date")*1000L);
						String log = 
								"\nSender: " + msg.sender.firstName + " Message: " 
								+ msg.content + " ChatID: " + msg.chat.id
								+ " Date: " + date.toString();
						System.out.println(log);
						// Log the messages on a file
						try
						{
						    Files.write(
						    		Paths.get("log.txt"),
						    		log.getBytes(),
						    		StandardOpenOption.APPEND);
						}
						catch (IOException e) 
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		// Analyze new messages
		for( Message msg : newMessages )
		{	
			String command = null;
			String a = msg.content;
			if(a.startsWith("/"))
			{
				int pf = a.indexOf(' ');
				if(pf != -1)
					command = a.substring(1,pf);
				else
					command = a.substring(1);
				a = a.substring(pf+1, a.length());
				
				// Take care for @dest in param
				if(command.contains("@"))
				{
					String dest = command.substring(command.indexOf("@")+1);
					if(!dest.equals("luxtexbot"))
						continue;
					else
						command = command.substring(0,command.indexOf("@"));
				}
			}
			String[] params = a.split(" ");
			// If command is no longer null, there's a command, execute it
			if(command != null)
			{
				boolean commandFound = false;
				// Search for the right command
				for(Command cmd : commands)
					if((cmd.getName().equals(command)))
					{
						cmd.execute(msg.sender, msg.chat, params, this);
						commandFound = true;
						break;
					}
				if(!commandFound && msg.content.charAt(0) == '/')
				{
					// TODO move this outside of TeleBot
					msg.chat.sendMessage(
							"`" + command + "` does not name a command.\n"
							+ "Type /help to get the list of available commands.",
							true);
					continue;
				}
			}
			else
			{
				if( defaultAction != null)
				{
					String[] p = new String[1];
					p[0] = a;
					this.defaultAction.execute(msg.sender, msg.chat, p, this);
				}
			}
		}
		// All the messages have been processed, clear the list
		newMessages.clear();
		// Sleep 1 second
		Thread.sleep(1000);
	}
}