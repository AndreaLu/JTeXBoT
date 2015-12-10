package commands;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import telegram.Chat;
import telegram.Command;
import telegram.TeleBot;
import telegram.User;

// TODO save sticker id of already compiled & loaded stickers to resend them in future
// to do this, you will have to keep them in an ordered data structure, some sort of simple
// database, so you can access them quickly. Obviously, the database will have to be saved 
// locally

// TODO take care of the timestamp (it gets in the way of the sticker containing a formula)

public class Tex implements Command
{
	@Override
	public void execute(User sender, Chat chat, String[] args, TeleBot botRef)
	{
		String LaTeXExpression = "";
		for(String arg : args)
			LaTeXExpression += arg + " ";
		
		// Compile TEX expression and convert to PNG (latex needed)
		// *****************************************************************************************
		if(utils.Util.Tex2Png(LaTeXExpression, "doc.png", 10) != 0)
		{
			chat.sendMessage("LaTeX error.");
			return ;
		}
		
		// Modify the PNG (add some vertical margin at the bottom)
		// *****************************************************************************************
		try
		{
			// Load doc.png to img
			BufferedImage img = ImageIO.read(new File("doc.png"));
			int w = img.getWidth(), h = img.getHeight();
			// Create a blank png of the right size
			BufferedImage scaled = new BufferedImage(
					(int)((double)w/(1.0-0.195)), h,
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = scaled.createGraphics();
			// Draw img to the new image
			g2.drawImage(
					img.getScaledInstance(w, h, BufferedImage.SCALE_DEFAULT),
					0, 0 , w, h, null);
			// Write back to doc.png
			ImageIO.write(scaled, "png", new File("doc.png"));
		}
		catch(IOException e)
		{
			(new File("doc.png")).delete();
			chat.sendMessage("PNG modify error.");
			return ;
		}
		
		// Convert doc.png to doc.webp and send the Sticker (cwebp needed)
		// *****************************************************************************************
		try 
		{
			Runtime rt = Runtime.getRuntime();
			Process p;
			p = rt.exec("cwebp -lossless doc.png -o doc.webp");
			p.waitFor();
			File sticker = new File("doc.webp");
			chat.sendSticker(sticker);
			sticker.delete(); // Just to keep the server clean
		}
		catch (Exception e)
		{
			(new File("doc.png")).delete(); // Just to keep the server clean
			chat.sendMessage("WEBP conversion error.");
			return ;
		}
	}
	private String name = "tex";
	@Override
	public String getName() 
	{
		return name;
	}
	private String description = "Returns the sticker of the specified TeX formula using LaTeX.\n" +
								 "Usage:      `/tex <latex_formula>`\n" +
								 "Example: `/tex \\lim_{x\\to 0}\\frac{\\sin(x)}{x}`";
	@Override
	public String getDescription()
	{
		return description;
	}
}
