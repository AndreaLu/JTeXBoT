package commands;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import telegram.Chat;
import telegram.Command;
import telegram.TeleBot;
import telegram.User;

public class Changelog implements Command 
{
	@Override
	public void execute(User sender, Chat chat, String[] args, TeleBot botRef)
	{
		try 
		{
			String changelog = readFile("changelog.txt", StandardCharsets.UTF_8);
			changelog = changelog.replace("->", "✓  ");
			chat.sendMessage(changelog, true); // Markdown sendMessage
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	private String readFile(String path, Charset encoding) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
	private String name = "changelog";
	@Override
	public String getName() 
	{
		return name;
	}
	private String description = "Displays the changelog.";
	@Override
	public String getDescription() 
	{
		return description;
	}
}
