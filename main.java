import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharArraySet;
public class main{
	public static void main(String[] args) throws IOException {
		
		int run = 0;
		// --CHOOSE WHICH PART TO RUN--
		
		//run = 1; // Pseudo-relevance feedback
		//run = 2; // Complete Link Clusterings
		run = 3; // Rocchio Relevance Feedback
	
		String cacmDocsDir = "data/cacm"; // directory containing CACM documents
		String medDocsDir = "data/med"; // directory containing MED documents
		
		String cacmIndexDir = "data/index/cacm"; // the directory where index is written into
		String medIndexDir = "data/index/med"; // the directory where index is written into
		
		String cacmQueryFile = "data/cacm_processed.query";    // CACM query file
		String cacmAnswerFile = "data/cacm_processed.rel";   // CACM relevance judgements file

		String medQueryFile = "data/med_processed.query";    // MED query file
		String medAnswerFile = "data/med_processed.rel";   // MED relevance judgements file
		
		String stopFile = "data/stopwords/stopwords_indri.txt";
		
		int cacmNumResults = 100;
		int medNumResults = 100;
		
		//--get stopwords--
		CharArraySet stopwords = new CharArraySet(0, false);
		BufferedReader br = new BufferedReader(new FileReader(stopFile));
		String line;
		while ((line = br.readLine()) != null) {
			stopwords.add(line);
		}
		br.close();
	    
		//--Build Indexes--
	    MyIndex cacmIndex = MyIndex.buildFIndex(cacmIndexDir,cacmDocsDir, stopwords);
	    MyIndex medIndex = MyIndex.buildFIndex(medIndexDir,medDocsDir, stopwords);
	    
 
	    
//CONTROL WHICH COLLECTION TO RUN ON
	    
	    // -- CACM --
	    double pCacm = evaluate(cacmIndex, cacmQueryFile, cacmAnswerFile, cacmNumResults, stopwords, run);
	    
	    // -- MEDLAR --
	    double pMed = evaluate(medIndex, medQueryFile, medAnswerFile, medNumResults, stopwords, run);
	    
	    System.out.println("atc.atc MAP CACM: " + pCacm);
	    System.out.flush();
	    System.out.println("atc.atc MAP MEDLAR: " + pMed);
	   
	    
	    System.out.println("Mini Seach Engine Complete");
	}
	
