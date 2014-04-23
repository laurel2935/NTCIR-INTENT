package org.archive.dataset.ntcir.sm;

/**
 * Np kernel-object
 * **/

public class PhraseIRAnnotator extends IRAnnotator {
	
	protected boolean accept(String posTag){
		if(posTag.equals("NP")){
			return true;
		}else{
			return false;
		}
	}

}
