package org.archive.dataset.ntcir.sm;

import org.archive.util.Language.Lang;

/**
 * Np kernel-object
 * **/

public class PhraseIRAnnotator extends IRAnnotator {
	
	protected boolean accept(String posTag, Lang lang){
		if(posTag.equals("NP")){
			return true;
		}else{
			return false;
		}
	}

}
