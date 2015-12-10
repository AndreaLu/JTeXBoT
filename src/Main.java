
import telegram.*;
import commands.*;

public class Main
{
	public static void main(String args[])
	{
		if(args.length == 0)
		{
			System.out.println("Please type the token as the first parameter.");
			return;
		}
		String tok = args[0];
		TeleBot bot = new TeleBot(tok, 141222412);
		bot.commands.add(new Tex());
		bot.commands.add(new Help());
		bot.commands.add(new About());
		bot.commands.add(new Changelog());
		bot.commands.add(new Plot());
	}
}


