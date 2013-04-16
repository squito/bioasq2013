package dataingest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gson.Gson;

public class JsonToCorrLDA {

	public static void main(String[] args) {

		int numberArticlesToRead = Integer.parseInt(args[4]);
		Gson gson = new Gson();
		BufferedWriter docWriter = null;
		BufferedWriter vocabWriter = null;
		BufferedWriter labelWriter = null;

		BioasqReader bioasqReader = new BioasqReader(args[0], numberArticlesToRead);
		List<Article> documents = bioasqReader.read();

		try {
			docWriter = new BufferedWriter(new FileWriter(args[1]));
			vocabWriter = new BufferedWriter(new FileWriter(args[2]));
			labelWriter = new BufferedWriter(new FileWriter(args[3]));			
			FormatConverter converter = new FormatConverter(documents);
			converter.convert(docWriter, vocabWriter, labelWriter);

		} catch (FileNotFoundException e) {
			e.printStackTrace();			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			//Close the BufferedWriters
			try {		
				if (docWriter != null) {
					docWriter.flush();
					docWriter.close();
				}
				if (vocabWriter != null) {
					vocabWriter.flush();
					vocabWriter.close();
				}
				if (labelWriter != null) {
					labelWriter.flush();
					labelWriter.close();
				}                
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		System.out.println("Done.");
	}


	private static class FormatConverter {

		LinkedHashMap<String, Integer> vocab = new LinkedHashMap<String, Integer>();
		LinkedHashMap<String, Integer> labelVocab = new LinkedHashMap<String, Integer>();
		List<Article> docs;		

		public FormatConverter(List<Article> docs) {
			this.docs = docs;
		}

		public void convert(BufferedWriter docWriter, BufferedWriter vocabWriter, BufferedWriter labelWriter) throws IOException {

			// Get vocabularies into maps. 
			int i = 0;
			int j = 0;
			for(Article a : docs) {

				List<String> abstractWords = abstractWords(a.abstractText);				
				for(String w : abstractWords) {

					if(Stopwords.stopwords.contains(w) || w.length() < 2) {
						continue;
					}

					Integer id = vocab.get(w);
					if(id == null) {
						vocab.put(w,  i);
						id = i;
						i++;
					}
					
					docWriter.write(id + " ");
					
				}
				docWriter.newLine();

				for(String l : a.meshMajor) {
					Integer id = labelVocab.get(l);
					if(id == null) {
						labelVocab.put(l, j);
						id = j;
						j++;
					}
					
					docWriter.write(id + " ");
				}
				docWriter.newLine();
				
			}

			System.out.println("Successfully wrote document file.");

			writeVocab(vocabWriter, vocab);
			writeVocab(labelWriter, labelVocab);

			System.out.println("Successfully wrote vocabulary files.");
		}

		private List<String> abstractWords(String abstractText) {

			String [] strings = abstractText.toLowerCase().replaceAll("[^A-Za-z0-9 ]", "").split(" ");
			List<String> abstractWords = new ArrayList<String>();
			for(String s : strings) {
				abstractWords.add(s);
			}

			return abstractWords;
		}

		private void writeVocab(BufferedWriter bw, LinkedHashMap<String, Integer> vocab) throws IOException {

			for(String w : vocab.keySet()) {
				bw.write(w);
				bw.newLine();
			}
		}

	}


}
