import java.util.Comparator;

public class DocComp implements Comparator<DocNode> {
		    public int compare(DocNode d1, DocNode d2) {
		        if (d1.score > d2.score) return 1;
		        if (d1.score == d2.score) return 0;
		        return -1;
		    }
		}