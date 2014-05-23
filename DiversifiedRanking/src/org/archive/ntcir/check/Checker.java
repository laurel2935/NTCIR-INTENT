package org.archive.ntcir.check;

import java.util.ArrayList;

import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.util.io.IOText;

public class Checker {
	
	
	//
	public void checkNoResultTopic(NTCIR_EVAL_TASK eval, String resultFile){
		ArrayList<String> totalIDList = new ArrayList<String>();
		//
		
		
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(resultFile);
		
	}

}
