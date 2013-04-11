package dataingest;

import java.util.List;

public class Article {

	String abstractText;
	String journal;
	List<String> meshMajor;
	Long pmid;
	String title;
	String year;
	
	public void print() {
		System.out.println("abstractText: " + abstractText);
		System.out.println("journal: " + journal);
		System.out.println("meshMajor:");
		for(String s : meshMajor) {
			System.out.println("    " + s);
		}
		System.out.println("pmid: " + pmid);
		System.out.println("title: " + title);
		System.out.println("year: " + year);
	}
	
}
