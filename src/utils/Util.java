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
   
   /**
    * processWait
    * Blocks the current thread till a specified process terminates.
    * @param p The process to wait for.
    * @param timeoutSeconds timeout seconds.
    * @return 0 - The process terminated normally.
    * @return 1 - The process was terminated by timeout.
    * @return 2 - The process terminated with errors.
    */
   public static int processWait(Process p)
   {
      while(processAlive(p))
      {
         try 
         {
            Thread.sleep(15);
         } 
         catch (InterruptedException e) 
         {
            // Don't care
         }
      }
      return p.exitValue() == 0 ? 0 : 2;
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
         try 
         {
            Thread.sleep(10);
         } 
         catch (InterruptedException e) 
         {
            // Don't care
         }
      }
      return p.exitValue() == 0 ? 0 : 2;
   }
   /**
    * Tex2Png
    * @param texExpression TeX expression to compile and convert to PNG.
    * @param pngFilename filename of the PNG file to create.
    * @param timeoutSeconds[optional] timeout seconds to prevent faulty TeX expression
    * from stucking the server. The default value is 30.
    * @return 0 The conversion succeeded and the specified file is ready.
    * @return 1 The conversion was terminated by timeout.
    * @return 2 The conversion failed because of reasons.
    */
   public static int Tex2Png(String texExpression, String pngFilename)
   {
      return Tex2Png(texExpression, pngFilename, 30);
   }
   public static int Tex2Png(String texExpression, String pngFilename, int timeoutSeconds) 
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
         // Handle errors
         int result = processWait(p,timeoutSeconds);
         if(result != 0)
            return result;   
         // Convert .dvi file to .png with transparent background
         p = runtime.exec("dvipng doc.dvi -bg Transparent -D 700 -o " + pngFilename);
         processWait(p);
         // Delete useless files (just to keep the server clean)
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
           // call the process
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
