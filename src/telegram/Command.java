package telegram;

/*
 *  Use this interface to create new commands for the bot (see Main.java)
 *  
 *  execute:
 *  Once a user types /name (name must be returned by getName()) execute is invoked.
 *  caller is a reference to the user who requested the command, chat is a reference
 *  to the chat where the command was requested and args are the parameters written
 *  after /name divided by a white space ('/name par0 par1 ...'), and botRef is a
 *  reference to the bot so that you can access commands and chats.
 *  
 *  getName:
 *  This method is used by the bot to forward received requests to the right command.
 *  
 *  getDescription:
 *  This method is automatically called when a user types /help to build the
 *  help message.
 *  

 */

public interface Command
{
	public void execute(User caller, Chat chat, String[] args, TeleBot botRef);
	public String getName();
	public String getDescription();
}
