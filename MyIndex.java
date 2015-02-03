import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;

// import org.apache.lucene.util.Version;


public class MyIndex {
	
	
	//private List<DocNode> index;
	private HashMap<String,DocNode> index;
	private String name;
	private List<String> docNames;
	
	
	// constructor
	public MyIndex(String n) {
		name = n;
		//index = new ArrayList<DocNode>();
		index = new HashMap<String,DocNode>();
		docNames = new ArrayList<String>();
	}
	
	public void setName(String n){
		name = n;
	}
	
	public String getName(){
		return name;
	}
	
	public void addDoc(DocNode doc) {
		index.put(doc.getName(),doc);
		docNames.add(doc.getName());
	}
	
	public DocNode getDoc(String name){
		return index.get(name);
	}
	
	public List<String> docNames(){
		return docNames; 
	}
	
	public int size(){
		return index.size();
	}
	
	/** Index all text files under a directory. 
	 * @throws FileNotFoundException */
	public static MyIndex buildFIndex(String indexPath,String docsPath, CharArraySet stopwords) throws FileNotFoundException {
		MyIndex index = new MyIndex(docsPath.substring(5));
		
		// --Check whether docsPath is valid--
		if (docsPath == null || docsPath.isEmpty()) {
			System.err.println("Document directory cannot be null");
			System.exit(1);
		}
		// --Check whether the directory is readable--
		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		Date start = new Date();
		System.out.println("Indexing "+ docsPath + "...");

		Analyzer analyzer = new MyAnalyzer(stopwords);
		
		//--get file list--
		File[] fList = docDir.listFiles();
		
		int i = 0;
		//--get tokens for each file--
		for (File file : fList){
			DocNode doc = new DocNode(file.getName().substring(0,file.getName().length() -4));
			
			String str = new Scanner(file).useDelimiter("\\A").next();
			List<String> tokens =tokenize(analyzer, str);
			doc.wordCount = tokens.size();
			// --get token frequency--
			for (String token : tokens){
				int freq = 0;
				for (String token2 : tokens){
					if(token.equals(token2)) freq ++;
				}
				if(!doc.hasToken(token)) doc.addFreq(token, freq);
			}
			i++;
			index.addDoc(doc);
		}

		Date end = new Date();
		System.out.println("Done Indexing: " + (end.getTime() - start.getTime()) + " total milliseconds");
		return index;	
	}

	public static List<String> tokenize(Analyzer analyzer, String doc) {
	    List<String> res = new ArrayList<String>();
	    try {
	      TokenStream str  = analyzer.tokenStream(null, new StringReader(doc));
	      str.reset();
	      while (str.incrementToken()) {
	        res.add(str.getAttribute(CharTermAttribute.class).toString());
	      }
	      str.close();
	    } catch (IOException e) {
	      throw new RuntimeException(e);
	    }
	    return res;
	  }
	
}
	
