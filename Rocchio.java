
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharArraySet;

public class Rocchio {

	public static List<DocNode> rocchio1(String querystring, Map<Integer, HashSet<String>> queryAnswers, 
			List<DocNode> dVectors, List<DocNode> top7, int i , CharArraySet stopwords,
			HashMap<String, Integer> tfreqs, MyIndex index) {
		
		//--SET Part 1 ROCCHIO CONSTRAINTS HERE--
		
		double A,B,K;
		A = 4;
		B = 8;
		//B = 16;
		K = 5;
		//K = 10;
		
		// 1.g)
		//A = 10;
		//B = 2;
		//K = 4;
		
		//A = 6;
		//B = 6;
		//K = 6;
		
		//--tokenize query--
		DocNode query = new DocNode(Integer.toString(i));
		Analyzer analyzer = new MyAnalyzer(stopwords);
		List<String> qTokens = MyIndex.tokenize(analyzer, querystring);
		query.wordCount = qTokens.size();
		
		// --get token frequency--
		for (String token : qTokens){
			int freq = 0;
			for (String token2 : qTokens){
				if(token.equals(token2)) freq ++;
			}
			if(!query.hasToken(token)) query.addFreq(token, freq);
		}
		List<String> reltokens = new ArrayList<String>();
		for (DocNode doc : top7){
			for(String t : doc.weights.keySet()){
				reltokens.add(t);
			}
		}
		//System.out.println("reltokens: " + reltokens);
		// --get query vector--
		DocNode qVector = MySearch.atcQ(query, index, tfreqs);
		
		HashMap<String, Double> w = qVector.weights;
		HashMap<String, Double> new_w = new HashMap<String, Double>();
		for(String t : reltokens){
			double docsum = 0;
			for (DocNode doc : top7){
				if(doc.hasToken(t)){
					docsum += doc.getWeight(t);
				}
			}
			//--ROCCHIO WEIGHTS--
			double Atermval = 0;
			if (w.containsKey(t)) Atermval = (w.get(t)*A);
			double val = Atermval + (B * (1/top7.size()) * docsum);
			new_w.put(t, val);
		}
		qVector.weights = new_w;
	
		//get terms with top K weights
		List<String> sortedT = sortHashMap(new_w);
		
		List<String> Ktokens = new ArrayList<String>();
		
		int n = 0;
		int p = 0;
		while( n < K){
			String t = sortedT.get(p);
			if (!(qTokens.contains(t))){
				Ktokens.add(t);
				n++;
			}
			p++;
		}
		System.out.println("Top " + K + " tokens added to query: " + Ktokens);
		
		//expand, reweight query
		for(String token : Ktokens){
			qTokens.add(token);
		}
		DocNode exp_query = new DocNode(Integer.toString(i));
		// --get token frequency--
		for (String token : qTokens){
			int freq = 0;
			for (String token2 : qTokens){
				if(token.equals(token2)) freq ++;
			}
			if(!exp_query.hasToken(token)) exp_query.addFreq(token, freq);
		}
		
		DocNode exp_qVector = MySearch.atcQ(exp_query, index, tfreqs);
		
		//System.out.println("original weights: " + w);
		//System.out.println("expanded weights: " + exp_qVector.weights);
	
		// --calc scores--
		for(DocNode dVector : dVectors){
			double score = 0;
			//compute dot product
			for(String qTerm : exp_qVector.weights.keySet()){
				for(String dTerm : dVector.weights.keySet()){
					if(qTerm.equals(dTerm)){
						score = score + (exp_qVector.weights.get(qTerm)*dVector.weights.get(dTerm));
					}
				}		
			}
			dVector.score = score;
		}		
		Collections.sort(dVectors, new DocComp());
		return dVectors;
	}
	
