package main.java.inference.variational.corrlda;

import java.util.List;

public class CorrLDAdata {

	public List<Document> docs;
	int Vs;  // Number of vocab words total. 
	int Vt;  // Number of labels total.
	
	// Number of documents.
	int D;   
	int [] Md;
	int [] Nd;
	
	public CorrLDAdata(List<Document> docs, int vocabSize, int labelSize) {
		this.Vs = vocabSize;
		this.Vt = labelSize;
		this.docs = docs;
		
		D = docs.size();
		Md = new int[D];
		Nd = new int[D];
		for(int i=0; i < D; i++) {
			Md[i] = docs.get(i).words.length;
			Nd[i] = docs.get(i).labels.length;
		}

		System.out.println(this);
		
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("D : " + D);
		return s.toString();
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
