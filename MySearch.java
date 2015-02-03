import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharArraySet;

public class MySearch {

	private MySearch() {}
	
	public static List<DocNode> searchQuery(MyIndex index,String queryString, 
			int numResults, CharArraySet stopwords, int i,HashMap<String,Integer> Tfreqs) {
		
		
		
		//--tokenize query--
		DocNode query = new DocNode(Integer.toString(i));
		Analyzer analyzer = new MyAnalyzer(stopwords);
		List<String> qTokens = MyIndex.tokenize(analyzer , queryString);
		query.wordCount = qTokens.size();
		
		// --get token frequency--
		for (String token : qTokens){
			int freq = 0;
			for (String token2 : qTokens){
				if(token.equals(token2)) freq ++;
			}
			if(!query.hasToken(token)) query.addFreq(token, freq);
		}
		
		// --get query vector--
		
		DocNode qVector = atcQ(query, index, Tfreqs);    //CHANGE SMART NOTAION FOR QUERY HERE
		
		
		//--get document vectors--
		
		List<DocNode> dVectors = atcD(index, Tfreqs); 	 //AND FOR DOCUMENT HERE
		
		// --calc scores--
		for(DocNode dVector : dVectors){
			double score = 0;
			//compute dot product
			for(String qTerm : qVector.weights.keySet()){
				for(String dTerm : dVector.weights.keySet()){
					if(qTerm.equals(dTerm)){
						score = score + (qVector.weights.get(qTerm)*dVector.weights.get(dTerm));
					}
				}
				
			}
			dVector.score = score;
			
		}
		
		
		Collections.sort(dVectors, new DocComp());
		
		return dVectors;
		
	}

	public static DocNode atcQ(DocNode query, MyIndex index, HashMap<String,Integer> Tfreqs) {
		
		for(String token : query.freqs.keySet()){
			int df;
			if(Tfreqs.containsKey(token)) df = Tfreqs.get(token);
			else df = 0;
			
			double idf;
			if(df != 0) idf = Math.log(index.docNames().size()/df);
			else idf = 1;
			
			double tf = query.freqs.get(token);
			double aug_tf = 0.5 + (tf/query.maxTf());
			query.addWeight(token, (idf*aug_tf));
		}
		
		query.normalize();
		return query;
	}

		

	public static List<DocNode> atcD(MyIndex index, HashMap<String,Integer> Tfreqs) {
		List<DocNode> res = new ArrayList<DocNode>();
		for(String n : index.docNames()){
			DocNode doc = index.getDoc(n);
			for(String token : doc.freqs.keySet()){
				double tf = doc.freqs.get(token);
				double aug_tf;
				if(tf==0){
					aug_tf = 0;
				}
				else{
					aug_tf = .5 + (.5*(tf/doc.maxTf()));
				}
				int df = Tfreqs.get(token);
				double idf = Math.log(index.docNames().size()/df);
				doc.addWeight(token, aug_tf*idf);
			}
			
			doc.normalize();
			res.add(doc);
	
		}
		return res;
	}
	
		
}

