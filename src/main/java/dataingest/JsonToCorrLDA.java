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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;

public class JsonToCorrLDA {

	public static void main(String[] args) throws KeepWordsTooSmallException {

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
			KeepWordsIdentifier identifier = new KeepWordsIdentifier(documents);
//			Set<String> keepWords = identifier.identifyHighFreq(20);
			Set<String> keepWords = identifier.identifyTfidf(13.0);
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
		double tfidfThreshold;
		private Map<String, Integer> totalFreq;  // Total number of times word appears anywhere.
		private Map<String, Integer> docFreq;    // Number of documents that have the word.
		private Map<String, Double> idf;        // Inverse doc freq = log(D/docFreq).
		private Map<String, Integer> maxTf;     // Maximum term frequency across all docs. 
		private Map<String, Double> maxTfIdf;   // Maximum tfidf across all docs. 
		
		public KeepWordsIdentifier(List<Article> docs) {
			this.docs = docs;
		}

		protected Set<String> identifyHighFreq(int threshold) {
			
			this.threshold = threshold;
			computeCounts();
			Set<String> keepWords = new HashSet<String>();
			for(Entry<String, Integer> e : totalFreq.entrySet()) {
				if(e.getValue() >= threshold) {
					keepWords.add(e.getKey());
				}
			}
			
			System.out.println("Total number of unique words: " + totalFreq.size());
			System.out.println("Total kept words with threshold " + threshold + " : " + keepWords.size()); 
			 
			return keepWords;
			
		}
		
		protected Set<String> identifyTfidf(double tfidfThreshold) {
			
			this.tfidfThreshold = tfidfThreshold;
			computeCounts();
			Set<String> keepWords = new HashSet<String>();
			for(Entry<String, Double> e : maxTfIdf.entrySet()) {
				if(e.getValue() >= tfidfThreshold) {
					keepWords.add(e.getKey());
				}
			}
			
			System.out.println("Total number of unique words: " + totalFreq.size());
			System.out.println("Total kept words with tfidf threshold " + tfidfThreshold + " : " + keepWords.size()); 
			 
			return keepWords;
			
		}
		
		
		
		private void computeCounts() {
			
			totalFreq = new HashMap<String, Integer>();
			docFreq = new HashMap<String, Integer>();			
			idf = new HashMap<String, Double>();
			maxTf = new HashMap<String, Integer>();
			maxTfIdf = new HashMap<String, Double>();
			
			for(Article a : docs) {
				
				List<String> words = abstractWords(a.abstractText);

				accumulateMap(new HashSet<String>(words), docFreq);
				accumulateMap(words, totalFreq);	
																
				Map<String, Integer> thisDoc = new HashMap<String, Integer>();
				accumulateMap(words, thisDoc);
				
				for(Entry<String, Integer> e : thisDoc.entrySet()) {
					Integer n = maxTf.get(e.getKey());
					if(n == null || e.getValue() > n) {
						maxTf.put(e.getKey(), e.getValue());
					}
				}
				
			}
			
			for(Entry<String, Integer> e : docFreq.entrySet()) {
				idf.put(e.getKey(), Math.log( (double)docs.size()/e.getValue() )  );
			}
			
			for(Entry<String, Integer> e : maxTf.entrySet()) {
				maxTfIdf.put(e.getKey(), e.getValue() * idf.get(e.getKey()));
			}
			
			
		}
		
	}
	
	private static void accumulateMap(Iterable<String> iterable, Map<String, Integer> map) {
		for(String s : iterable) {
			Integer n = map.get(s);
			if(n == null) {
				map.put(s, 1);
			} else {
				map.put(s, 1 + n);
			}
		}
	}
	
	
	private static class KeepWordsTooSmallException extends Exception {}
	
	private static class FormatConverter {

		LinkedHashMap<String, Integer> vocab = new LinkedHashMap<String, Integer>();
		LinkedHashMap<String, Integer> labelVocab = new LinkedHashMap<String, Integer>();
		List<Article> docs;		
		Set<String>keepWords;

		public FormatConverter(List<Article> docs, Set<String> keepWords) {
			this.docs = docs;
			this.keepWords = keepWords;
		}

		public void convert(BufferedWriter docWriter, BufferedWriter vocabWriter, BufferedWriter labelWriter) throws IOException, KeepWordsTooSmallException {

			// Get vocabularies into maps. 
			int i = 0;
			int j = 0;
			for(Article a : docs) {

				List<String> abstractWords = abstractWords(a.abstractText);	
				boolean allWordsRejected = true;
				for(String w : abstractWords) {

					if(Stopwords.stopwords.contains(w) || w.length() < 3 || (!keepWords.contains(w))) {
						continue;
					}

					allWordsRejected = false;
					Integer id = vocab.get(w);
					if(id == null) {
						vocab.put(w,  i);
						id = i;
						i++;
					}
					
					docWriter.write(id + " ");
					
				}
				if(allWordsRejected) {
					throw new KeepWordsTooSmallException();
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
