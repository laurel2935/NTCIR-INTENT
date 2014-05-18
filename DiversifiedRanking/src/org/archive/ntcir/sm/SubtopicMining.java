package org.archive.ntcir.sm;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.dataset.ntcir.sm.SMSubtopicItem;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.ml.clustering.ap.abs.ClusterString;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.ufl.K_UFL;
import org.archive.ml.ufl.K_UFL.UFLMode;
import org.archive.nlp.chunk.lpt.ltpService.LTML;
import org.archive.nlp.chunk.lpt.ltpService.Word;
import org.archive.nlp.qpunctuation.QueryPreParser;
import org.archive.ntcir.sm.RunParameter.ClusteringFunction;
import org.archive.ntcir.sm.RunParameter.SimilarityFunction;
import org.archive.ntcir.sm.clustering.ap.APClustering;
import org.archive.ntcir.sm.similarity.editdistance.GregorEditDistance;
import org.archive.ntcir.sm.similarity.editdistance.StandardEditDistance;
import org.archive.ntcir.sm.similarity.editdistance.definition.SuperString;
import org.archive.util.Language.Lang;
import org.archive.util.io.IOText;
import org.archive.util.tuple.StrStr;

public class SubtopicMining {
	private final static boolean DEBUG = true;
	
	public void run(RunParameter runParameter) throws Exception{
		
		//formal system result
		File dirFile = new File(runParameter.outputDir);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}
		String formalRun = runParameter.outputDir+runParameter.runTitle+".txt";
		BufferedWriter writer = IOText.getBufferedWriter_UTF8(formalRun);		
		writer.write(runParameter.runIntroduction);
		writer.newLine();
		//		
		if(NTCIR_EVAL_TASK.NTCIR11_SM_CH == runParameter.eval){
			//runParameter.loadLTMLForChTopics(runParameter.topicList);
			runParameter.segment(runParameter.topicList);
			
			chRun(runParameter, writer);
			
		}else{
			
			enRun(runParameter, writer);
			
		}
	}	
	//
	private static double getMedian(ArrayList<InteractionData> releMatrix){	
		ArrayList<Double> vList = new ArrayList<Double>();
		for(InteractionData iData: releMatrix){
			vList.add(iData.getSim());
		}
		Collections.sort(vList);
    	if(vList.size() % 2 == 0){
    		return (vList.get(vList.size()/2 - 1)+vList.get(vList.size()/2))/2.0;
    	}else{    		
    		return vList.get(vList.size()/2);
    	}
	 }
	
	private void chRun(RunParameter runParameter, BufferedWriter writer){
		
		runParameter.loadLTMLForChTopics(runParameter.topicList);
		
		for(int i=0; i<runParameter.topicList.size(); i++){
			
			SMTopic smTopic = runParameter.topicList.get(i);						
			if(DEBUG){
				System.out.println("Processing topic "+ smTopic.getID()+"\t"+smTopic.getTopicText());
			}
			
			RankedList rankedList = null;
			
			if(ClusteringFunction.StandardAP == runParameter.cFunction){
				
				ArrayList<InteractionData> releMatrix = getChReleMatrix(smTopic, runParameter);				
				
		    	double lambda = 0.5;
		    	int iterations = 5000;
		    	int convits = 10;
		    	double preferences = getMedian(releMatrix);    	
		    	APClustering apClustering = new APClustering(lambda, iterations, convits, preferences, releMatrix);
		    	apClustering.setParemeters();
		    	Map<String, ClusterString> clusterMap = (Map<String, ClusterString>)apClustering.run();
		    	//rankedList = DCGK.staticDCG_K(runParameter, smTopic, clusterMap);
		    	if(DEBUG){
		    		System.out.println("Cluster size:\t"+clusterMap.size());
		    		Set<String> keySet = clusterMap.keySet();
		    		int id = 1;
		    		for(String key: keySet){
		    			System.out.println("Exemplar-"+(id++)+":\t"+getSubtopicString(smTopic, key));
		    			Collection<String> memberSet = clusterMap.get(key).getElements();
		    			int mID = 1;
		    			for(String memKey: memberSet){
		    				System.out.println("\t"+(mID++)+":\t"+getSubtopicString(smTopic, memKey));
		    			}
		    			System.out.println();
		    		}		    		
		    	}
			}
			
			/*
			if(null != rankedList){				
				for(RankedRecord record: rankedList.recordList){
					writer.write(record.toString());
					writer.newLine();
				}
			}else{
				new Exception("Null RankedList Error!").printStackTrace();
			}
			*/
		}
		//
		/*
		writer.flush();
		writer.close();
		*/
	}
	
	private String getSubtopicString(SMTopic smTopic, String key){
		String idStr = key.substring(key.indexOf("-")+1);
		int id = Integer.parseInt(idStr);
		return smTopic.uniqueRelatedQueries.get(id-1);		
	}
	
	private ArrayList<InteractionData> getChReleMatrix(SMTopic smTopic, RunParameter runParameter){
		ArrayList<InteractionData> chReleMatrix = new ArrayList<InteractionData>();
		
		if(runParameter.simFunction==SimilarityFunction.StandardTermEditDistance
				|| runParameter.simFunction==SimilarityFunction.SemanticTermEditDistance){
			
			StandardEditDistance standardEditDistance = new StandardEditDistance();
			
			ArrayList<String> keyList = new ArrayList<String>();
			ArrayList<LTML> ltmlList = new ArrayList<LTML>();
			
			int id = 1;
			for(String str: smTopic.uniqueRelatedQueries){
				if(!QueryPreParser.isOddQuery(str, Lang.Chinese)){
					continue;
				}
				String key = smTopic.getID()+"-"+Integer.toString(id);
				if(runParameter.ltmlMap.containsKey(key)){
					LTML subTLTML = runParameter.ltmlMap.get(key);		
					keyList.add(key);
					ltmlList.add(subTLTML);
				}				
				//
				id++;
			}
			
			for(int i=0; i<ltmlList.size()-1; i++){
				ArrayList<Word> iWordList = ltmlList.get(i).getWords(0);
				if(null == iWordList){
					continue;
				}
				ArrayList<StrStr> iwList = getWords(iWordList);
				for(int j=i+1; j<ltmlList.size(); j++){
					ArrayList<Word> jWordList = ltmlList.get(j).getWords(0);
					if(null == jWordList){
						continue;
					}
					ArrayList<StrStr> jwList = getWords(jWordList);
					
					double simValue = 0.0;
					if(runParameter.simFunction==SimilarityFunction.StandardTermEditDistance){
						simValue = 0 - standardEditDistance.getEditDistance(SuperString.createTermSuperString_2(iwList),
								SuperString.createTermSuperString_2(jwList), Lang.Chinese);
					}else if(runParameter.simFunction==SimilarityFunction.SemanticTermEditDistance){
						//System.out.println("SemanticTermEditDistance!!!!!!!!!!!!!");
						simValue = 0 - standardEditDistance.getEditDistance(SuperString.createSemanticTermSuperString_1(iwList),
								SuperString.createSemanticTermSuperString_1(jwList), Lang.Chinese);
						if(DEBUG){
							System.out.println(iwList.toString());
							System.out.println(jwList.toString());
							System.out.println(simValue);
							System.out.println();
						}
					}else{
						new Exception("Odd Error!").printStackTrace();
					}
					
					chReleMatrix.add(new InteractionData(keyList.get(i), keyList.get(j), simValue));
				}
			}
			
			return chReleMatrix;
		}else if(runParameter.simFunction==SimilarityFunction.GregorEditDistance){
			GregorEditDistance Ged = new GregorEditDistance();
			
			ArrayList<String> keyList = new ArrayList<String>();
			ArrayList<ArrayList<String>> wordsList = new ArrayList<ArrayList<String>>();
			
			int id = 1;
			for(String str: smTopic.uniqueRelatedQueries){
				if(!QueryPreParser.isOddQuery(str, Lang.Chinese)){
					continue;
				}
				String key = smTopic.getID()+"-"+Integer.toString(id);
				if(runParameter.SegmentedStringMap.containsKey(key)){
					ArrayList<String> words = runParameter.SegmentedStringMap.get(key);		
					keyList.add(key);
					wordsList.add(words);
				}				
				//
				id++;
			}
			
			for(int i=0; i<wordsList.size()-1; i++){
				ArrayList<String> iWords = wordsList.get(i);
				if(null == iWords){
					continue;
				}				
				for(int j=i+1; j<wordsList.size(); j++){
					ArrayList<String> jWords = wordsList.get(j);
					if(null == jWords){
						continue;
					}
					
					//System.out.println("SemanticTermEditDistance!!!!!!!!!!!!!");
					double simValue = 0 - Ged.getEditDistance(SuperString.createRawTermSuperString(iWords),
							SuperString.createRawTermSuperString(jWords), Lang.Chinese);
										
					chReleMatrix.add(new InteractionData(keyList.get(i), keyList.get(j), simValue));
				}
			}
			
			return chReleMatrix;
		}else{
			return null;
		}		
	}
	
	private ArrayList<StrStr> getWords(ArrayList<Word> wordList){
		ArrayList<StrStr> wList = new ArrayList<StrStr>();
		for(Word word: wordList){
			wList.add(new StrStr(word.getWS(), word.getPOS()));
		}
		return wList;
	}
	
	private void enRun(RunParameter runParameter, BufferedWriter writer){
		
		for(int i=0; i<runParameter.topicList.size(); i++){
			
			SMTopic smTopic = runParameter.topicList.get(i);						
			if(DEBUG){
				System.out.println("Processing topic "+ smTopic.getID()+"\t"+smTopic.getTopicText());
			}
			
			RankedList rankedList = null;
			
			if(ClusteringFunction.StandardAP == runParameter.cFunction){
				
				ArrayList<InteractionData> releMatrix = getEnReleMatrix(smTopic, runParameter);				
				
		    	double lambda = 0.5;
		    	int iterations = 5000;
		    	int convits = 10;
		    	double preferences = getMedian(releMatrix);    	
		    	APClustering apClustering = new APClustering(lambda, iterations, convits, preferences, releMatrix);
		    	apClustering.setParemeters();
		    	Map<String, ClusterString> clusterMap = (Map<String, ClusterString>)apClustering.run();
		    	//rankedList = DCGK.staticDCG_K(runParameter, smTopic, clusterMap);
		    	if(DEBUG){
		    		System.out.println("Cluster size:\t"+clusterMap.size());
		    		Set<String> keySet = clusterMap.keySet();
		    		int id = 1;
		    		for(String key: keySet){
		    			System.out.println("Exemplar-"+(id++)+":\t"+getSubtopicString(smTopic, key));
		    			Collection<String> memberSet = clusterMap.get(key).getElements();
		    			int mID = 1;
		    			for(String memKey: memberSet){
		    				System.out.println("\t"+(mID++)+":\t"+getSubtopicString(smTopic, memKey));
		    			}
		    			System.out.println();
		    		}		    		
		    	}
			}else if(ClusteringFunction.K_UFL == runParameter.cFunction){
				ArrayList<InteractionData> releMatrix = getEnReleMatrix(smTopic, runParameter);
				ArrayList<InteractionData> costMatrix = getCostMatrix(releMatrix);
				
				ArrayList<Double> fList = new ArrayList<Double>();
				for(int j=0; j<smTopic.smSubtopicItemList.size(); j++){
		    		fList.add(0.0);
		    	}
				
		    	double lambda = 0.5;
		    	int iterations = 5000;
		    	int convits = 10;
		    	double preferences = 0-getMedian(releMatrix);
		    	int preK = 5;
		    	K_UFL kUFL = new K_UFL(lambda, iterations, convits, preferences, preK, UFLMode.C_Same_F, costMatrix, fList);
		    	//    	
		    	kUFL.run();
			}
			
			/*
			if(null != rankedList){				
				for(RankedRecord record: rankedList.recordList){
					writer.write(record.toString());
					writer.newLine();
				}
			}else{
				new Exception("Null RankedList Error!").printStackTrace();
			}
			*/
		}
		//
		/*
		writer.flush();
		writer.close();
		*/
	}
	private ArrayList<InteractionData> getEnReleMatrix(SMTopic smTopic, RunParameter runParameter){
		double termIRWeight = 0.4; double phraseIRWeight = 0.6;
		
		ArrayList<InteractionData> enReleMatrix = new ArrayList<InteractionData>();
		ArrayList<SMSubtopicItem> itemList = smTopic.smSubtopicItemList;
		
		if(runParameter.simFunction == SimilarityFunction.SemanticTermEditDistance){
			
			StandardEditDistance standardEditDistance = new StandardEditDistance();
									
			for(int i=0; i<itemList.size()-1; i++){
				SMSubtopicItem iItem = itemList.get(i);			
				ArrayList<ArrayList<String>> iTermModifierGroupList = iItem.termModifierGroupList;
				ArrayList<ArrayList<String>> iPhraseModifierGroupList = iItem.phraseModifierGroupList;
				
				for(int j=i+1; j<itemList.size(); j++){
					SMSubtopicItem jItem = itemList.get(j);
					ArrayList<ArrayList<String>> jTermModifierGroupList = jItem.termModifierGroupList;
					ArrayList<ArrayList<String>> jPhraseModifierGroupList = jItem.phraseModifierGroupList;
					
					double simValue = 0.0;
					if(runParameter.simFunction == SimilarityFunction.SemanticTermEditDistance){
						
						for(int k=0; k<iTermModifierGroupList.size(); k++){
							simValue += ((termIRWeight/iTermModifierGroupList.size())*
									(0 - standardEditDistance.getEditDistance(SuperString.createSemanticTermSuperString_2(iTermModifierGroupList.get(k)),
											SuperString.createSemanticTermSuperString_2(jTermModifierGroupList.get(k)), Lang.English)));
						}
						
						for(int k=0; k<iPhraseModifierGroupList.size(); k++){
							simValue += ((phraseIRWeight/iPhraseModifierGroupList.size())*
									(0 - standardEditDistance.getEditDistance(SuperString.createSemanticTermSuperString_2(iPhraseModifierGroupList.get(k)),
											SuperString.createSemanticTermSuperString_2(jPhraseModifierGroupList.get(k)), Lang.English)));
						}	
					}else{
						new Exception("Odd Error!").printStackTrace();
					}
					
					enReleMatrix.add(new InteractionData(Integer.toString(i), Integer.toString(j), simValue));
				}
			}
			
			return enReleMatrix;
		}else{
			return null;
		}		
	}
	private static ArrayList<InteractionData> getCostMatrix(ArrayList<InteractionData> interList){
		ArrayList<InteractionData> costMatrix = new ArrayList<InteractionData>();
		for(InteractionData itr: interList){
			costMatrix.add(new InteractionData(itr.getFrom(), itr.getTo(), -itr.getSim()));
		}
		return costMatrix;		
	}
	
	public static void main(String []args){
		SubtopicMining smMining = new SubtopicMining();
		
		//1 test StandardTermEditDistance
		/*
		String runTitle = "testTitle";
		String runIntroduction = "testIntroduction";
		RunParameter runParameter = new RunParameter(NTCIR_EVAL_TASK.NTCIR11_SM_CH, runTitle, runIntroduction,
				SimilarityFunction.StandardTermEditDistance, ClusteringFunction.StandardAP);
		try {
			smMining.run(runParameter);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		*/
		
		//2 
		String runTitle = "testTitle";
		String runIntroduction = "testIntroduction";
		RunParameter runParameter = new RunParameter(NTCIR_EVAL_TASK.NTCIR11_SM_CH, runTitle, runIntroduction,
				SimilarityFunction.GregorEditDistance, ClusteringFunction.StandardAP);
		try {
			smMining.run(runParameter);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}	
	}
	

}
