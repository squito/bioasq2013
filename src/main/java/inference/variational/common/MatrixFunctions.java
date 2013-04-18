package inference.variational.common;

public class MatrixFunctions {
	
	private MatrixFunctions(){};
	
	public enum o {
		plus, minus, times, divide;
	}
	
	public static double [] op(double [] x, o operator, double [] y) {
		int n = x.length;
		if(n != y.length) {
			throw new RuntimeException("The dimension of x and y is not the same");
		}
		double [] z = new double[n];

		switch (operator) {
		case plus:
			for (int i = 0; i < n; i++) {
				z[i] = x[i] + y[i];
			}
			break;
		case minus:
			for (int i = 0; i < n; i++) {
				z[i] = x[i] - y[i];
			}
			break;
		case times:
			for (int i = 0; i < n; i++) {
				z[i] = x[i] * y[i];
			}
			break;
		case divide:
			for (int i = 0; i < n; i++) {
				z[i] = x[i] / y[i];
			}
			break;
		}
		return z;
	}
	
	public static double [] op(double [] x, o operator, double y) {
		int n = x.length;
		double [] z = new double[n];

		switch (operator) {
		case plus:
			for (int i = 0; i < n; i++) {
				z[i] = x[i] + y;
			}
			break;
		case minus:
			for (int i = 0; i < n; i++) {
				z[i] = x[i] - y;
			}
			break;
		case times:
			for (int i = 0; i < n; i++) {
				z[i] = x[i] * y;
			}
			break;
		case divide:
			for (int i = 0; i < n; i++) {
				z[i] = x[i] / y;
			}
			break;
		}
		return z;
	}
	
	public static double [] op(double x, o operator, double [] y) {
		int n = y.length;
		double [] z = new double[n];

		switch (operator) {
		case plus:
			for (int i = 0; i < n; i++) {
				z[i] = x + y[i];
			}
			break;
		case minus:
			for (int i = 0; i < n; i++) {
				z[i] = x - y[i];
			}
			break;
		case times:
			for (int i = 0; i < n; i++) {
				z[i] = x * y[i];
			}
			break;
		case divide:
			for (int i = 0; i < n; i++) {
				z[i] = x / y[i];
			}
			break;
		}
		return z;
	}
	
	
	public static double [][] op(double [][] x, o operator, double [][] y) {
		int n = x.length;
		int m = x[0].length;
		if(n != y.length || m != y[0].length) {
			throw new RuntimeException("The dimension of x and y is not the same");
		}
		
		double [][] z = new double[n][m];

		switch (operator) {
		case plus:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x[i][j] + y[i][j];
				}
			}
			break;
		case minus:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x[i][j] - y[i][j];
				}
			}
			break;
		case times:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x[i][j] * y[i][j];
				}
			}
			break;
		case divide:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x[i][j] / y[i][j];
				}
			}
			break;
		}
		return z;
	}
	
	public static double [][] op(double [][] x, o operator, double y) {
		int n = x.length;
		int m = x[0].length;
		
		double [][] z = new double[n][m];

		switch (operator) {
		case plus:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x[i][j] + y;
				}
			}
			break;
		case minus:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x[i][j] - y;
				}
			}
			break;
		case times:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x[i][j] * y;
				}
			}
			break;
		case divide:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x[i][j] / y;
				}
			}
			break;
		}
		return z;
	}
	
	public static double [][] op(double x, o operator, double y [][]) {
		int n = y.length;
		int m = y[0].length;
		
		double [][] z = new double[n][m];

		switch (operator) {
		case plus:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x + y[i][j];
				}
			}
			break;
		case minus:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x - y[i][j];
				}
			}
			break;
		case times:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x * y[i][j];
				}
			}
			break;
		case divide:
			for (int i = 0; i < n; i++) {
				for(int j=0; j < m; j++) {
					z[i][j] = x / y[i][j];
				}
			}
			break;
		}
		return z;
	}
	
	public static double innerProduct(double [] x, double [] y) {
		
		int n = x.length;
		if(y.length != n) {
			throw new RuntimeException("x and y are not the same size");
		}
		double s = 0.0;
		for(int i=0; i < n; i++) {
			s += x[i] * y[i];
		}
		return s;
		
		
	}
	
	public static double innerProduct(double [] x, double y) {
		
		int n = x.length;
		double s = 0.0;
		for(int i=0; i < n; i++) {
			s += x[i] * y;
		}
		return s;
		
		
	}
	
	public static double innerProduct(double y, double [] x) {

		return innerProduct(x, y);
		
	}
	
	
	public static double [][] outerProduct(double [] x, double [] y) {
		
		int n = x.length;
		int m = y.length;
		
		double [][] z = new double[n][m];
		for(int i=0; i < n; i++) {
			for(int j=0; j < m; j++) {
				z[i][j] = x[i] * y[j];
			}
		}
		return z;
		
	}
	
	public static double sum(double [] x) {
		
		double s = 0.0;
		for(double v : x) {
			s += v;
		}
		return s;
	}
	
	public static double sumSquares(double [] x) {
		
		double s = 0.0;
		for(double v : x) {
			s += v * v;
		}
		return s;
		
	}
	
	public static void print(double [] vec) {
		for(int i=0; i < vec.length; i++) {
			System.out.print(vec[i] + "  ");
		}
		System.out.println();
	}
	 	
}
