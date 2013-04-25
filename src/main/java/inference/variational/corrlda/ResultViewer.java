package inference.variational.corrlda;

import inference.variational.corrlda.CorrLDAdata.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResultViewer {

	String vocabFilename;
	String labelFilename;
	Map<Integer, String> vocab;
	Map<Integer, String> labels;
	
	
	public ResultViewer(String vocabFilename, String labelFilename) {
		this.vocabFilename = vocabFilename;
		this.labelFilename = labelFilename;
	}
	
	public void loadDictionaries() throws IOException {
		
		vocab = new HashMap<Integer, String>();
		labels = new HashMap<Integer, String>();
		
		BufferedReader reader = null;
		reader = new BufferedReader(new FileReader(vocabFilename));
		int i=0;
		String s = reader.readLine();
		while(s != null) {
			vocab.put(i, s);
			i++;
			s = reader.readLine();
		}
		reader.close();

		reader = new BufferedReader(new FileReader(labelFilename));
		i = 0;
		s = reader.readLine();
		while(s != null) {
			labels.put(i, s);
			i++;
			s = reader.readLine();
		}
		reader.close();
		
	}
	
	public void view(CorrLDA corrLDA) {
		
		System.out.println("--- pi ---");
		double [][] pi = corrLDA.state.getBeta();
		
		ArrayList<ArrayList<LabelValue>> toSort = new ArrayList<ArrayList<LabelValue>>(); 
		for(int i=0; i < corrLDA.param.K; i++) {
			toSort.add(new ArrayList<LabelValue>());
			for(int j=0; j < corrLDA.dat.Vt; j++) {
				toSort.get(i).add(new LabelValue(labels.get(j), pi[i][j]));
			}
			
			Collections.sort(toSort.get(i));
			System.out.println("----");
			for(int j=0; j < 30; j++) {
				System.out.print(String.format("%d %7.6f %s\n", i, toSort.get(i).get(j).value, toSort.get(i).get(j).label));
			}
		}
		
	}
	
	public List<LabelValue> viewLabelPrediction(double [] dist) {
		
		List<LabelValue> list = new ArrayList<LabelValue>();
		for(int i=0; i < dist.length; i++) {
			list.add(new LabelValue(labels.get(i), dist[i]));
		}
		
		Collections.sort(list);
		List<LabelValue> topLabels = list.subList(0, 10);
		
		for(LabelValue l : topLabels) {
			System.out.println(String.format("%7.6f %s", l.value, l.label));
		}
				
		return topLabels;
		
	}
	
	public void viewNumberMatching(Document doc, List<LabelValue> prediction) {
		
		int matching = 0;		
		for(int i : doc.labels) {
			for(LabelValue l : prediction) {
				if(labels.get(i).equals(l.label)) {
					matching += 1;
				}
			}
			
		}
		System.out.println(String.format("MATCHED: %d", matching));
		
	}
	
	
	public void viewTrueLabels(Document doc) {

		System.out.println("True label ---> ");
		for(int i=0; i < doc.labels.length; i++) {
			System.out.println(labels.get( doc.labels[i] ));
		}
	}
	
	
	static class LabelValue implements Comparable<LabelValue>{
		
		String label;
		double value;
		
		public LabelValue(String label, double value) {
			this.label = label;
			this.value = value;
		}

		public int compareTo(LabelValue arg0) {
			
			if(arg0.value > this.value) {
				return 1;
			} else if(arg0.value < this.value) {
				return -1;
			}
			return 0;
		}

		
		
	}
	
}
