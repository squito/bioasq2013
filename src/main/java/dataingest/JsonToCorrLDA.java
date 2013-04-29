package dataingest;

import inference.variational.corrlda.CorrLDA;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
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

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

public class JsonToCorrLDA {

	public static void main(String[] args) throws KeepWordsTooSmallException, JSAPException {

		SimpleJSAP jsap = new SimpleJSAP( CorrLDA.class.getName(), "Launches correspondence LDA.",
	            new Parameter[] {
	                new FlaggedOption("jsonFilename")
	                .setStringParser(JSAP.STRING_PARSER)
	                .setRequired(true)
	                .setLongFlag("jsonFilename")
	                .setHelp("Filename of json file we will read from."),	
	                new FlaggedOption("docFilename")
	                .setStringParser(JSAP.STRING_PARSER)
	                .setRequired(true)
	                .setLongFlag("docFilename")
	                .setHelp("Filename of document file we will write."),
	                new FlaggedOption("vocabFilename")
	                .setStringParser(JSAP.STRING_PARSER)
	                .setRequired(true)
	                .setLongFlag("vocabFilename")
	                .setHelp("Filename of vocab file we will write."),
	                new FlaggedOption("labelFilename")
	                .setStringParser(JSAP.STRING_PARSER)
	                .setRequired(true)
	                .setLongFlag("labelFilename")
	                .setHelp("Filename of label file we will write."),
	                new FlaggedOption("numberArticles")
	                .setStringParser(JSAP.INTEGER_PARSER)
	                .setRequired(true)
	                .setLongFlag("numberArticles")
	                .setHelp("Number of articles to read."),
	                new FlaggedOption("frequencyThreshold")
	                .setStringParser(JSAP.INTEGER_PARSER)
	                .setRequired(false)
	                .setDefault("0")
	                .setLongFlag("frequencyThreshold")
	                .setHelp("Threshold on corpus word frequency."),
	                new FlaggedOption("tfidfThreshold")
	                .setStringParser(JSAP.DOUBLE_PARSER)
	                .setRequired(false)
	                .setDefault("13.0")
	                .setLongFlag("tfidfThreshold")
	                .setHelp("Threshold on tfidf word frequency."),
	                new FlaggedOption("tfidfLabelThreshold")
	                .setStringParser(JSAP.DOUBLE_PARSER)
	                .setRequired(false)
	                .setDefault("13.0")
	                .setLongFlag("tfidfLabelThreshold")
	                .setHelp("Threshold on tfidf labels.")
	                }
		);
		
		JSAPResult config = jsap.parse(args);    
        if ( jsap.messagePrinted() ) System.exit( 1 );
		
		String jsonFilename = config.getString("jsonFilename");
		String docFilename = config.getString("docFilename");
		String vocabFilename = config.getString("vocabFilename");
		String labelFilename = config.getString("labelFilename");
		Integer numberArticlesToRead = config.getInt("numberArticles");
		Integer frequencyThreshold = config.getInt("frequencyThreshold");
		Double tfidfThreshold = config.getDouble("tfidfThreshold");
        Double tfidfLabelThreshold = config.getDouble("tfidfLabelThreshold");
		
		BufferedWriter docWriter = null;
		BufferedWriter vocabWriter = null;
		BufferedWriter labelWriter = null;

		BioasqReader bioasqReader = new BioasqReader(jsonFilename, numberArticlesToRead);
		List<Article> documents = bioasqReader.read();

		try {
			docWriter = new BufferedWriter(new FileWriter(docFilename));
			vocabWriter = new BufferedWriter(new FileWriter(vocabFilename));
			labelWriter = new BufferedWriter(new FileWriter(labelFilename));	
			KeepWordsIdentifier identifier = new KeepWordsIdentifier(documents);
			Set<String> keepWords = null;
			if(frequencyThreshold > 0) {
				keepWords = identifier.identifyHighFreqAbstractWords(frequencyThreshold);
			} 
			if(tfidfThreshold > 0) {
				keepWords = intersection(identifier.identifyTfidfAbstractWords(tfidfThreshold), keepWords);
			}
			if(keepWords == null) {
				throw new RuntimeException("keepWords is null");
			}

			Set<String>keepLabels = null;
			keepLabels = identifier.identifyTfidfLabels(tfidfLabelThreshold);
			
			System.out.println("Final kept words : " + keepWords.size());
			System.out.println("Final kept labels: " + keepLabels.size());
			
			FormatConverter converter = new FormatConverter(documents, keepWords, keepLabels);
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


	// Return intersection between two sets. Intersection between null and null will be null,
	// intersection between null and non-null will be non-null, and intersection between two non-nulls
	// will be their intersection. 
	private static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
		Set<T> set = new HashSet<T>();				
		if(set1 == null && set2 == null) {
			return null;  // Return null if both are null. 
		}
		if(set1 == null) {
			return set2;
		} 
		if(set2 == null) {
			return set1;
		}
		for(T t : set2) {
			if(set1.contains(t)) {
				set.add(t);
			}
		}
		return set;
	}
	
	private static class Tfidf {
		
		public Tfidf(WordExtractor extractor) {
			this.extractor = extractor;
		}
		Map<String, Integer> totalFreq;  // Total number of times word appears anywhere.
		Map<String, Integer> docFreq;    // Number of documents that have the word.
		Map<String, Double> idf;        // Inverse doc freq = log(D/docFreq).
		Map<String, Integer> maxTf;     // Maximum term frequency across all docs. 
		Map<String, Double> maxTfIdf;   // Maximum tfidf across all docs. 
		WordExtractor extractor;
		
	}
	
	private static interface WordExtractor {
		List<String> extractWords(Article a);
	}
	
	private static class AbstractWordExtractor implements WordExtractor {
		public List<String> extractWords(Article a) {
			return abstractWords(a.abstractText);
		}
	}
	
	private static class LabelWordExtractor implements WordExtractor {
		public List<String> extractWords(Article a) {
			return a.meshMajor;
		}
	}
	
	private static class KeepWordsIdentifier {
	
		List<Article> docs;
		Tfidf abstractTfdif = new Tfidf(new AbstractWordExtractor());
		Tfidf labelsTfdif = new Tfidf(new LabelWordExtractor());
		
		public KeepWordsIdentifier(List<Article> docs) {
			this.docs = docs;
		}

		private Set<String> identifyHighFreq(Tfidf tfdif, int threshold) {
			computeCounts(tfdif);
			Set<String> set = new HashSet<String>();
			for(Entry<String, Integer> e : tfdif.totalFreq.entrySet()) {
				if(e.getValue() >= threshold) {
					set.add(e.getKey());
				}
			}
			return set;
		}
		
		protected Set<String> identifyHighFreqAbstractWords(int threshold) {
			
			Set<String> keepWords = identifyHighFreq(abstractTfdif, threshold);
			System.out.println("Total number of words meeting frequency threshold " + threshold + " : " + keepWords.size()); 
			return keepWords;
			
		}
		
		public Set<String> identifyHighFreqLabels(int threshold) {

			Set<String> keepLabels = identifyHighFreq(labelsTfdif, threshold);
			System.out.println("Total number of labels meeting frequency threshold " + threshold + " : " + keepLabels.size()); 
			return keepLabels;
	
		}
		
		protected Set<String> identifyTfidfAbstractWords(double threshold) {
			
			Set<String> keepWords = identifyTfidf(abstractTfdif, threshold);
			System.out.println("Total number of words meeting tfidf threshold " + threshold + " : " + keepWords.size()); 
			return keepWords;
			
		}
		
		public Set<String> identifyTfidfLabels(double threshold) {

			Set<String> keepLabels = identifyTfidf(labelsTfdif, threshold);
			System.out.println("Total number of labels meeting tfidf threshold " + threshold + " : " + keepLabels.size()); 
			return keepLabels;

		}
		
		private Set<String> identifyTfidf(Tfidf tfidf, double threshold) {
			computeCounts(tfidf);
			Set<String> set = new HashSet<String>();
			for(Entry<String, Double> e : tfidf.maxTfIdf.entrySet()) {
				if(e.getValue() >= threshold) {
					set.add(e.getKey());
				}
			}
			return set;
		}
		
		private void computeCounts(Tfidf tfidf) {
			
			tfidf.totalFreq = new HashMap<String, Integer>();
			tfidf.docFreq = new HashMap<String, Integer>();			
			tfidf.idf = new HashMap<String, Double>();
			tfidf.maxTf = new HashMap<String, Integer>();
			tfidf.maxTfIdf = new HashMap<String, Double>();
			
			for(Article a : docs) {
				
				List<String> words = tfidf.extractor.extractWords(a);

				accumulateMap(new HashSet<String>(words), tfidf.docFreq);
				accumulateMap(words, tfidf.totalFreq);	
																
				Map<String, Integer> thisDoc = new HashMap<String, Integer>();
				accumulateMap(words, thisDoc);
				
				for(Entry<String, Integer> e : thisDoc.entrySet()) {
					Integer n = tfidf.maxTf.get(e.getKey());
					if(n == null || e.getValue() > n) {
						tfidf.maxTf.put(e.getKey(), e.getValue());
					}
				}
				
			}
			
			for(Entry<String, Integer> e : tfidf.docFreq.entrySet()) {
				tfidf.idf.put(e.getKey(), Math.log( (double)docs.size()/e.getValue() )  );
			}
			
			for(Entry<String, Integer> e : tfidf.maxTf.entrySet()) {
				tfidf.maxTfIdf.put(e.getKey(), e.getValue() * tfidf.idf.get(e.getKey()));
			}
			
			System.out.println("Total number of unique tokens: " + tfidf.totalFreq.size());
			
		}
		
	}
	
	static void accumulateMap(Iterable<String> iterable, Map<String, Integer> map) {
		for(String s : iterable) {
			Integer n = map.get(s);
			if(n == null) {
				map.put(s, 1);
			} else {
				map.put(s, 1 + n);
			}
		}
	}
	
	
	private static class KeepWordsTooSmallException extends Exception {
		public KeepWordsTooSmallException() {}
	}
	
	private static class FormatConverter {

		LinkedHashMap<String, Integer> vocab = new LinkedHashMap<String, Integer>();
		LinkedHashMap<String, Integer> labelVocab = new LinkedHashMap<String, Integer>();
		List<Article> docs;		
		Set<String>keepWords;
		Set<String>keepLabels;

		public FormatConverter(List<Article> docs, Set<String> keepWords, Set<String> keepLabels) {
			this.docs = docs;
			this.keepWords = keepWords;
			this.keepLabels = keepLabels;
		}

		public void convert(BufferedWriter docWriter, BufferedWriter vocabWriter, BufferedWriter labelWriter) throws IOException, KeepWordsTooSmallException {

			// Get vocabularies into maps. 
			int i = 0;
			int j = 0;
			int numDocsWithAllLabelsFiltered = 0;
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

				boolean allLabelsRejected = true;
				for(String l : a.meshMajor) {
					
					if(!keepLabels.contains(l)) {
						continue;
					}
					
					allLabelsRejected = false;
					Integer id = labelVocab.get(l);
					if(id == null) {
						labelVocab.put(l, j);
						id = j;
						j++;
					}
					
					docWriter.write(id + " ");
				}
				if(allLabelsRejected) {					
					numDocsWithAllLabelsFiltered++;
					// throw new KeepWordsTooSmallException();
				}
				docWriter.newLine();
				
			}

			if(numDocsWithAllLabelsFiltered > 0) {
				System.out.println("Warning. All labels for some documents have been removed.");
				System.out.println("Number of docs with all labels filtered: " + numDocsWithAllLabelsFiltered);
			}
				
			System.out.println("Successfully wrote document file. Total docs: " + docs.size());

			writeVocab(vocabWriter, vocab);
			writeVocab(labelWriter, labelVocab);

			System.out.println("Successfully wrote vocabulary files.");
		}

		
		private static void writeVocab(BufferedWriter bw, LinkedHashMap<String, Integer> vocab) throws IOException {

			for(String w : vocab.keySet()) {
				bw.write(w);
				bw.newLine();
			}
		}

		
		
	}

	static List<String> abstractWords(String abstractText) {

		String [] strings = abstractText.toLowerCase().replaceAll("[^A-Za-z0-9 ]", "").split(" ");
		List<String> abstractWords = new ArrayList<String>();
		for(String s : strings) {
			abstractWords.add(s);
		}

		return abstractWords;
	}

	

}
