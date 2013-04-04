package dataingest;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.Gson;

/**
 * Convert the Bioasq json file into lda-c format described here:
 * http://www.cs.princeton.edu/~blei/lda-c/readme.txt
 * 
 * args[0] = json file.
 * args[1] = filename to write documents in lda-c format to.
 * args[2] = filename to write vocabulary list to.
 * args[3] = filename to write label list to.
 * args[4] = how many documents to read. 
 * 
 * @author kassak
 *
 */
public class JsonToLdaC {

	
	static HashSet<String> stopwords = new HashSet<String>();
	static {
		String commaSepStopwords = "a,able,about,across,after,all,almost,also,am,among,an,and,any,are,as,at,be,because,been,but,by,can,cannot,could,dear,did,do,does,either,else,ever,every,for,from,get,got,had,has,have,he,her,hers,him,his,how,however,i,if,in,into,is,it,its,just,least,let,like,likely,may,me,might,most,must,my,neither,no,nor,not,of,off,often,on,only,or,other,our,own,rather,said,say,says,she,should,since,so,some,than,that,the,their,them,then,there,these,they,this,tis,to,too,twas,us,wants,was,we,were,what,when,where,which,while,who,whom,why,will,with,would,yet,you,your";
		String [] sw = commaSepStopwords.split(",");
		for(String s : sw) {
			stopwords.add(s);
		}
	}
		
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int numberArticlesToRead = Integer.parseInt(args[4]);
		Gson gson = new Gson();
		BufferedWriter bw = null;
		BufferedWriter vocabWriter = null;
		BufferedWriter labelWriter = null;
		BufferedReader reader = null;
		
		Documents documents = new Documents();
		
		try {
			reader = new BufferedReader(new FileReader(args[0]));
			
			reader.readLine(); // Skip very first line. 
			for(int i=0; i < numberArticlesToRead; i++) {
				String line = reader.readLine();
				if(line == null || line.equals("")) {
					System.out.println("End of input reached, " + i + " documents successfully read" );
					break;
				}
				
				documents.articles.add( gson.fromJson(line.replaceAll(",$", ""), Article.class) );
				
				if(i % 10000 == 0) {
					System.out.println("Read " + i + " of " + numberArticlesToRead);
				}
			}
			
			bw = new BufferedWriter(new FileWriter(args[1]));
			vocabWriter = new BufferedWriter(new FileWriter(args[2]));
			labelWriter = new BufferedWriter(new FileWriter(args[3]));			
			FormatConverter converter = new FormatConverter(documents);
			converter.convert(bw, vocabWriter, labelWriter);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
            //Close the BufferedWriters and BufferedReaders
            try {
            	if (reader != null) {
            		reader.close();
            	}
                if (bw != null) { 
                    bw.flush();
                    bw.close();
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
		
		System.out.println("Successfully wrote files");
		
	}

	// The classes below define the java object representation of the json. 
	private static class Documents {
		public List<Article> articles = new ArrayList<Article>();
	
		public void print() {
			
			for(Article a : articles) {
				System.out.println();
				a.print();
			}
			
		}
	
	}
	
	private static class Article {

		String abstractText;
		String journal;
		List<String> meshMajor;
		Long pmid;
		String title;
		String year;
		
		public void print() {
			System.out.println("abstractText: " + abstractText);
			System.out.println("journal: " + journal);
			System.out.println("meshMajor:");
			for(String s : meshMajor) {
				System.out.println("    " + s);
			}
			System.out.println("pmid: " + pmid);
			System.out.println("title: " + title);
			System.out.println("year: " + year);
		}
		
	}
	
	// Main class that converts into lda-c form
	private static class FormatConverter {
		
		LinkedHashMap<String, Integer> vocab = new LinkedHashMap<String, Integer>();
		LinkedHashMap<String, Integer> labelVocab = new LinkedHashMap<String, Integer>();
		Documents docs;		
		
		public FormatConverter(Documents docs) {
			this.docs = docs;
		}
		
		public void convert(BufferedWriter bw, BufferedWriter vocabWriter, BufferedWriter labelWriter) throws IOException {
			
			// Get vocabularies into maps. 
			int i = 0;
			int j = 0;
			for(Article a : docs.articles) {
				
				List<String> abstractWords = abstractWords(a.abstractText);
				for(String w : abstractWords) {
					
					if(stopwords.contains(w) || w.length() < 2) {
						continue;
					}
					
					Integer id = vocab.get(w);
					if(id == null) {
						vocab.put(w,  i);
						i++;
					}
				}
				
				List<String> labelWords = labelWords(a.meshMajor);
				for(String l : labelWords) {
					Integer id = labelVocab.get(l);
					if(id == null) {
						labelVocab.put(l, j);
						j++;
					}
				}
				
			}
			
			System.out.println("Successfully identified vocabulary and labels");
			
			// Now we know how many regular vocabulary words there will be, so 
			// we need to renumber the label vocab. 
			int Nvocab = vocab.size();
			for(String l : labelVocab.keySet()) {
				labelVocab.put(l, labelVocab.get(l) + Nvocab);
			}
			
			// Write out the documents in the lda-c format.
			// We do need to iterate through the list of documents again. 
			for(Article a : docs.articles) {
				bw.write(ldaC(vocab, labelVocab, abstractWords(a.abstractText), labelWords(a.meshMajor))) ;
				bw.newLine();
			}
			
			writeVocab(vocabWriter, vocab);
			writeVocab(labelWriter, labelVocab);
			
		}
		
		private List<String> abstractWords(String abstractText) {
			
			String [] strings = abstractText.toLowerCase().replaceAll("[^A-Za-z0-9 ]", "").split(" ");
			List<String> abstractWords = new ArrayList<String>();
			for(String s : strings) {
				abstractWords.add(s);
			}
			
			return abstractWords;
		}
		
		private List<String> labelWords(List<String> meshMajor) {
			
			List<String> newWords = new ArrayList<String>();
			for(String s : meshMajor) {
				s = "label:" + s;
				newWords.add(s);
			}
			return newWords;
		}
		
	
		private String ldaC(HashMap<String, Integer> vocab, HashMap<String, Integer> labelVocab, List<String> words, List<String> labels) {
		
			String s = words.size() + " ";
			for(String w : words) {
				Integer id = vocab.get(w);
				if(id != null) {
					s += vocab.get(w) + ":1 "; 
				}
			}
			for(String l : labels) {
				Integer id = labelVocab.get(l);
				if(id != null) {
					s += labelVocab.get(l) + ":1 ";
				} else {
					throw new RuntimeException("There was a label that wasn't part of the label vocabulary");
				}
			}
			s.trim();
			return s;
		}

		private void writeVocab(BufferedWriter bw, LinkedHashMap<String, Integer> vocab) throws IOException {

			for(String w : vocab.keySet()) {
				bw.write(w);
				bw.newLine();
			}
		}
		
	}

}
