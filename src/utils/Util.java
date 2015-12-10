package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public abstract class Util 
{
	private static boolean processAlive(Process p)
	{
		try
		{
			// If p is still alive an exception will be caught
			p.exitValue();
			return false;
		}
		catch( IllegalThreadStateException e )
		{
			return true;
		}
	}
	// Wait for a process to terminate.
	// return value:
	// 0 - process terminated normally
	// 1 - process terminated by timeout
	// 2 - process exited with errors
	public static int processWait(Process p)
	{
		while(processAlive(p))
		{
			try {
				Thread.sleep(15);
			} catch (InterruptedException e) {
				// Don't care
			}
		}
		return 0;
	}
	public static int processWait(Process p, int timeoutSeconds)
	{
		int counter = timeoutSeconds * 10;
		while(processAlive(p))
		{
			counter --;
			if(counter == 0)
			{
				p.destroy();
				return 1;
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) 
			{
				// Don't care
			}
		}
		return 0;
	}
	
	public static int Tex2Png(String texExpression, String pngFilename, int secondsTimeout) 
	{
		try
		{
			// Create .tex file
			String contents = 
					"\\documentclass[12pt]{article}\n" +
					"\\usepackage{geometry}\n" +
					"\\pagenumbering{gobble}\n" +
					"\\clearpage\n" +
					"\\geometry{a6paper,left=1px,top=1px}\n" +
					"\\begin{document}\n" +
					  "$\\displaystyle\n" +
					  texExpression + "$\n" +                        
					"\\end{document}";
			PrintWriter out = new PrintWriter("doc.tex");
			out.print(contents);
			out.flush();
			out.close();
			
			// Compile .tex file to .dvi with latex
			Runtime runtime = Runtime.getRuntime();
			Process p = runtime.exec("latex doc.tex");
			// Read latex output
	        // Handle Timeout compiler error
	        if(processWait(p,secondsTimeout) == 1)
	        	return 1;
	        // Handle latex generic error
	        if(p.exitValue() != 0)
	        	return 2;    
	        // Convert .dvi file to .png with transparent background
	        p = runtime.exec("dvipng doc.dvi -bg Transparent -D 700 -o " + pngFilename);
	        processWait(p);
	        // Delete useless files (just for order's sake)
		    (new File("doc.aux")).delete();
		    (new File("doc.dvi")).delete();
		    (new File("doc.log")).delete();
		    (new File("doc.tex")).delete();
	        return 0;
		}
		catch(Exception e)
		{
			return 2;
		}
	}
	public static void Pdf2Png(String pdfFilename, String pngFilename) throws IOException
	{
		
        try {
        	// call the process!
    		Runtime rt = Runtime.getRuntime();
    		String command = "java -jar pdf2png.jar " + pdfFilename + " " + pngFilename;
    		System.out.println("Execute this: " + command);
    		Process p = rt.exec(command);
    		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
    		 
            // read the output from the command
    		System.out.println("pdf2png output:\n");
            while ((stdInput.readLine()) != null) {}
			p.waitFor();
		} 
        catch (InterruptedException e) 
        {
			e.printStackTrace();
		}
	}
}
