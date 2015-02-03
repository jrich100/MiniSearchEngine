import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

class DocNode{
		private String docname;
		public HashMap<String, Integer> freqs;
		public HashMap<String, Double> weights;
		public int wordCount;
		
		
		public double tf;
		public double score;
		
		public DocNode(String name){
			docname = name;
			freqs = new HashMap<String, Integer>();
			weights = new HashMap<String, Double>();
			wordCount = 0;
		}
		
		
		public void setName(String n){
			docname = n;
		}
		
		public String getName(){
			return docname;
		}
			
		public void addFreq(String word, Integer f){
			freqs.put(word, f);
		}
		
		public void addWeight(String word, double d){
			weights.put(word, d);
		}
		
		public double getWeight(String word){
			return weights.get(word);
		}

		public boolean hasToken(String token) {
			return freqs.containsKey(token);
		}

		public void normalize() {
			HashMap<String, Double> new_w = new HashMap<String, Double>();
			Set<String> keys = weights.keySet();
			double sum = 0;
			for(String key : keys){
				double val = weights.get(key);
				sum = sum + (val * val);
			}
			double len = Math.sqrt(sum);
			//Divide all weights by len
			for(String key : keys){
				double val = weights.get(key);
				new_w.put(key, val/len);
			}
			weights = new_w;
		}
		
		public double maxTf(){
			double max_tf = 0;
			for(Integer i : freqs.values()){
				if(i>max_tf) max_tf = i;
			}
			return max_tf;
		}

		public double sumW() {
			double sum=0;
			for(Double w : weights.values()){
				sum = sum + w;
			}
			
			return sum;
		}
		
		public double numWords() {
			double sum=0;
			for(Double w : weights.values()){
				sum = sum + w;
			}
			
			return sum;
		}
		
	}
