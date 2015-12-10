package commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.fathzer.soft.javaluator.StaticVariableSet;

import telegram.Chat;
import telegram.Command;
import telegram.TeleBot;
import telegram.User;
import utils.Util;

public class Plot implements Command
{
	@Override
	public void execute(User sender, Chat chat, String[] args, TeleBot botRef)
	{
		chat.sendMessage("/plot sotto debug..");
		boolean val = true;
		if(val)
			return;
		
		try
		{
		// /plot expr min,max
		DoubleEvaluator eval = new DoubleEvaluator();
		String texCode =
				"\\documentclass[convert={density=300,size=1080x800,outext=.png}]{standalone}\n" +
				"\\usepackage{pgfplots}\n" +
				"\\begin{document}\n" +
				"\\begin{tikzpicture}\n" +
					"\\begin{axis}[width=600pt,axis x line=middle]\n";
		
		for(String arg : args)
		{
			// Extract funcion expression and domain from arg
			int vpos = arg.indexOf(',');
			if(vpos == -1)
				return;
			String expression = arg.substring(0,vpos);
			String domain     = arg.substring(vpos+1);
			System.out.println("  expr:"+expression);
			System.out.println("domain:"+domain);
			vpos = domain.indexOf(':');
			if(vpos == -1)
				return;
			double min, max;
			min = eval.evaluate(domain.substring(0,vpos));
			max = eval.evaluate(domain.substring(vpos+1));
			System.out.println("min:" + min);
			System.out.println("max:" + max);
			double step = (max-min)/200.0;
			double x = min;
			if(min >= max)
				return;
			
			// Compute the points of the plot
			StaticVariableSet<Double> variables = new StaticVariableSet<Double>();
			String points = "{";
			while( x <= max )
			{
				if(x != min)
					points += " ";
				double y = 0;
				try
				{
					variables.set("x", x);
					y = eval.evaluate(expression,variables);
					if( y == Double.NaN )
					{
						chat.sendMessage("Undefined value for x = " + x);
						return;
					}
					if( Double.isInfinite(y))
					{
						chat.sendMessage("Infinite value for x = " + x);
						return;
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					System.out.println("HAHAHA");
					return;
				}
				points += 
						"(" + Double.toString(x) + "," +
						Double.toString(y) +")";
				x += step;
			}
			points += "};";
			System.out.println(points);
			
			// Create .tex document
			texCode +=
					"\\addplot+[mark=none] coordinates\n" +
					points + "\n";
		}
		
		texCode +=
				"\\end{axis}\n" +
						"\\end{tikzpicture}\n" +
						"\\end{document}\n";
		PrintWriter out;
		try 
		{
			out = new PrintWriter("doc.tex");
			out.print(texCode);
			out.flush();
			out.close();
			System.out.println("Written doc.tex");
			
			// LaTeX Compile doc.tex creating doc.pdf
			Runtime rt = Runtime.getRuntime();
			Process p;
			p = rt.exec("pdflatex doc.tex");
			// Obtain compiling output: for some reasons, if you don't do so
			// pdflatex doesent create pdf
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String s = null;
            while ((s = stdInput.readLine()) != null) {
            	System.out.println(s);
            }
            if(Util.processWait(p, 10) != 0)
            {
            	chat.sendMessage("LaTeX error.");
            	return;
            }
	        System.out.println("generated doc.pdf");
	        // Convert doc.pdf to a png image
	        Util.Pdf2Png("doc.pdf", "doc.png");
	        
	        // Send the image to the chat
	        chat.sendPhoto(new File("doc.png"));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	catch(Exception e)
	{
		chat.sendMessage("LaTeX error");
		return;
	}
	}
	private String name = "plot";
	@Override
	public String getName()
	{
		return name;
	}
	private String description = "Returns the plot of a mathematical function.\n" +
			 "Usage:      `/plot <function>,<domain>`\n" +
			 "Example: `/plot sin(x),0:2*pi`\n" +
			 "You can also plot multiple functions if you put a white space in between.\n" +
			 "Example: `/plot sin(x),0:2*pi cos(x),0:2*pi`\n";
	@Override
	public String getDescription()
	{
		return description;
	}

}
