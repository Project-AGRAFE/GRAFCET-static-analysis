package de.hsu.grafcet.staticAnalysis.popup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class Util {

	public static void createOutputFile(String out, String pathName) {
		//System.out.println(out);
		
		//TODO getParentsFile, createNewFile in anderem Paket nachpflegen
		
		try {
            //Whatever the file path is.
            File statText = new File(pathName);
            
            statText.getParentFile().mkdirs();
            statText.createNewFile();
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);    
            Writer w = new BufferedWriter(osw);
            w.write(out);
            w.close();
        } catch (IOException e) {
            System.err.println("Problem writing to the file statsTest.txt");
            e.printStackTrace();
        }
	}
	
}