	private static double evaluate( MyIndex index, String queryFile, String answerFile,
			int numResults, CharArraySet stopwords, int run){


		// --load queries and answer--
		Map<Integer, String> queries = loadQueries(queryFile);
		Map<Integer, HashSet<String>> queryAnswers = loadAnswers(answerFile);
		
		//--Find DF for all tokens--
		Set<String> tokens = new HashSet<String>();
		for(String n : index.docNames()){
			tokens.addAll(index.getDoc(n).freqs.keySet());
		}
		HashMap<String,Integer> Tfreqs = new HashMap<String,Integer>();
		for(String token : tokens){
			int df = 0; 		           				//# of documents where token occurs
			for(String n : index.docNames()){
				if(index.getDoc(n).hasToken(token)) df++;
			}
			Tfreqs.put(token, df);
		}
		
		
		double sum = 0;
		double rsum = 0;
		int better = 0;
		int worse = 0;
		// --tokenize each query, then search for similar docs--
		for (Integer i : queries.keySet()) {
		//CHANGE i FOR SINGLE QUERY
			if (i == 3) {			   // <----
			
			List<DocNode> dVectors = null;
			
			dVectors = MySearch.searchQuery(index, queries.get(i),
					numResults, stopwords, i, Tfreqs);
			
			List<DocNode> top100 = dVectors.subList(dVectors.size()-100,dVectors.size());
			Collections.reverse(top100);
			
			List<String> results = new ArrayList<String>();
			for(DocNode doc : top100){
				results.add(doc.getName());
			}
			
			double ap = avgPrecision(queryAnswers.get(i), results);
			sum += ap;
			
			System.out.printf("\nQuery %d  ", i);
			
			if(run == 1){ //  Pseudo-relevance feedback
				
				List<DocNode> top7 = dVectors.subList(dVectors.size()-7,dVectors.size());
				Collections.reverse(top7);
				
				List<DocNode> rVectors = Rocchio.rocchio1(queries.get(i), queryAnswers, dVectors, top7, i, stopwords, Tfreqs,index);
				
				List<DocNode> new_top100 = rVectors.subList(rVectors.size()-100,rVectors.size());
				Collections.reverse(new_top100);
				
				List<String> new_results = new ArrayList<String>();
				for(DocNode doc : new_top100){
					new_results.add(doc.getName());
				}
				
				double r_ap = avgPrecision(queryAnswers.get(i), new_results);
				rsum += r_ap;
				
				
				
				System.out.println ("Rocchio Psuedo: " + new_results);
				if(r_ap > ap){
					System.out.println("query IMPROVED by " + (r_ap-ap) + " with expansion");
					better ++;				
				}
				if(r_ap < ap){
					System.out.println ("query performance DECREASED by " + -(r_ap-ap));
					worse++;
				}
			}
			
			if(run == 2){ //  Complete Link Clustering
				
				List<DocNode> top30 = dVectors.subList(dVectors.size()-30,dVectors.size());
				Collections.reverse(top30);
				List<DocNode> rest = dVectors.subList(dVectors.size()-100,dVectors.size()-30);
				Collections.reverse(rest);
				
				// Docs clustered in the form [c1,c2,c3...] where ci = [d1,d12] doc number in top30
				
				
				List<String> old_results = new ArrayList<String>();
				for(DocNode doc : top30){
					old_results.add(doc.getName());
				}
				List<String> bottom70 = new ArrayList<String>();
				for(DocNode doc : rest){
					bottom70.add(doc.getName());
				}
				
				System.out.println("before clustering: " + old_results);
				
				List<String> newtop30 = Cluster.cluster(dVectors, top30);
				List<String> new_results = new ArrayList<String>();
				
				newtop30.addAll(bottom70);
				new_results = newtop30;
				
				
				System.out.println("after clustering: " + new_results);
				
				double r_ap = avgPrecision(queryAnswers.get(i), new_results);
				rsum += r_ap;
				
				if(r_ap > ap){
					System.out.println("query IMPROVED by " + (r_ap-ap) + " with clustering");
					better ++;				
				}
				if(r_ap < ap){
					System.out.println ("query performance DECREASED by " + -(r_ap-ap)+ " with clustering");
					worse++;
				}
			}
			if(run == 3){ //  Rocchio Relevance feedback
				
				List<DocNode> top7 = dVectors.subList(dVectors.size()-7,dVectors.size());
				Collections.reverse(top7);
				
				List<DocNode> rVectors = Rocchio.rocchio3(queries.get(i), queryAnswers, dVectors, top7, i, stopwords, Tfreqs,index);
				
				List<DocNode> new_top100 = rVectors.subList(rVectors.size()-100,rVectors.size());
				Collections.reverse(new_top100);
				
				List<String> new_results = new ArrayList<String>();
				for(DocNode doc : new_top100){
					new_results.add(doc.getName());
				}
				
				double r_ap = avgPrecision(queryAnswers.get(i), new_results);
				rsum += r_ap;
					
				System.out.println ("Rocchio: " + new_results);
				if(r_ap > ap){
					System.out.println("query IMPROVED by " + (r_ap-ap) + " with expansion");
					better ++;				
				}
				if(r_ap < ap){
					System.out.println ("query performance DECREASED by " + -(r_ap-ap));
					worse++;
				}
				
				
			}
			
			} 						//<----
			
		}
		String s = null;
		
		if(run == 1){
			s = "Rocchio Psuedo";
		}
		if(run == 2){
			s = "Complete-Link Cluster";
		}
		if(run == 3){
			s = "Rocchio";
		}
		if(run != 0) System.out.println( s + " MAP: " + (rsum/queries.size()) + " " + better + " improved, " + worse + " got worse");
		return sum / queries.size();
	}
	

	private static Map<Integer, String> loadQueries(String filename) {
		HashMap<Integer, String> queryIdMap = new HashMap<Integer, String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(
					new File(filename)));
		} catch (FileNotFoundException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}

		String line;
		try {
			while ((line = in.readLine()) != null) {
				int pos = line.indexOf(',');
				queryIdMap.put(Integer.parseInt(line.substring(0, pos)), line
						.substring(pos + 1));
			}
		} catch(IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryIdMap;
	}

	private static Map<Integer, HashSet<String>> loadAnswers(String filename) {
		HashMap<Integer, HashSet<String>> queryAnswerMap = new HashMap<Integer, HashSet<String>>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(
					new File(filename)));

			String line;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split(" ");
				HashSet<String> answers = new HashSet<String>();
				for (int i = 1; i < parts.length; i++) {
					answers.add(parts[i]);
				}
				queryAnswerMap.put(Integer.parseInt(parts[0]), answers);
			}
		} catch(IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryAnswerMap;
	}

	private static double avgPrecision(HashSet<String> answers,
			List<String> results) {
		double i = 1;
		double matches = 0;
		double sum = 0;
		for (String res : results) {
			if (answers.contains(res)){
				matches++;
				sum = sum + ((matches/i));
			}
			i++;
		}
		return sum/ answers.size();
	}
	
}


