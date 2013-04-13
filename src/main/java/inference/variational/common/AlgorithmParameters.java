package main.java.inference.variational.common;

public class AlgorithmParameters {
	public double tolerance;
	public int maxIter;
	public boolean saveHistory;
	
	public AlgorithmParameters(double tolerance, int maxIter, boolean saveHistory) {
		this.tolerance = tolerance;
		this.maxIter = maxIter;
		this.saveHistory = saveHistory;
	}
}
