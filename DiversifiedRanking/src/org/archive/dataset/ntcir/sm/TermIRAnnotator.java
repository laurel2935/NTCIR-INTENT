package org.archive.dataset.ntcir.sm;

import org.archive.util.Language.Lang;

/**
 * NN or NR kernel-object
 * **/

public class TermIRAnnotator extends IRAnnotator {	
		
	protected boolean accept(String posTag, Lang lang){
		if(Lang.English == lang){
			if(posTag.equals("NN") || posTag.equals("NR")){
				return true;
			}else{
				return false;
			}
		}else{
			if(posTag.startsWith("n")){
				return true;
			}else{
				return false;
			}
		}		
	}

}
