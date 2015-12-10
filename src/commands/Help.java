package commands;

import telegram.Chat;
import telegram.Command;
import telegram.TeleBot;
import telegram.User;

public class Help implements Command
{
   @Override
   public void execute(User caller, Chat chat, String[] args, TeleBot botRef)
   {
      String message = "";
      for(Command cmd : botRef.commands)
      {
         message += "\u2022 /" + cmd.getName() + "\n";
         message += cmd.getDescription() + "\n";
      }
      chat.sendMessage(message,true);
   }
   private String name = "help";
   @Override
   public String getName()
   {
      return name;
   }
   private String description = 
         "Returns a list of the available commands along with a brief description.";
   @Override 
   public String getDescription()
   {
      return description;
   }
}
