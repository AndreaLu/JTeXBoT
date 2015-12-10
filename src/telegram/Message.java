package telegram;

public class Message
{
   public Message()
   {
      chat = null;
      sender = null;
      content = "";
   }
   public String content;
   public User sender;
   public Chat chat;
}