package inference.variational.corrlda;

import java.io.Serializable;

public class CorrLDAparameters implements Serializable {

	protected int K;

	public CorrLDAparameters() {}
	
	public CorrLDAparameters(int K) {
		this.K = K;
	}
	
	
}
