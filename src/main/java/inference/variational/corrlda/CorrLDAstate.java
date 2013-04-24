package inference.variational.corrlda;

import inference.variational.common.AlgorithmParameters;
import inference.variational.common.MatrixFunctions;
import inference.variational.common.Normalizer;
import inference.variational.corrlda.CorrLDAdata.Document;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.special.Gamma;

public class CorrLDAstate implements Serializable {

	
	// Md - number of words for doc.
	// Nd - number of labels for doc.
	double [][] gamma; // variational dirichlet. D x K
	double [] sumGamma; // D
	double[][][] phi; // variational multinomial for words.  D x Md x K
	double [][][] lambda; // variational multinomial for y.  D x Nd x Md
	double [][] pi; // parameter for labels.  K x Vs
	double [][] beta; // parameter for words. K x Vt
	double [] alpha; // dirichlet parameter for topics.  K
	double objective = Double.NEGATIVE_INFINITY; // The lower bound we are maximizing. 
	
	private CorrLDAdata dat;
	private CorrLDAparameters param;
	private AlgorithmParameters algParam;
	private Integer holdoutIndex = null;

	private static transient Random rand = new Random();
	
	public CorrLDAstate() {};
	
	public CorrLDAstate(CorrLDAstate state) {
		gamma = state.gamma;
		sumGamma = state.sumGamma;
		phi = state.phi;
		lambda = state.lambda;
		pi = state.pi;
		beta = state.beta;
		alpha = state.alpha;
		dat = state.dat;
		param = state.param;
		algParam = state.algParam;
	}
	
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

	
	public void setHoldoutIndex(int holdoutIndex) {
		this.holdoutIndex = holdoutIndex;
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
	
	
	protected void computeObjective() {
		
		double term1 = (double)dat.D * Gamma.logGamma( MatrixFunctions.sum(alpha) );

		double term2 = 0;
		for(int i=0; i < param.K; i++) {
			term2 += Gamma.logGamma(alpha[i]);
		}
		term2 *= dat.D;
		
		double term3 = 0;
		for(int d=0; d < dat.D; d++) {			
			for(int i=0; i < param.K; i++) {
				term3 += (alpha[i] - 1) * (Gamma.digamma(gamma[d][i]) - Gamma.digamma(sumGamma[d]) );
			}
		}
		
		double term4 = 0;
		for(int d=0; d < dat.D; d++) {
			for(int m=0; m < dat.Md[d]; m++) {
				for(int i=0; i < param.K; i++) {
					term4 += ( Gamma.digamma(gamma[d][i]) - Gamma.digamma(sumGamma[d]) ) * phi[d][m][i];
				}
			}
		}
		
		double term5 = 0;
		for(int d=0; d < dat.D; d++) {
			for(int m=0; m < dat.Md[d]; m++) {
				for(int i=0; i < param.K; i++) {
					if(pi[i][dat.docs.get(d).words[m]] != 0) {  // if this is true, then I believe term += 0.
						term5 += phi[d][m][i] * Math.log( pi[i][dat.docs.get(d).words[m]] );
					}
				}
			}
		}
		
		double term6 = 0;
		for(int d=0; d < holdoutIndex; d++) {
			for(int n=0; n < dat.Nd[d]; n++) {
				for(int i=0; i < param.K; i++) {
					for(int m=0; m < dat.Md[d]; m++) {
						term6 += phi[d][m][i] * lambda[d][n][m] * Math.log( beta[i][dat.docs.get(d).labels[n]] );
					}
				}
			}
		}
		
		double term7 = 0;		
		for(int d=0; d < dat.D; d++) {
			term7 += Gamma.logGamma(sumGamma[d]);
		}
		
		double term8 = 0;
		for(int d=0; d < dat.D; d++) {
			for(int i=0; i < param.K; i++) {
				term8 += Gamma.logGamma(gamma[d][i]);
			}
		}	

		double term9 = 0;
		for(int d=0; d < dat.D; d++) {
			for(int i=0; i < param.K; i++) {
				term9 += (gamma[d][i] - 1) * (Gamma.digamma(gamma[d][i]) - Gamma.digamma(sumGamma[d]));
			}
		}

		double term10 = 0;
		for(int d=0; d < dat.D; d++) {
			for(int m=0; m < dat.Md[d]; m++) {
				for(int i=0; i < param.K; i++) {
					if(phi[d][m][i] != 0) {  // limit of xlog(x) as x --> 0 is zero. 
						term10 += phi[d][m][i] * Math.log(phi[d][m][i]);
					}
				}
			}
		}

		double term11 = 0;
		for(int d=0; d < holdoutIndex; d++) {
			for(int m=0; m < dat.Md[d]; m++) {
				for(int n=0; n < dat.Nd[d]; n++) {
					term11 += lambda[d][n][m] * Math.log(lambda[d][n][m]);
				}
			}
		}
		
		System.out.println();
		System.out.println("1: " + term1);
		System.out.println("2: " + term2);
		System.out.println("3: " + term3);
		System.out.println("4: " + term4);
		System.out.println("5: " + term5);
		System.out.println("6: " + term6);
		System.out.println("7: " + term7);
		System.out.println("8: " + term8);
		System.out.println("9: " + term9);
		System.out.println("10: " + term10);
		System.out.println("11: " + term11);
		
		objective = term1 - term2 + term3 + term4 + term5 + term6 - term7 + term8 - term9 - term10 - term11;
		
	}
	
	
	private void computeBeta() {
		for(int i=0; i < param.K; i++) {
			beta[i] = new double[dat.Vt]; // Clear beta[i].
			for(int d=0; d < holdoutIndex; d++) {	
				
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
		
		for(int d=0; d < holdoutIndex; d++) {
			
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
					if(d < holdoutIndex) {
						for(int n=0; n < dat.Nd[d]; n++) {
							sm += lam[n][m] * Math.log(beta[i][doc.labels[n]]);
						}
					}
					
					try {
						phi[d][m][i] = Math.log( pi[i][doc.words[m]]) + Gamma.digamma(gam[i]) - Gamma.digamma(sumGam) + sm;
					} catch (StackOverflowError e) {
						System.out.println(" Stack overflow error. Trying to take digamma(" + gam[i] + ") and digamma(" + sumGam + ").  i = " + i );
						throw new StackOverflowError();
					}

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
				//pi[k][i] = 1.0/dat.Vs;
				pi[k][i] = rand.nextDouble();
			}
			pi[k] = Normalizer.normalize(pi[k]);
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
			alpha[k] = 1.0;
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
	
	public double getObjective() {
		return objective;
	}
	
}
