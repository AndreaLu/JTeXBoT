import telegram.*;
import telegram.TeleBot.UpdatesMode;
import commands.*;

public class Main
{
   public static void main(String args[])
   {
      String token = args[0];
      TeleBot bot = new TeleBot(token, UpdatesMode.LOCAL_POLLING);
      bot.commands.add(new About());
      bot.commands.add(new Changelog());
      bot.commands.add(new Help());
      bot.commands.add(new Plot());
      bot.commands.add(new Tex());
   }
}