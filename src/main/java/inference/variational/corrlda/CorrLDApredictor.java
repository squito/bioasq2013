package inference.variational.corrlda;

import inference.variational.common.Normalizer;

public class CorrLDApredictor {

	CorrLDAstate state;
	
	public CorrLDApredictor(CorrLDAstate state) {
		this.state = state;
	}
	
	public double[] labelPrediction(int docid) {
		
		double [][] phi = state.getPhi()[docid]; 
		double [][] beta = state.getBeta();

		int K = beta.length;
		int Vt = beta[0].length;
		int M = phi.length;
		double [] dist = new double[Vt];
		for(int k=0; k < K; k++) {
			for(int i=0; i < M; i++) {
				for(int j=0; j < Vt; j++) {
					dist[j] += phi[i][k] * beta[k][j];
				}
			}
		}
		
		return Normalizer.normalize( dist );
	}
	
}
