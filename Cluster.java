import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Cluster {
	public static List<String> cluster(List<DocNode> dVectors,
			List<DocNode> top30) {
		
	//--Set K value here--
		int K = 20; 
		//int K = 10;
		
	//--Set similarity measure--
		String sim = "max";
		//String sim = "avg";
		
		
		double dist[][] = new double[30][30];
		List<List<Integer>> clusters = new ArrayList<List<Integer>>();
		int i = 0;
		while(i < 30){
			List<Integer> cluster_i = new ArrayList<Integer>();
			cluster_i.add(i+1);
			clusters.add(cluster_i);
			
			// get distances between docs
			int j = 0;
			while(j < 30){
				if (i < j){
					double dij = dot(top30.get(i),top30.get(j));
					if(dij != 0)dist[i][j] = (1/dij);
					else dist[i][j] = 0;
				}j++;
			}i++;
		}
		
		//-Perform Clustering--
		while(clusters.size() > K){
			
			double min = Double.POSITIVE_INFINITY;
			List<List<Integer>> nextClust = new ArrayList();
			
			for(List c1 : clusters){
				for (List c2 : clusters){
					if (!c1.equals(c2)){
						double val = 0;
						//if(sim == 'max') val = clustcompMAX(c1,c2,dist);
						if(sim == "avg") val = clustcompAVG(c1,c2,dist);
						
						if (val < min){
							min = val;
							nextClust.clear();
							nextClust.add(c1);
							nextClust.add(c2);
						}
					}
				}
			}
			List newClust = new ArrayList();
			for(List c : nextClust){
				clusters.remove(c);
				newClust.addAll(c);
			}
			clusters.add(newClust);
		}
		//System.out.println("clusters: " + clusters);
		//Reorder top 30
		List<String> new30 = new ArrayList<String>();
		i = 1;
		while(i <= K){
			for(List<Integer> c : clusters){
				if(c.contains(i)){
					//add all from same cluster
					for(Integer x : c){
						String name = top30.get(x-1).getName();
						if(!new30.contains(name)) new30.add(name);
					}
				}
			}i++;
		}
		
		return new30;
	}
	
	private static double clustcompMAX(List<Integer> c1, List<Integer> c2, double[][] dist) {
		
		double max = Double.NEGATIVE_INFINITY;
		
		for(int d1 : c1){
			for(int d2 : c2){
				double val;
				if (d1<d2) val = dist[d1-1][d2-1];
				else val = dist[d2-1][d1-1];
				
				max = Math.max(max, val);
			}
		}
		
		return max;
	}
	
	private static double clustcompAVG(List<Integer> c1, List<Integer> c2, double[][] dist) {
		
		double sum = 0;
		double i = 0;
		for(int d1 : c1){
			for(int d2 : c2){
				double val;
				if (d1<d2) val = dist[d1-1][d2-1];
				else val = dist[d2-1][d1-1];
				i++;
				sum += val;
			}
		}
		
		return sum/i;
	}

	private static double dot(DocNode d1, DocNode d2) {
		
		HashMap<String,Double> w1 = d1.weights;
		HashMap<String,Double> w2 = d2.weights;
		
		double sum = 0;
		for (String i : w1.keySet()){
			for(String j : w2.keySet()){
				if(i.equals(j)){
					double val = (w1.get(i)*w2.get(j));
					sum = sum + val;
				}
			}
		}
		return sum;
	}
	
}
