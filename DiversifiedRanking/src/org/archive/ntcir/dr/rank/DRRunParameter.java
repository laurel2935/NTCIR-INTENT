package org.archive.ntcir.dr.rank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.archive.OutputDirectory;
import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.nlp.qpunctuation.QueryPreParser;
import org.archive.nlp.tokenizer.Tokenizer;
import org.archive.util.Language.Lang;

public class DRRunParameter {
	
	NTCIR_EVAL_TASK eval;
	protected String runTitle;
	protected String runIntroduction;
	
	protected String outputDir = OutputDirectory.ROOT+"DRResult/";
	
	
	public List<SMTopic> topicList;
	public HashMap<String, String> docMap;
	public HashMap<String, ArrayList<String>> baselineMap;
	public HashMap<String, ArrayList<String>> SegmentedStringMap = null;
	
	public DRRunParameter(NTCIR_EVAL_TASK eval, String runTitle, String runIntroduction, HashMap<String, String> docMap, HashMap<String, ArrayList<String>> baselineMap){		
		this.eval = eval;
		this.runTitle = runTitle;		
		this.runIntroduction = "<SYSDESC>"+runIntroduction+"</SYSDESC>";		
		
		if(NTCIR_EVAL_TASK.NTCIR11_SM_EN == eval){
			this.topicList = NTCIRLoader.loadNTCIR11TopicList(eval, true);
			this.docMap = NTCIRLoader.loadNTCIR11BaselineDocs_EN();
			this.baselineMap = NTCIRLoader.loadNTCIR11Baseline_EN();
		}else{
			this.topicList = NTCIRLoader.loadNTCIR11TopicList(eval, true);
			this.docMap = NTCIRLoader.loadNTCIR11Docs_CH();
			this.baselineMap = NTCIRLoader.loadNTCIR11Baseline_CH();
			//
			segment(this.topicList);
		}	
	}
	
	protected void segment(List<SMTopic> smTopicList){
		SegmentedStringMap = new HashMap<String, ArrayList<String>>();
		//
		for(SMTopic smTopic: smTopicList){			
			ArrayList<String> tWords = Tokenizer.adaptiveQuerySegment(Lang.Chinese, smTopic.getTopicText(), null, true, true);			
			if(null != tWords){
				SegmentedStringMap.put(smTopic.getID(), tWords);
			}else{
				new Exception("Segment Topic Error!").printStackTrace();
				continue;
			}			
			
			int id = 1;
			String reference = Tokenizer.isDirectWord(smTopic.getTopicText(), Lang.Chinese)?smTopic.getTopicText():null;
			for(String str: smTopic.uniqueRelatedQueries){
				if(!QueryPreParser.isOddQuery(str, Lang.Chinese)){
					continue;
				}
				String subTKey = smTopic.getID()+"-"+Integer.toString(id);
				ArrayList<String> subTWords = Tokenizer.adaptiveQuerySegment(Lang.Chinese, str, reference, true, true);	
				if(null != subTWords){
					SegmentedStringMap.put(subTKey, subTWords);					
				}		
				//
				id++;
			}
		}
	}

}
