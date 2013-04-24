package dataingest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

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
			KeepWordsIdentifier identifier = new KeepWordsIdentifier(documents, 20);
			Set<String> keepWords = identifier.identify();
			FormatConverter converter = new FormatConverter(documents, keepWords);
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


	private static class KeepWordsIdentifier {
	
		List<Article> docs;
		int threshold;
		
		public KeepWordsIdentifier(List<Article> docs, int threshold) {
			this.docs = docs;
			this.threshold = threshold;
		}

		public Set<String> identify() {
			
			HashMap<String, Integer> count = new HashMap<String, Integer>();
			for(Article a : docs) {
				
				for(String s : abstractWords(a.abstractText)) {
					Integer n = count.get(s);
					if(n == null) {
						count.put(s, 1);
					} else {
						count.put(s, 1 + n);
					}
				}
				
			}
			
			HashSet<String> keepWords = new HashSet<String>();
			for(String s : count.keySet()) {
				if(count.get(s) >= threshold) {
					keepWords.add(s);
				}
			}
			
			System.out.println("Total number of unique words: " + count.size());
			System.out.println("Total kept words with threshold " + threshold + " : " + keepWords.size()); 
			 
			return keepWords;
			
		}
		
	}
	
	
	private static class FormatConverter {

		LinkedHashMap<String, Integer> vocab = new LinkedHashMap<String, Integer>();
		LinkedHashMap<String, Integer> labelVocab = new LinkedHashMap<String, Integer>();
		List<Article> docs;		
		Set<String>keepWords;

		public FormatConverter(List<Article> docs, Set<String> keepWords) {
			this.docs = docs;
			this.keepWords = keepWords;
		}

		public void convert(BufferedWriter docWriter, BufferedWriter vocabWriter, BufferedWriter labelWriter) throws IOException {

			// Get vocabularies into maps. 
			int i = 0;
			int j = 0;
			for(Article a : docs) {

				List<String> abstractWords = abstractWords(a.abstractText);				
				for(String w : abstractWords) {

					if(Stopwords.stopwords.contains(w) || w.length() < 2 || (!keepWords.contains(w))) {
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

		
		private void writeVocab(BufferedWriter bw, LinkedHashMap<String, Integer> vocab) throws IOException {

			for(String w : vocab.keySet()) {
				bw.write(w);
				bw.newLine();
			}
		}

	}

	private static List<String> abstractWords(String abstractText) {

		String [] strings = abstractText.toLowerCase().replaceAll("[^A-Za-z0-9 ]", "").split(" ");
		List<String> abstractWords = new ArrayList<String>();
		for(String s : strings) {
			abstractWords.add(s);
		}

		return abstractWords;
	}

	

}
