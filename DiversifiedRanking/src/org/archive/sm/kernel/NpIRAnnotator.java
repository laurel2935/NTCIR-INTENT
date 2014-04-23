package org.archive.sm.kernel;

public class NpIRAnnotator extends IRAnnotator {
	
	public boolean accept(String posTag){
		if(posTag.equals("NP")){
			return true;
		}else{
			return false;
		}
	}

}
