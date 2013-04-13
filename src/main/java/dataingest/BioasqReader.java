package main.java.dataingest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class BioasqReader {

	String filename;
	int numberArticlesToRead;
	Gson gson = new Gson();
	BufferedReader reader = null;

	public BioasqReader(String filename, int numberArticlesToRead) {

		this.filename = filename;
		this.numberArticlesToRead = numberArticlesToRead;

	}

	protected List<Article> read() {

		List<Article> documents = new ArrayList<Article>();

		try {
			reader = new BufferedReader(new FileReader(filename));

			reader.readLine(); // Skip very first line. 
			for(int i=0; i < numberArticlesToRead; i++) {
				String line = reader.readLine();
				if(line == null || line.equals("")) {
					System.out.println("End of input reached, " + i + " documents successfully read" );
					break;
				}

				documents.add( gson.fromJson(line.replaceAll(",$", ""), Article.class) );

				if(i % 10000 == 0) {
					System.out.println("Read " + i + " of " + numberArticlesToRead);
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			//Close the BufferedReader.
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		System.out.println("Successfully read file");
		return documents;

	}

}
