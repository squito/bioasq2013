package inference.variational.corrlda;

import java.io.Serializable;

public class CorrLDAparameters implements Serializable {

	protected int K;
	protected double eta;

	public CorrLDAparameters() {}
	
	public CorrLDAparameters(int K, double eta) {
		this.K = K;
		this.eta = eta;
	}
	
	
}
