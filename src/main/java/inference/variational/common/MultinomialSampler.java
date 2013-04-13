package main.java.inference.variational.common;

import java.util.Arrays;
import java.util.Random;

public class MultinomialSampler {
	
	public static int sample(double [] p) {
	
		double r = new Random().nextDouble();
		int K = p.length;
		double [] cumsum = new double[K];
		cumsum[0] = p[0];
		for(int k=1; k < K; k++){
			cumsum[k] = cumsum[k-1] + p[k];
		}
		
		int v = Arrays.binarySearch(cumsum, r);
		v = v < 0 ? -(v+1) : v;
		return v;
		
	}

}
