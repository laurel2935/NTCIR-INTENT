package org.archive.a1.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.archive.OutputDirectory;
import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.nicta.evaluation.evaluator.Evaluator;
import org.archive.nicta.evaluation.metricfunction.NDEval10Losses;
import org.archive.util.io.IOText;
import org.archive.util.tuple.IntStrInt;
import org.archive.util.tuple.StrDouble;

public class ResultAnalyzer {
	private final static boolean DEBUG = true;
	
	public static void getTopicDistributionOfLambda(DivVersion divVersion, String resultFile){
		
		HashMap<String, StrDouble> topicLambdaMap = getMaxLambdaSetting(resultFile);
		
		HashMap<String, HashSet<String>> lambdaTopicMap = new HashMap<String, HashSet<String>>();
		
		for(Entry<String, StrDouble> entry: topicLambdaMap.entrySet()){
			String topicID = entry.getKey();
			String lambdaStr = getLambdaStr(entry.getValue().first);
			
			if(lambdaTopicMap.containsKey(lambdaStr)){
				lambdaTopicMap.get(lambdaStr).add(topicID);
			}else{
				HashSet<String> topicSet = new HashSet<String>();
				topicSet.add(topicID);
				lambdaTopicMap.put(lambdaStr, topicSet);
			}
		}
		
		Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(divVersion);	
		
		ArrayList<IntStrInt> list = new ArrayList<IntStrInt>();
		
		for(Entry<String, HashSet<String>> entry: lambdaTopicMap.entrySet()){
			String lambdaStr = entry.getKey();
			HashSet<String> topicSet = entry.getValue();
			
			int facetedCount = 0;
			int amCount = 0;
			
			for(String topic: topicSet){
				if(trecDivQueries.get(topic)._type.equals("faceted")){
					facetedCount++;
				}else{
					amCount++;
				}
			}
			
			list.add(new IntStrInt(facetedCount, lambdaStr, amCount));
		}
		
		Collections.sort(list);
		
		System.out.println();
		System.out.println(divVersion.toString());
		for(IntStrInt element: list){
			System.out.println(element.second+"\t"+"faceted: "+element.first+"\t"+"ambiguous: "+element.third+"\tTotal:"+(element.first+element.third));
		}
	}
	private static String getLambdaStr(String lambdaStr){
		return lambdaStr.substring(lambdaStr.indexOf("[")+1, lambdaStr.indexOf("]"));		
	}
	
	
	////topic-id -> [lambdaString & alphaNDCG@20]
	public static HashMap<String, StrDouble> getMaxLambdaSetting(String resultFile){
		
		HashMap<String, StrDouble> topicLambdaMap = new HashMap<String, StrDouble>();
		
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(resultFile);
		for(String line: lineList){
			line = line.replaceAll("[\\s]+", "\t");
			String [] fields = line.split("\\s");
			
			/*
			System.out.println(fields.length);
			for(int k=0; k<fields.length; k++){
				System.out.println(fields[k]);
			}
			System.out.println();
			*/
			
			String topicID = fields[0];
			String lambdaStr = fields[1];
			String alphaNDCG20Str = fields[14];
			
			//System.out.println(topicID);
			//System.out.println(lambdaStr);
			//System.out.println(alphaNDCG20Str);
			
			Double currV = getDouble(alphaNDCG20Str.trim());
			
			if(topicLambdaMap.containsKey(topicID)){				
				if(currV > topicLambdaMap.get(topicID).second){
					topicLambdaMap.put(topicID, new StrDouble(lambdaStr, currV));
				}
			}else{
				topicLambdaMap.put(topicID, new StrDouble(lambdaStr, currV));
			}			
		}
		
		if(DEBUG){
			for(Entry<String, StrDouble> entry: topicLambdaMap.entrySet()){
				String topicID = entry.getKey();
				StrDouble strD = entry.getValue();

				System.out.println(topicID+":\t"+strD.first+"\t"+strD.second);
			}
		}
		
		return topicLambdaMap;
	}
	private static Double getDouble(String alphaNDCG20Str){
		String targetStr = alphaNDCG20Str.substring(alphaNDCG20Str.indexOf(":")+1).trim();
		//System.out.println(targetStr);
		return Double.valueOf(targetStr);			
	}
	
	
	