	public static List<DocNode> rocchio3(String querystring, Map<Integer, HashSet<String>> queryAnswers, 
			List<DocNode> dVectors, List<DocNode> top7, int i , CharArraySet stopwords,
			HashMap<String, Integer> tfreqs, MyIndex index) {
		
		//--SET Part 3 ROCCHIO CONSTRAINTS HERE--
		
				double A,B,C,K;
				A = 4;
				B = 8;
				//B = 16;
				C = 4;
				//C = 0;
				K = 5;
				
				//--tokenize query--
				DocNode query = new DocNode(Integer.toString(i));
				Analyzer analyzer = new MyAnalyzer(stopwords);
				List<String> qTokens = MyIndex.tokenize(analyzer, querystring);
				query.wordCount = qTokens.size();
				
				// --get token frequency--
				for (String token : qTokens){
					int freq = 0;
					for (String token2 : qTokens){
						if(token.equals(token2)) freq ++;
					}
					if(!query.hasToken(token)) query.addFreq(token, freq);
				}
				//Get top Rel and nonRel document
				HashSet<String> relDocs = queryAnswers.get(i);
				
				DocNode topRel = null;
				DocNode topNonRel = null;
				
				for (DocNode doc : top7){
					if(relDocs.contains(doc.getName()) && topRel == null){
						topRel = doc;
					}
					if( !relDocs.contains(doc.getName()) && topNonRel == null){
						topNonRel = doc;
					} 
				}
				List<String> reltokens = new ArrayList<String>();
				if(topRel != null){
					for(String t : topRel.weights.keySet()){
						reltokens.add(t);
					}
				}
				List<String> nonreltokens = new ArrayList<String>();
				if(topNonRel != null){
					for(String t : topNonRel.weights.keySet()){
						nonreltokens.add(t);
					}
				}
				// --get query vector--
				DocNode qVector = MySearch.atcQ(query, index, tfreqs);
				
				List<String> alltokens = new ArrayList<String>();
				alltokens.addAll(reltokens);
				alltokens.addAll(nonreltokens);
				
				HashMap<String, Double> w = qVector.weights;
				HashMap<String, Double> new_w = new HashMap<String, Double>();
				for(String t : alltokens){
					double docsumRel = 0;
					double docsumNonRel = 0;
					
					if(topRel != null && topRel.hasToken(t)){
						docsumRel += topRel.getWeight(t);
					}
					if( topNonRel.hasToken(t)){
						docsumNonRel += topNonRel.getWeight(t);
					}
					//--ROCCHIO WEIGHTS--
					double Atermval = 0;
					if (w.containsKey(t)) Atermval = (w.get(t)*A);
					double val = Atermval + (B * docsumRel) - (C * docsumNonRel);
					new_w.put(t, val);
				}
				qVector.weights = new_w;
				
				//get terms with top K weights
				List<String> sortedT = sortHashMap(new_w);
				
				List<String> Ktokens = new ArrayList<String>();
				int n = 0;
				int p = 0;
				while( n < K){
					String t = sortedT.get(p);
					if (!(qTokens.contains(t))){
						Ktokens.add(t);
						n++;
					}
					p++;
				}
				System.out.println("Top " + K + " tokens added to query: " + Ktokens);
				
				//expand, reweight query
				for(String token : Ktokens){
					qTokens.add(token);
				}
				DocNode exp_query = new DocNode(Integer.toString(i));
				// --get token frequency--
				for (String token : qTokens){
					int freq = 0;
					for (String token2 : qTokens){
						if(token.equals(token2)) freq ++;
					}
					if(!exp_query.hasToken(token)) exp_query.addFreq(token, freq);
				}
				
				DocNode exp_qVector = MySearch.atcQ(exp_query, index, tfreqs);
			
				//System.out.println("original weights: " + w);
				//System.out.println("expanded weights: " + exp_qVector.weights);
				
				// --calc scores--
				for(DocNode dVector : dVectors){
					double score = 0;
					//compute dot product
					for(String qTerm : exp_qVector.weights.keySet()){
						for(String dTerm : dVector.weights.keySet()){
							if(qTerm.equals(dTerm)){
								score = score + (exp_qVector.weights.get(qTerm)*dVector.weights.get(dTerm));
							}
						}			
					}
					dVector.score = score;	
				}		
				Collections.sort(dVectors, new DocComp());
				return dVectors;
	}
	
	public static List<String> sortHashMap(final HashMap<String, Double> map) {
	    Set<String> set = map.keySet();
	    List<String> keys = new ArrayList<String>(set);

	    Collections.sort(keys, new Comparator<String>() {

	        public int compare(String k1, String k2) {
				return Double.compare(map.get(k2), map.get(k1)); //reverse order
			}
	    });
	    return keys;
	}
}
