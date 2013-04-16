package inference.variational.corrlda;

import inference.variational.common.AlgorithmParameters;
import inference.variational.common.Normalizer;
import inference.variational.corrlda.CorrLDAdata.Document;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.special.Gamma;

public class CorrLDAstate {

	
	// Md - number of words for doc.
	// Nd - number of labels for doc.
	double [][] gamma; // variational dirichlet. D x K
	double [] sumGamma; // D
	double[][][] phi; // variational multinomial for words.  D x Md x K
	double [][][] lambda; // variational multinomial for y.  D x Nd x Md
	double [][] pi; // parameter for labels.  K x Vs
	double [][] beta; // parameter for words. K x Vt
	double [] alpha; // dirichlet parameter for topics.  K

	private CorrLDAdata dat;
	private CorrLDAparameters param;
	private AlgorithmParameters algParam;

	public CorrLDAstate(CorrLDAdata dat, CorrLDAparameters param, AlgorithmParameters algorithmParameters) {
		this.dat = dat;
		this.param = param;
		this.algParam = algorithmParameters;

		initializeAlpha(); 
		initializePi();
		initializeBeta();
		
		initializeGamma();
		initializePhi();
		initializeLambda();
		
	}

	public void iterate() {

		// E-step		
		computePhi();
		computeLambda();
		computeGamma();
		
		// M-step
		computeAlpha();
		computePi();
		computeBeta();
		
	}
	
	
	private void computeBeta() {
		for(int i=0; i < param.K; i++) {
			beta[i] = new double[dat.Vt]; // Clear beta[i].
			for(int d=0; d < dat.D; d++) {	

				Document doc = dat.docs.get(d);
				double [][] lam = lambda[d];
				
				for(int n=0; n < dat.Nd[d]; n++) {
					for(int m=0; m < dat.Md[d]; m++) {
						beta[i][doc.labels[n]] += phi[d][m][i] * lam[n][m];
					}
				}
			}
			beta[i] = Normalizer.normalize(beta[i]);
		}
		
	}

	private void computePi() {
		
		for(int i=0; i < param.K; i++) {
			pi[i] = new double[dat.Vs];  // Clear pi[i].
			for(int d=0; d < dat.D; d++) {					
				for(int m=0; m < dat.Md[d]; m++) {
					pi[i][dat.docs.get(d).words[m]] += phi[d][m][i];
				}
			}
			pi[i] = Normalizer.normalize(pi[i]);
		}
		
	}

	private void computeLambda() {
		
		for(int d=0; d < dat.D; d++) {
			
			Document doc = dat.docs.get(d);
			
			for(int n=0; n < dat.Nd[d]; n++) {
				for(int m=0; m < dat.Md[d]; m++) {
					
					double sm = 0;
					for(int i=0; i < param.K; i++) {
						sm += phi[d][m][i] * Math.log(beta[i][doc.labels[n]]);
					}
					
					lambda[d][n][m] = sm;
					
				}
				
				lambda[d][n] = Normalizer.normalizeFromLog(lambda[d][n]);
			}
		}
	}

	private void computePhi() {
				
		for(int d=0; d < dat.D; d++) {		
			
			Document doc = dat.docs.get(d);
			double [] gam = gamma[d];
			double [][] lam = lambda[d];
			double sumGam = sumGamma[d];
			
			for(int m=0; m < dat.Md[d]; m++) {
				for(int i=0; i < param.K; i++) {
					
					double sm = 0;
					for(int n=0; n < dat.Nd[d]; n++) {
						sm += lam[n][m] * Math.log(beta[i][doc.labels[n]]);
					}
					
					phi[d][m][i] = Math.log( pi[i][doc.words[m]]) + Gamma.digamma(gam[i]) - Gamma.digamma(sumGam) + sm;

				}

				phi[d][m] = Normalizer.normalizeFromLog(phi[d][m]);
			}
		}
	}

	private void computeGamma() {
		
		sumGamma = new double[dat.D];
		for(int d=0; d < dat.D; d++) {
			
			for(int i=0; i < param.K; i++) {
				
				double sm = 0;
				for(int m=0; m < dat.Md[d]; m++) {
					sm += phi[d][m][i];
				}
				
				gamma[d][i] = alpha[i] + sm;
				sumGamma[d] += gamma[d][i];
			}
			
		}
		
	}

	private void computeAlpha() {
		// TODO Auto-generated method stub
		
	}

	private void initializeGamma() {
		gamma = new double[dat.D][];
		sumGamma = new double[dat.D];
		for(int d=0; d < dat.D; d++) {
			gamma[d] = new double[param.K];
			for(int k=0; k < param.K; k++) {
				gamma[d][k] = alpha[k] + dat.Md[d]/param.K;
				sumGamma[d] += gamma[d][k];
			}
		}
	}
	private void initializePhi() {
		phi = new double[dat.D][][];
		for(int d=0; d < dat.D; d++) {
			phi[d] = new double[dat.Md[d]][];			
			for(int i=0; i < dat.Md[d]; i++) {
				phi[d][i] = new double[param.K];
				for(int k=0; k < param.K; k++) {
					phi[d][i][k] = 1.0/param.K;
				}
			}
		}
	}
	private void initializeLambda() {
		lambda = new double[dat.D][][];
		for(int d=0; d < dat.D; d++) {
			lambda[d] = new double[dat.Nd[d]][];
			for(int i=0; i < dat.Nd[d]; i++) {
				lambda[d][i] = new double[dat.Md[d]];
				for(int j=0; j < dat.Md[d]; j++) {
					lambda[d][i][j] = 1.0/dat.Md[d];  
				}
			}
			
		}
	}
	private void initializePi() {
		pi = new double[param.K][dat.Vs];
		for(int k=0; k < param.K; k++) {
			for(int i=0; i < dat.Vs; i++) {
				pi[k][i] = 1.0/dat.Vs;
			}
		}
	}
	private void initializeBeta() {
		beta = new double[param.K][dat.Vt];
		for(int k=0; k < param.K; k++) {
			for(int i=0; i < dat.Vt; i++) {
				beta[k][i] = 1.0/dat.Vt;
			}
		}
	}
	private void initializeAlpha() {
		alpha = new double[param.K];
		for(int k=0; k < param.K; k++) {
			alpha[k] = 1.0/param.K;
		}
	}
	
	public double [][] getGamma() {
		return gamma;
	}
	
	public double [][][] getPhi() {
		return phi;
	}
	
	public double [][][] getLambda() {
		return lambda;
	}
	
	public double [][] getPi() {
		return pi;
	}
	
	public double [][] getBeta() {
		return beta;
	}
	
	public double [] getAlpha() {
		return alpha;
	}
	
	
}
