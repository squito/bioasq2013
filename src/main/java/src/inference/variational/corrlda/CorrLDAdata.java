package inference.variational.corrlda;

import java.util.List;

public class CorrLDAdata {

	public List<Document> docs;
	int vocabSize;
	int labelSize;
	
	// Number of documents.
	int D;   
	int [] Mword;
	int [] Mlabel;
	
	public CorrLDAdata(List<Document> docs, int vocabSize, int labelSize) {
		this.vocabSize = vocabSize;
		this.labelSize = labelSize;
		
		D = docs.size();
		Mword = new int[D];
		for(int i=0; i < D; i++) {
			Mword[i] = docs.get(i).words.length;
			Mlabel[i] = docs.get(i).labels.length;
		}
		
	}
	
	public static class Document {
		public int [] words;
		public int [] labels;
		public Document(int [] words, int [] labels) {
			this.words = words;
			this.labels = labels;
		}
	}
	
}
