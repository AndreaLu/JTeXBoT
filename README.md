## JTeXBoT
This is a simple bot for Telegram written in Java using the long polling technique. In other words, the bot will periodically request updates from Telegram, enabling any computer with Java installed and an internet connection to run the bot so that there is no need for a web server. However, if you already have one, you may be interested in a [simpler](https://github.com/AndreaLu/PTeXBoT) solution.

The main functionality of the bot is to send elegant mathematical expressions as stickers.
## Usage
One can remove the implemented functionalities (TeX stickers) and/or implement his own, thus creating a new original bot.
#### Creating a Bot
To create a new bot just import `telegram.*` and instantiate `TeleBot`: `TeleBot bot = new TeleBot(token)`. It will run in a different thread so that the main one is not blocked.
#### Implementing a new command
The only messages that the bot views are command-type messages, that is messages in the form  `/cmd param0 param1`, where the command is `cmd`. To implement a new command one has to define a class extending `telegram.Command`. Here is a valid template:
```
package commands;

import telegram.Chat;
import telegram.TeleBot;
import telegram.User;

public class TemplateCommand implements telegram.Command
{
   @Override
   public void execute(User caller, Chat chat, String[] args, TeleBot botRef) 
   {
      chat.sendMessage(
            "Thank you for calling /template_command !"
      );
   }
   private String name = "template_command";
   @Override
   public String getName() 
   {
      return name;
   }
   private String description = "This is just a template.";
   @Override
   public String getDescription()
   {
      return description;
   }
}
```
Once the command is added to the bot with `bot.commands.add(new TemplateCommand());`, it will automatically recognize when someone types '/template_command' and invoke `execute`.
