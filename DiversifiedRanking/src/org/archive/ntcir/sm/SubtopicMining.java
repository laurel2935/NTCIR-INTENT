package org.archive.ntcir.sm;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ntcir.sm.RunParameter.ClusteringFunction;
import org.archive.ntcir.sm.RunParameter.SimilarityFunction;
import org.archive.ntcir.sm.clustering.ap.APClustering;
import org.archive.util.io.IOText;

public class SubtopicMining {
	private final static boolean DEBUG = true;
	
	public void run(RunParameter runParameter) throws Exception{
		//formal system result		
		String formalRun = runParameter.outputDir+runParameter.runTitle+".txt";
		BufferedWriter writer = IOText.getBufferedWriter_UTF8(formalRun);		
		writer.write(runParameter.runIntroduction);
		writer.newLine();
		//		
		
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
		for(int i=0; i<runParameter.topicList.size(); i++){
			
			SMTopic smTopic = runParameter.topicList.get(i);						
			if(DEBUG){
				System.out.println("Processing topic "+ smTopic.getID()+"\t"+smTopic.getTopicText());
			}
			
			RankedList rankedList = null;
			
			if(ClusteringFunction.StandardAP == runParameter.cFunction){
				
				ArrayList<InteractionData> releMatrix = getReleMatrix(smTopic, runParameter.simFunction);				
				
		    	double lambda = 0.5;
		    	int iterations = 5000;
		    	int convits = 10;
		    	double preferences = getMedian(releMatrix);    	
		    	APClustering apClustering = new APClustering(lambda, iterations, convits, preferences, releMatrix);
		    	apClustering.setParemeters();
		    	HashMap<String, String> clusterMap = apClustering.run();
		    	rankedList = DCGK.staticDCG_K(runParameter, smTopic, clusterMap);
			}
			
			if(null != rankedList){				
				for(RankedRecord record: rankedList.recordList){
					writer.write(record.toString());
					writer.newLine();
				}
			}else{
				new Exception("Null RankedList Error!").printStackTrace();
			}
			//
			writer.flush();
			writer.close();	
		}
	}
	
	private ArrayList<InteractionData> getReleMatrix(SMTopic smTopic, SimilarityFunction simFunction){
		
	}
	
	
	public static void main(String []args){
		//1
	}
	

}
