package org.archive.sm.kernel;

public class NpIRAnnotator {
	
	public boolean accept(String posTag){
		if(posTag.equals("NP")){
			return true;
		}else{
			return false;
		}
	}

}
