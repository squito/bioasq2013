package inference.variational.corrlda;

import inference.variational.common.AlgorithmParameters;
import inference.variational.corrlda.CorrLDAdata.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CorrLDA {

	CorrLDAparameters param;
	CorrLDAdata dat;
	CorrLDAstate state;
	AlgorithmParameters algorithmParameters;
	
	public CorrLDA(String corpusFilename, int vocabSize, int labelSize, int K, int maxIter, double tolerance) throws InputFormatException, IOException {

		List<Document> documents = readCorpusFile(corpusFilename, vocabSize, labelSize);
		dat = new CorrLDAdata(documents, vocabSize, labelSize);
		param = new CorrLDAparameters(K);
		algorithmParameters = new AlgorithmParameters(tolerance, maxIter, false);
		state = new CorrLDAstate(dat, param, algorithmParameters);
		
	}

	protected void infer() {
		
		for(int iter = 0; iter < algorithmParameters.maxIter; iter++) {
			System.out.println("Iteration: " + iter);
			state.iterate();
		}
		
	}
	
	
	public static void main(String[] args) throws IOException, InputFormatException { 

		if(args.length != 5) {
			throw new RuntimeException("You need five arguments.");
		}
		
		String corpusFilename = args[0];
		String vocabFilename = args[1];
		String labelFilename = args[2];
		int K = Integer.parseInt( args[3] );
		int maxIter = Integer.parseInt( args[4] );
		int vocabSize = 0;
		int labelSize = 0;

		BufferedReader reader = null;
		reader = new BufferedReader(new FileReader(vocabFilename));
		while(reader.readLine() != null) {
			vocabSize++;
		}
		reader.close();

		reader = new BufferedReader(new FileReader(labelFilename));
		while(reader.readLine() != null) {
			labelSize++;
		}
		reader.close();

		CorrLDA corrLDA = new CorrLDA(corpusFilename, vocabSize, labelSize, K, maxIter, 0.001);
		corrLDA.infer();
		
		
		System.out.println("Done.");
		
	}
	
	
	private List<Document> readCorpusFile(String corpusFilename, int vocabSize, int labelSize) throws InputFormatException, IOException {

		List<Document> documents = new ArrayList<Document>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(corpusFilename));

			String wordLine = br.readLine();

			while(wordLine != null) {

				String labelLine = br.readLine();
				if(labelLine == null) {
					throw new InputFormatException("There were an odd number of lines in the file. This file does not have the right format.");		
				}

				String [] stringWords = wordLine.split("\\s+"); 			
				int [] words = new int[stringWords.length];
				for(int i=0; i < stringWords.length; i++) {
					words[i] = Integer.parseInt(stringWords[i]);
							
					if(words[i] >= vocabSize) {
						throw new RuntimeException("You have a word in your document that has index bigger than the vocabulary.");
					}
				}

				String [] stringLabels = labelLine.split("\\s+");
				int [] labels = new int[stringLabels.length];
				for(int i=0; i < stringLabels.length; i++) {
					labels[i] = Integer.parseInt(stringLabels[i]);
					
					if(labels[i] >= labelSize) {
						System.out.println("i: " + i + " labels[i]: " + labels[i] + " labelSize: " + labelSize);
						throw new RuntimeException("You have a label in your document that has index bigger than the number of labels you know about.");
					}
				}

				// Add to documents.
				documents.add(new Document(words, labels));

				// Read next line. 
				wordLine = br.readLine();

			}

		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		} finally {
			if(br != null) {
				br.close();
			}
		}

		return documents;

	}
	
	static class InputFormatException extends Exception {
		public InputFormatException(String message) {
			super(message);
		}
	}
	
}
