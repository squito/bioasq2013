package inference.variational.common;

import java.io.Serializable;

public class AlgorithmParameters implements Serializable {
	public double tolerance;
	public int maxIter;
	public boolean saveHistory;
	
	public AlgorithmParameters() {}
	
	public AlgorithmParameters(double tolerance, int maxIter, boolean saveHistory) {
		this.tolerance = tolerance;
		this.maxIter = maxIter;
		this.saveHistory = saveHistory;
	}
}
