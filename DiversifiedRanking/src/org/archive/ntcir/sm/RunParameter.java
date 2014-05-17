package org.archive.ntcir.sm;

import java.util.HashMap;
import java.util.List;

import org.archive.OutputDirectory;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.nlp.chunk.lpt.ltpService.LTML;

public class RunParameter {
	//////////////
	//set up parameters
	//////////////
	
	protected static enum SimilarityFunction{StandardEditDistance}
	
	protected static enum ClusteringFunction{StandardAP}
	
	protected String runTitle;
	protected String runIntroduction;
	
	protected String outputDir = OutputDirectory.ROOT+"SMResult/";
	
	protected SimilarityFunction simFunction;
	
	protected ClusteringFunction cFunction;
	
	protected List<SMTopic> topicList;
	//(topicID+SubtopicStrID) -> subtopic string, for ch and en
	protected HashMap<String, String> subtopicStrMap = null;
	
	//////////////////
	//Specific objects
	//////////////////
	//for Ch topics only
	protected HashMap<String, LTML> ltmlMap = null;
	
	
	
	public RunParameter(NTCIR_EVAL_TASK eval, String runTitle, String runIntroduction, SimilarityFunction simFunction, ClusteringFunction cFunction){		
		this.runTitle = runTitle;
		//
		this.runIntroduction = "<SYSDESC>"+runIntroduction+"</SYSDESC>";
		
	}

}
