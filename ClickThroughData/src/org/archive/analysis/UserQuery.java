package org.archive.analysis;

import java.util.Vector;

public class UserQuery {
	private String rawQuery = null;
	private String noSymbolStr = "";
	private Vector<String> words = null;
	//
	UserQuery(String rawQ){
		
	}
	//
	public boolean valid(){
		if(null!=this.words && this.words.size()>0){
			return true;
		}else{
			return false;
		}
	}
	//
	
	//
	public static Vector<String> getWords(String rawStr){
		return null;
	}
}
