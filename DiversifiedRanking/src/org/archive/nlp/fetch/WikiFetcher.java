package org.archive.nlp.fetch;

import java.util.List;

import info.bliki.api.Page;
import info.bliki.api.User;
import info.bliki.wiki.model.WikiModel;

public class WikiFetcher {
	
	
	public static void test(){
		String[] listOfTitleStrings = { "Web service" };
		User user = new User("", "", "http://en.wikipedia.org/w/api.php");
		//http://en.wikipedia.org/w/api.php
		user.login();
		List<Page> listOfPages = user.queryContent(listOfTitleStrings);
		for (Page page : listOfPages) {
		  WikiModel wikiModel = new WikiModel("${image}", "${title}");
		  String html = wikiModel.render(page.toString());
		  System.out.println(html);
		}
	}
	
	public static void main(String []args){
		//1
		WikiFetcher.test();
	}

}
