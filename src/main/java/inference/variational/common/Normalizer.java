package main.java.inference.variational.common;

import main.java.inference.variational.common.MatrixFunctions.o;


public class Normalizer {

	public static double[] normalizeFromLog(double [] y) {
		
		// Get max value.
		double mx = Double.NEGATIVE_INFINITY;
		for(int i=0; i < y.length; i++) {
			if(y[i] > mx) {
				mx = y[i];
			}
		}
		
		//Subtract off max value and take exponent.
		double [] r	= new double[y.length];
		double sum = 0.0;
		for(int i=0; i < y.length; i++) {
			r[i] = Math.exp(y[i] - mx);
			sum += r[i];
		}
		
		// Normalize.
		for(int i=0; i < y.length; i++) {
			r[i] = r[i]/sum;
		}

		return r;
	}

	public static double[] normalize(double [] y) {
		double sum = MatrixFunctions.sum(y);
		double [] r = MatrixFunctions.op(y, o.divide, sum);		
		return r;
 	}
	
	public static double[] normalizeToUnitNorm(double [] y) {
		double sumSquares = MatrixFunctions.sumSquares(y);
		double [] r = MatrixFunctions.op(y, o.divide, Math.sqrt(sumSquares));		
		return r;
	}
	
	
}
