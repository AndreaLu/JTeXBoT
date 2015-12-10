package commands;

import telegram.Chat;
import telegram.TeleBot;
import telegram.User;

public class About implements telegram.Command
{
   // Id of a preloaded Sticker
   private String stickerID = "BQADBAADAgADy8SJCDzXkd29_9R2Ag";
   @Override
   public void execute(User caller, Chat chat, String[] args, TeleBot botRef) 
   {
      // Send sticker and text
      chat.sendSticker(stickerID);
      chat.sendMessage(
            "I was born because of a very tiny opening.. " +
            "in between my Creator's duties.\n" +
            "Enjoy fag math formulas.\n" +            
            "p.s.: I speak Java ☕, find me at https://github.com/AndreaLu/TeXBoT.\n" +
            "Talk to me: @alu_texbot\n" +
            "\n" +
            "TeXBoT v0.2β\n" +
            "Release date: 7 December 2015\n" +
            "Author: Andrea Luzzati"
      );
   }
   private String name = "about";
   @Override
   public String getName() 
   {
      return name;
   }
   private String description = "Displays some informations about me.";
   @Override
   public String getDescription()
   {
      return description;
   }
   
}