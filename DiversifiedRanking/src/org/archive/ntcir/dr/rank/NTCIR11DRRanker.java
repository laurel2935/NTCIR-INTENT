package org.archive.ntcir.dr.rank;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.archive.OutputDirectory;
import org.archive.a1.ranker.fa.DCKUFLRanker;
import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.nlp.tokenizer.Tokenizer;
import org.archive.ntcir.sm.SMRankedList;
import org.archive.ntcir.sm.SMRankedRecord;
import org.archive.ntcir.sm.SMRunParameter;
import org.archive.ntcir.sm.SMRunParameter.ClusteringFunction;
import org.archive.ntcir.sm.SMRunParameter.SimilarityFunction;
import org.archive.util.Language.Lang;
import org.archive.util.io.IOText;
import org.archive.util.tuple.StrDouble;

public class NTCIR11DRRanker {
	private final static boolean DEBUG = true;
	
	public void run(DRRunParameter drRunParameter, Lang lang) throws Exception{
		//formal system result
		File dirFile = new File(drRunParameter.outputDir);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}
		String formalRun = drRunParameter.outputDir+drRunParameter.runTitle+".txt";
		BufferedWriter writer = IOText.getBufferedWriter_UTF8(formalRun);		
		writer.write(drRunParameter.runIntroduction);
		writer.newLine();
		//		
		run(drRunParameter, writer, lang);
		
	}
	
	private void run(DRRunParameter drRunParameter, BufferedWriter writer, Lang lang) throws Exception{
		
		HashMap<String, ArrayList<String>> smResult = loadSMResult(drRunParameter, lang);
		
		DCKUFLRanker rRanker = null;
		
		for(int t=0; t<drRunParameter.topicList.size(); t++){
			
			SMTopic smTopic = drRunParameter.topicList.get(t);
			ArrayList<String> baseline = drRunParameter.baselineMap.get(smTopic.getID());
						
			if(DEBUG){
				System.out.println("Processing topic "+ smTopic.getID()+"\t"+smTopic.getTopicText());
			}
			
			if(smTopic.belongToBadCase()){
				continue;
			}
			
			DRRankedList rankedList = null;
			//
			if(smTopic.CompleteSentence){
				
				rankedList = new DRRankedList();
				
				int size = Math.min(100, baseline.size());
				
				for(int i=0; i<size; i++){
					String [] fields = baseline.get(i).split("\\s");					
					DRRankedRecord record = new DRRankedRecord(smTopic.getID(), fields[2], (i+1), Double.parseDouble(fields[4]), drRunParameter.runTitle);					
					rankedList.addRecord(record);
				}
				
				System.out.println("For clear topic:");
				System.out.println(rankedList.tosString());
				
			}else{
				//1
				ArrayList<String> subtopicList = smResult.get(smTopic.getID());			
				
				//
				ArrayList<String> topnDocList = getTopnDocs(baseline);
				
				//
				rRanker.clearInfoOfTopNDocs();				
				if (DEBUG){
					System.out.println("- Evaluating with " + topnDocList.size() + " docs");
				}
				
				for (String doc_name : topnDocList) {
					if (!drRunParameter.docMap.containsKey(doc_name))
						System.err.println("ERROR: '" + doc_name + "' not found for '" + smTopic.getTopicText() + "'");
					else {
						rRanker.addATopNDoc(doc_name);						
					}
				}				
				// Get the results
				if (DEBUG){
					System.out.println("- Running alg: " + rRanker.getDescription());
				}	
				
				rankedList = new DRRankedList();
				
				ArrayList<StrDouble> resultList = null;
				
				resultList = rRanker.getResultList(drRunParameter, smTopic, subtopicList, 20);
				
				int others = 100 - resultList.size();
				
				HashSet<String> rSet = new HashSet<String>();
				for(int i=0; i<resultList.size(); i++){
					StrDouble r = resultList.get(i);
					DRRankedRecord record = new DRRankedRecord(smTopic.getID(), r.first, (i+1), r.second, drRunParameter.runTitle);					
					rankedList.addRecord(record);
					
					rSet.add(r.first);
				}
				
				int goon = resultList.size()+1;
				int jj = 0;
				for(String line: baseline){
					String [] fields = line.split("\\s");
					if(!rSet.contains(fields[2]) && jj<others){
						DRRankedRecord record = new DRRankedRecord(smTopic.getID(), fields[2], goon++, Double.parseDouble(fields[4]), drRunParameter.runTitle);					
						rankedList.addRecord(record);
						jj++;
					}
				}
			}
			///*
			if(null != rankedList && null!=rankedList.recordList){				
				for(DRRankedRecord record: rankedList.recordList){
					writer.write(record.toString());
					writer.newLine();
				}
			}else{
				System.err.println("Null RankedList Error!");
				continue;
				//new Exception("Null RankedList Error!").printStackTrace();
			}
			//*/
		}
		///*
		writer.flush();
		writer.close();
		//*/
	}
	
	private ArrayList<String> getTopnDocs(ArrayList<String> baseline){
		ArrayList<String> topnDocList = new ArrayList<String>();
		int size = Math.min(100, baseline.size()); 
		
		for(int i=0; i<size; i++){
			String [] fields = baseline.get(i).split("\\s");
			topnDocList.add(fields[2]);
		}
		
		return topnDocList;
	}
	
	private static HashMap<String, ArrayList<String>> loadSMResult(DRRunParameter drRunParameter, Lang lang){
		String dir = OutputDirectory.ROOT+"SMResult/";
		String file = null;
		if(Lang.Chinese == lang){
			file = dir+"TUTA1-S-C-1A.txt";
		}else{
			file = dir+"TUTA1-S-E-1A.txt";
		}
		
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(file);
		lineList.remove(0);
		
		HashMap<String, ArrayList<String>> lineMap = new HashMap<String, ArrayList<String>>();
		for(String line: lineList){
			String [] fields = line.split(";");
			if(lineMap.containsKey(fields[0])){
				lineMap.get(fields[0]).add(line);
			}else{
				ArrayList<String> group = new ArrayList<String>();
				group.add(line);
				lineMap.put(fields[0], group);
			}
		}
		
		HashMap<String, ArrayList<String>> subtopicMap = new HashMap<String, ArrayList<String>>();
		for(Entry<String, ArrayList<String>> entry: subtopicMap.entrySet()){
			ArrayList<String> subtopicList = getSubtopicList(drRunParameter, entry.getValue(), lang);
			subtopicMap.put(entry.getKey(), subtopicList);
		}
		
		return subtopicMap;
	}
	
	private static ArrayList<String> getSubtopicList(DRRunParameter drRunParameter, ArrayList<String> rankedList, Lang lang){
		HashMap<String, ArrayList<String>> substrMap = new HashMap<String, ArrayList<String>>();
		for(String record: rankedList){
			String [] fields = record.split(";");
			if(substrMap.containsKey(fields[2])){
				substrMap.get(fields[2]).add(fields[6]);
			}else{
				ArrayList<String> substrGroup = new ArrayList<String>();
				substrGroup.add(fields[6]);
				substrMap.put(fields[2], substrGroup);
			}
		}
		
		ArrayList<String> subtopicList = new ArrayList<String>();
		for(Entry<String, ArrayList<String>> entry: substrMap.entrySet()){
			ArrayList<String> group = entry.getValue();
			HashSet<String> termSet = new HashSet<String>();
			for(String substr: group){
				ArrayList<String> termList = null;
				if(Lang.Chinese == lang){
					termList = drRunParameter.SegmentedStringMap.get(substr);					
				}else{
					termList = Tokenizer.segment(substr, lang);
				}
				for(String term: termList){
					if(!termSet.contains(term)){
						termSet.add(term);
					}
				}				
			}
			subtopicList.add(convert(termSet.toArray(new String [0])));			
		}
		
		return subtopicList;		
	}
			
	private static String convert(String [] tList){
		StringBuffer buffer = new StringBuffer();
		for(String t: tList){
			buffer.append(t);
			buffer.append(" ");
		}
		return buffer.toString().trim();
	}
	
	
	
	
	
	
	
	
	
	//
	public static void main(String []args){
		NTCIR11DRRanker drRanker = new NTCIR11DRRanker();
		
		//1
		HashMap<String, String> chNtcir11docMap = NTCIRLoader.loadNTCIR11Docs_CH();
		HashMap<String, ArrayList<String>> chNtcir11BaselineMap = NTCIRLoader.loadNTCIR11Baseline_CH();
		
		String drRunTitle = "TUTA1-D-E-1A";
		String drRunIntroduction = "Corresponding to the English subtopic mining subtask, we rely on the categorical knowledge of Wikipedia and the Stanford Parser "
				+ "to identify the ambiguous topics, clear topics. Based on the Affinity Propagation clustering approach to cluster the subtopic strings.";
		DRRunParameter drRunParameter = new DRRunParameter(NTCIR_EVAL_TASK.NTCIR11_SM_EN, drRunTitle, drRunIntroduction, chNtcir11docMap, chNtcir11BaselineMap);
		try {
			drRanker.run(drRunParameter, Lang.Chinese);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	

}
