package main.java.inference.variational.corrlda;

import main.java.inference.variational.common.AlgorithmParameters;

import java.util.ArrayList;
import java.util.List;

public class CorrLDAstate {

	List<double []> gamma; // variational dirichlet.
	List<List<double []>> phi; // variational multinomial for words.
	List<List<double []>> lambda; // variational multinomial for y. 
	double [][] pi; // parameter for labels.
	double [][] beta; // parameter for words.
	double [] alpha; // dirichlet parameter for topics.

	private CorrLDAdata dat;
	private CorrLDAparameters param;
	private AlgorithmParameters algParam;
	
	public CorrLDAstate(CorrLDAdata dat, CorrLDAparameters param, AlgorithmParameters algorithmParameters) {
		this.dat = dat;
		this.param = param;
		this.algParam = algorithmParameters;
		
		// M - number of words for doc.
		// N - number of labels for doc.
		initializeAlpha(); // K
		initializeGamma(); // K
		initializePhi();  // MxK
		initializeLambda(); // NxM
		initializePi(); // KxVs
		initializeBeta(); // KxVt
		
	}

	public void iterate() {
		
		computeAlpha();
		computeGamma();
		computePhi();
		computeLambda();
		computePi();
		computeBeta();
		
	}
	
	
	private void computeBeta() {
		// TODO Auto-generated method stub
		
	}

	private void computePi() {
		for(int i=0; i < param.K; i++) {
			for(int j=0; j < dat.vocabSize; j++) {
				pi[i][j] = 0;
				for(int d=0; d < dat.D; d++) {					
					for(int m=0; m < dat.Mword[d]; m++) {
						pi[i][j] += phi.get(d).get(m)[i];
					}
				}
			}
		}
	}

	private void computeLambda() {
		// TODO Auto-generated method stub
		
	}

	private void computePhi() {
		// TODO Auto-generated method stub
		
	}

	private void computeGamma() {
		// TODO Auto-generated method stub
		
	}

	private void computeAlpha() {
		// TODO Auto-generated method stub
		
	}

	private void initializeGamma() {
		gamma = new ArrayList<double []>();
		for(int d=0; d < dat.D; d++) {
			gamma.add(new double[param.K]);
			for(int k=0; k < param.K; k++) {
				gamma.get(d)[k] = alpha[k] + dat.Mword[d]/param.K;
			}
		}
	}
	private void initializePhi() {
		phi = new ArrayList<List<double []>>();
		for(int d=0; d < dat.D; d++) {
			phi.add(new ArrayList<double []>());
			for(int i=0; i < dat.Mword[d]; i++) {
				phi.get(d).add(new double[param.K]);
				for(int k=0; k < param.K; k++) {
					phi.get(d).get(i)[k] = 1.0/param.K;
				}
			}
		}
	}
	private void initializeLambda() {
		lambda = new ArrayList<List<double []>>();
		for(int d=0; d < dat.D; d++) {
			lambda.add(new ArrayList<double []>());
			for(int i=0; i < dat.Mlabel[d]; i++) {
				lambda.get(d).add(new double[dat.Mword[i]]);
				for(int j=0; j < dat.Mword[i]; j++) {
					lambda.get(d).get(i)[j] = 1.0;  //TODO what's the right initialization here?
				}
			}
			
		}
	}
	private void initializePi() {
		pi = new double[param.K][dat.vocabSize];
		for(int k=0; k < param.K; k++) {
			for(int i=0; i < dat.vocabSize; i++) {
				pi[k][i] = 1.0/dat.vocabSize;
			}
		}
	}
	private void initializeBeta() {
		beta = new double[param.K][dat.labelSize];
		for(int k=0; k < param.K; k++) {
			for(int i=0; i < dat.labelSize; i++) {
				beta[k][i] = 1.0/dat.labelSize;
			}
		}
	}
	private void initializeAlpha() {
		alpha = new double[param.K];
		for(int k=0; k < param.K; k++) {
			alpha[k] = 1.0/param.K;
		}
	}
	
	
}
