package org.sharder.math;


/**
 * Sharder相关数学计算实现
 * @author x.y
 * @date 2017/12/13
 */
public class SharderMath implements SharderMathI {


	@Override
	public double retrieve(int boxerCount, int pieceCount, int replicaCount) {
		return hyp(boxerCount,pieceCount,replicaCount);
	}


	private Double approximation(Double d){
		return Double.isInfinite(d) ? Double.MAX_VALUE : d;
	}

	private Double fac(int p){
		return p == 0 ? 1  :  approximation(p * fac(p-1));
	}

	private Double choose(int h,int k){
		return fac(h) / fac(k) / fac(h-k);
	}

	private Double hyp(int b,int p,int r){
		return choose(b-p,r-p) / choose(b,r);
	}



	static private void testFAC(){
		SharderMath SharderMath = new SharderMath();
		System.out.println(SharderMath.fac(500));

	}

	static private void testRetrieve(){
		int[] bAry = new int[]{100,100,200,200,300,500,500,900,900,900,900,900,900};
		int[] pAry = new int[]{10,10,10,50,80,50,50,10,10,10,50,50,50};
		int[] rAry = new int[]{10,50,50,90,90,200,400,200,400,800,200,400,800};

		SharderMath SharderMath = new SharderMath();
		System.out.println("boxerCount" + "pieceCount" + "replicaCount" + "retrieveRatio");
		System.out.println("------------------------------------------------------------");
		for(int i = 0 ; i < bAry.length ; i++){
			System.out.println(bAry[i] + "	" + pAry[i]  + "	" + rAry[i] + "	" + SharderMath.retrieve(bAry[i],pAry[i],rAry[i]));
		}

	}

	public static void main(String[] args) {
		testRetrieve();
//		testFAC();
	}
}
