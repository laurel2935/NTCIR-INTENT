package org.archive.ntcir.sm;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;

import org.archive.OutputDirectory;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.nlp.chunk.lpt.ltpService.LTML;
import org.archive.nlp.qpunctuation.QueryPreParser;
import org.archive.nlp.tokenizer.Tokenizer;
import org.archive.ntcir.sm.preprocess.TopicParser.StringType;
import org.archive.util.Language.Lang;
import org.archive.util.io.IOText;

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
	//for Ch topics and subtopic strings
	protected HashMap<String, LTML> ltmlMap = null;
	
	
	
	public RunParameter(NTCIR_EVAL_TASK eval, String runTitle, String runIntroduction, SimilarityFunction simFunction, ClusteringFunction cFunction){		
		this.runTitle = runTitle;
		//
		this.runIntroduction = "<SYSDESC>"+runIntroduction+"</SYSDESC>";
		
	}
	
	//
	protected void loadLTMLForChTopics(List<SMTopic> smTopicList){
		String tDir = OutputDirectory.ROOT+"ntcir-11/SM/ParsedTopic/PerFile/";
		String subTDir = OutputDirectory.ROOT+"ntcir-11/SM/SubtopicString/ParsedWithLTP/";
		
		ltmlMap = new HashMap<String, LTML>();
		//
		for(SMTopic smTopic: smTopicList){
			String tXMLFile = tDir+smTopic.getID()+".xml";
			LTML tLTML = loadLTML(tXMLFile);
			ltmlMap.put(smTopic.getID(), tLTML);
			
			int id = 1;
			for(String str: smTopic.uniqueRelatedQueries){
				if(!QueryPreParser.isOddQuery(str, Lang.Chinese)){
					continue;
				}
				String subTXMLFile = subTDir+smTopic.getID()+"-"+Integer.toString(id)+".xml";
				LTML subTLTML = loadLTML(subTXMLFile);
				ltmlMap.put(smTopic.getID()+"-"+Integer.toString(id), subTLTML);
				//
				id++;
			}
		}		
	}
	private static LTML loadLTML(String xmlFile){		
		try {
			LTML ltml = new LTML();
			//System.out.println(xmlFile);
			ltml.build(loadAFile(xmlFile));
			return ltml;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
		return null;		
	}
	private static String loadAFile(String targetFile){
		try {
			StringBuffer buffer = new StringBuffer();
			//
			BufferedReader reader = IOText.getBufferedReader_UTF8(targetFile);
			String line = null;			
			while(null != (line=reader.readLine())){
				if(line.length() > 0){					
					buffer.append(line);
					buffer.append("\n");
				}				
			}
			reader.close();
			return buffer.toString();			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}

}
