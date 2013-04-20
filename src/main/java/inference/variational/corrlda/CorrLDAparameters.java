package inference.variational.corrlda;

import java.io.Serializable;

import inference.variational.common.AlgorithmParameters;

public class CorrLDAparameters implements Serializable {

	protected int K;

	public CorrLDAparameters() {}
	
	public CorrLDAparameters(int K) {
		this.K = K;
	}
	
	
}
