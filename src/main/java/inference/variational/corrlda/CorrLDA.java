package inference.variational.corrlda;

import inference.variational.common.AlgorithmParameters;
import inference.variational.corrlda.CorrLDAdata.Document;
import inference.variational.corrlda.ResultViewer.LabelValue;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

public class CorrLDA implements Serializable {

	CorrLDAparameters param;
	CorrLDAdata dat;
	CorrLDAstate state;
	AlgorithmParameters algorithmParameters;
	
	String vocabFilename;
	String labelFilename;
	String saveFilename;
	
	public CorrLDA() {}
	
	public CorrLDA(String corpusFilename, String vocabFilename, String labelFilename, String saveFilename, int vocabSize, int labelSize, int K, int maxIter, double tolerance) throws InputFormatException, IOException {

		this.vocabFilename = vocabFilename;
		this.labelFilename = labelFilename;
		this.saveFilename = saveFilename;
		
		List<Document> documents = readCorpusFile(corpusFilename, vocabSize, labelSize);
		Collections.shuffle(documents);
		dat = new CorrLDAdata(documents, vocabSize, labelSize);
		param = new CorrLDAparameters(K);
		algorithmParameters = new AlgorithmParameters(tolerance, maxIter, false);
		state = new CorrLDAstate(dat, param, algorithmParameters);
		
		
	}

	protected void infer() {
		
		for(int iter = 0; iter < algorithmParameters.maxIter; iter++) {
			
			state.iterate();
			if(iter % 10 == 0) {
				state.computeObjective();
				System.out.println(String.format("Iteration: %d (%9.8f)", iter, state.getObjective()));
				save();
			} else {
				System.out.println(String.format("Iteration: %d", iter));
			}
		}
		
	}
	
	public static void main(String[] args) throws IOException, InputFormatException, JSAPException { 

		SimpleJSAP jsap = new SimpleJSAP( CorrLDA.class.getName(), "Launches correspondence LDA.",
	            new Parameter[] {
	                new FlaggedOption("maxIter")
	                .setStringParser(JSAP.INTEGER_PARSER)
	                .setRequired(true)
	                .setLongFlag("maxIter")
	                .setHelp("Number of iterations"),
	                new FlaggedOption("corpus")
	                .setStringParser(JSAP.STRING_PARSER)
	                .setLongFlag("corpus")
	                .setHelp("Corpus filename"),
	                new FlaggedOption("vocabFile")
	                .setStringParser(JSAP.STRING_PARSER)
	                .setLongFlag("vocabFile")
	                .setHelp("Vocab filename"),
	                new FlaggedOption("labelFile")
	                .setStringParser(JSAP.STRING_PARSER)
	                .setLongFlag("labelFile")
	                .setHelp("Label filename"),
	                new FlaggedOption("K")
	                .setStringParser(JSAP.INTEGER_PARSER)	                
	                .setLongFlag("K")
	                .setHelp("Number of topics K"),
	                new FlaggedOption("modelFile")
	                .setStringParser(JSAP.STRING_PARSER)
	                .setLongFlag("modelFile")
	                .setHelp("Model file to load instead of starting from scratch."),
	                new FlaggedOption("tolerance")
	                .setStringParser(JSAP.DOUBLE_PARSER)	                
	                .setLongFlag("tol")
	                .setDefault("0.001")
	                .setHelp("Tolerance"),
	                new FlaggedOption("holdoutPercent")
	                .setStringParser(JSAP.DOUBLE_PARSER)	                
	                .setLongFlag("holdoutPercent")
	                .setDefault("10")
	                .setHelp("Holdout Percentage"),
	                new FlaggedOption("saveFile")
	                .setStringParser(JSAP.STRING_PARSER)
	                .setLongFlag("saveFile")
	                .setRequired(true)
	                .setHelp("File name to save model to.")
	                }
		);
		
		JSAPResult config = jsap.parse(args);    
        if ( jsap.messagePrinted() ) System.exit( 1 );
		
		Integer maxIter = config.getInt("maxIter"); 
		String corpusFilename = config.getString("corpus");
		String vocabFilename = config.getString("vocabFile");
		String labelFilename = config.getString("labelFile");
		Integer K = config.contains("K") ? config.getInt("K") : null;
		String modelFile = config.getString("modelFile");
        Double tol = config.getDouble("tolerance");
        Double holdoutFraction = (100 - config.getDouble("holdoutPercent"))/100.0;
        String saveFile = config.getString("saveFile");
        
		CorrLDA corrLDA = null;

		if(modelFile != null) {
			corrLDA = CorrLDA.load(modelFile);			
			corrLDA.algorithmParameters.maxIter = maxIter;
		} else {

			if(corpusFilename == null || vocabFilename == null || labelFilename == null || K == null) {
				throw new RuntimeException("You need to pass a corpus, vocabFile, labelFile, and K");
			}
			
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

			corrLDA = new CorrLDA(corpusFilename, vocabFilename, labelFilename, saveFile, vocabSize, labelSize, K, maxIter, tol);
		}

		
		int holdoutIndex = (int)Math.ceil(corrLDA.dat.D*holdoutFraction);
		System.out.println("holdoutIndex is " + holdoutIndex);		
		corrLDA.state.setHoldoutIndex(holdoutIndex);

		corrLDA.infer();
		corrLDA.save();

		CorrLDApredictor predictor = new CorrLDApredictor(corrLDA.state);

		ResultViewer viewer = new ResultViewer(corrLDA.vocabFilename, corrLDA.labelFilename);
		viewer.loadDictionaries();

		
		for(int docid = 1 + holdoutIndex; docid < 100 + holdoutIndex; docid++) {
			System.out.println();
			viewer.viewTrueLabels(corrLDA.dat.docs.get(docid));
			List<LabelValue> topLabels = viewer.viewLabelPrediction(predictor.labelPrediction(docid));
			viewer.viewNumberMatching(corrLDA.dat.docs.get(docid), topLabels);
		}
		
		viewer.view(corrLDA);
		
		System.out.println("Done.");
		
	}
	
	protected static CorrLDA load(String filename) {
		
		
		System.out.println("Reading from saved model file...");
		FileInputStream fis = null;
		ObjectInputStream in = null;
		CorrLDA corrLDA = null;
		try {
			fis = new FileInputStream(filename);
			in = new ObjectInputStream(fis);
			corrLDA = (CorrLDA) in.readObject();
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		
		if(corrLDA != null) {
			System.out.println("Model successfully read from file.");
		}
		return corrLDA;
	}
	
	
	protected void save() {
		
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(saveFilename);
			out = new ObjectOutputStream(fos);
			out.writeObject(this);
			out.close();
			System.out.println("CorrLDA saved.");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
	}
	
	private static List<Document> readCorpusFile(String corpusFilename, int vocabSize, int labelSize) throws InputFormatException, IOException {

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
