package org.archive.dataset.ntcir.sm;

/**
 * NN or NR kernel-object
 * **/

public class TermIRAnnotator extends IRAnnotator {	
		
	protected boolean accept(String posTag){
		if(posTag.equals("NN") || posTag.equals("NR")){
			return true;
		}else{
			return false;
		}
	}

}
