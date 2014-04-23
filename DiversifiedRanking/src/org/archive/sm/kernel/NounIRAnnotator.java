package org.archive.sm.kernel;

/**
 * NN or NR
 * **/

public class NounIRAnnotator extends IRAnnotator {	
		
	public boolean accept(String posTag){
		if(posTag.equals("NN") || posTag.equals("NR")){
			return true;
		}else{
			return false;
		}
	}

}