	////ideal result with an adaptive lambda
	public static void getIdealResultsOfLambda(String resultFile){
		
		HashMap<String, StrDouble> topicLambdaMap = new HashMap<String, StrDouble>();
		
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(resultFile);
		for(String line: lineList){
			line = line.replaceAll("[\\s]+", "\t");
			String [] fields = line.split("\\s");
			
			/*
			System.out.println(fields.length);
			for(int k=0; k<fields.length; k++){
				System.out.println(fields[k]);
			}
			System.out.println();
			*/
			
			String topicID = fields[0];			
			String alphaNDCG20Str = fields[14];
			
			//System.out.println(topicID);
			//System.out.println(lambdaStr);
			//System.out.println(alphaNDCG20Str);
			
			Double currV = getDouble(alphaNDCG20Str.trim());
			
			if(topicLambdaMap.containsKey(topicID)){				
				if(currV > topicLambdaMap.get(topicID).second){
					topicLambdaMap.put(topicID, new StrDouble(line, currV));
				}
			}else{
				topicLambdaMap.put(topicID, new StrDouble(line, currV));
			}			
		}
		
		if(DEBUG){
			for(Entry<String, StrDouble> entry: topicLambdaMap.entrySet()){
				String topicID = entry.getKey();
				StrDouble strD = entry.getValue();

				System.out.println(topicID+"->"+strD.first+"\t"+strD.second);
			}
			System.out.println();
		}
		
		ArrayList<String> idealPerResultList = new ArrayList<String>();
		
		for(Entry<String, StrDouble> entry: topicLambdaMap.entrySet()){
			//String topicID = entry.getKey();
			StrDouble strD = entry.getValue();
			idealPerResultList.add(strD.first);			
		}
		
		double [] sumArray = new double [21];
		for(int i=0; i<sumArray.length; i++){
			sumArray[i] = 0.0d;
		}
		
		for(String resultLine: idealPerResultList){
			String [] fields = resultLine.split("\t");
			for(int i=3; i<fields.length; i++){
				sumArray[i-3] += getDouble(fields[i]);
			}
		}
		
		for(int i=0; i<sumArray.length; i++){
			sumArray[i] = sumArray[i]/idealPerResultList.size();
		}
		
		StringBuffer buffer = new StringBuffer();
		for(int i=0; i<sumArray.length; i++){
			buffer.append(NDEval10Losses.metricVector.get(i)+":");
			buffer.append(Evaluator.fourResultFormat.format(sumArray[i])+"\t");
		}
		
		String resultString = buffer.toString();
		resultString = resultString.replaceAll("\n", "");
		
		System.out.println(resultString);		
	}
	
	
	
	
	
	//
	public static void main(String []args){
		//1 
		String perLambdaResultdir = OutputDirectory.ROOT+"DivEvaluation/PerLambdaEvaluation/";
		
		//DivVersion.Div2009 BM25Kernel_A1+TFIDF_A1-Div2009BFS_PerLambda_ndeval.txt
		//String Div2009File = "BM25Kernel_A1+TFIDF_A1-Div2009BFS_PerLambda_ndeval.txt";				
		//ResultAnalyzer.getTopicDistributionOfLambda(DivVersion.Div2009, perLambdaResultdir+Div2009File);
		
		//DivVersion.Div2010 BM25Kernel_A1+TFIDF_A1-Div2010BFS_PerLambda_ndeval.txt
		//String Div2010File = "BM25Kernel_A1+TFIDF_A1-Div2010BFS_PerLambda_ndeval.txt";				
		//ResultAnalyzer.getTopicDistributionOfLambda(DivVersion.Div2010, perLambdaResultdir+Div2010File);
		
		//String Div2009File_mdp = "MDP-TFIDF_A1-Div2009MDP_PerLambda_ndeval.txt";				
		//ResultAnalyzer.getTopicDistributionOfLambda(DivVersion.Div2009, perLambdaResultdir+Div2009File_mdp);
		
		String Div2010File_mdp = "MDP-TFIDF_A1-Div2010MDP_PerLambda_ndeval.txt";				
		ResultAnalyzer.getTopicDistributionOfLambda(DivVersion.Div2010, perLambdaResultdir+Div2010File_mdp);
		
		//2 ideal results
		//DivVersion.Div2009 BM25Kernel_A1+TFIDF_A1-Div2009BFS_PerLambda_ndeval.txt
		//String Div2009File = "BM25Kernel_A1+TFIDF_A1-Div2009BFS_PerLambda_ndeval.txt";				
		//ResultAnalyzer.getIdealResultsOfLambda(perLambdaResultdir+Div2009File);
		
	}

}
