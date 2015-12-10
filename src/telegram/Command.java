package telegram;
public interface Command
{
	public void execute(User sender, Chat chat, String[] args, TeleBot botRef);
	public String getName();
	public String getDescription();
}
