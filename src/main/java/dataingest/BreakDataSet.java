package dataingest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BreakDataSet {

	/**
	 * Break up the one giant file into smaller files.
	 * @param args
	 */
	public static void main(String[] args) {
		
		BufferedWriter writer = null;
		BufferedReader reader = null;
		
		String filename = args[0];
		String writeFilenameRoot = args[1];
		Integer articlesPerDoc = Integer.parseInt(args[2]);
		Integer totalNumberOfFiles = Integer.parseInt(args[3]);
		
		try {
			reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine(); // Skip first line.
			
			int i = 0;
			int fileno = 0;
			line = reader.readLine();
			while(line != null) {
				
				if(i % articlesPerDoc == 0) {					
					if(writer != null) {
						writer.write("\n]}");
						writer.flush(); 
						writer.close();
					}
					writer = new BufferedWriter(new FileWriter(writeFilenameRoot + "_" + articlesPerDoc + "_" + fileno + ".json"));
					writer.write("{'articles'=[\n");
					System.out.println("Wrote file " + fileno);
					fileno++;
					if(fileno > totalNumberOfFiles) break;
				} else {
					if(writer == null) {
						throw new RuntimeException("writer shouldnt be null here.");
					}
					writer.write(",\n");
				}
				
				writer.write(line.replaceAll(",$", ""));
				line = reader.readLine();
				i++;
			}
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			//Close the files.
			try {
				if (reader != null) {
					reader.close();
				}
				if(writer != null) {
					writer.flush();
					writer.close();
				}
				
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			
		}
	
		System.out.println("Done.");
	}

}
