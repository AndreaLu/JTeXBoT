package telegram;

import commands.About;
import commands.Changelog;
import commands.Help;
import commands.Plot;
import commands.Tex;

public class Main
{
	public static void main(String args[])
	{
		// This token is invalid
		String tok = "141222412:AAGk358IjSeYZMiJJVXUn3xAXLYmJ1NEhQg";
		TeleBot bot = new TeleBot(tok, 141222412);
		bot.commands.add(new Tex());
		bot.commands.add(new Help());
		bot.commands.add(new About());
		bot.commands.add(new Changelog());
		bot.commands.add(new Plot());
	}
}


