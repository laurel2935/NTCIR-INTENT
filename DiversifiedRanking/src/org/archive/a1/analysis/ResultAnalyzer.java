package org.archive.a1.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.util.io.IOText;
import org.archive.util.tuple.IntStrInt;
import org.archive.util.tuple.StrDouble;

public class ResultAnalyzer {
	
	
	public void getTopicDistributionOfLambda(DivVersion divVersion, HashMap<String, StrDouble> topicLambdaMap){
		
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
		
		for(IntStrInt element: list){
			System.out.println(element.second+"\t"+"faceted: "+element.first+"\t"+"ambiguous: "+element.third);
		}
	}
	private static String getLambdaStr(String lambdaStr){
		return lambdaStr.substring(lambdaStr.indexOf("[")+1, lambdaStr.indexOf("]"));		
	}
	
	
	
	public static HashMap<String, StrDouble> getMaxLambdaSetting(String resultFile){
		//topic-id -> [lambdaString & alphaNDCG@20]
		HashMap<String, StrDouble> topicLambdaMap = new HashMap<String, StrDouble>();
		
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(resultFile);
		for(String line: lineList){
			String [] fields = line.split("\t");
			
			String topicID = fields[0];
			String lambdaStr = fields[1];
			String alphaNDCG20Str = fields[14];
			
			Double currV = getDouble(alphaNDCG20Str);
			
			if(topicLambdaMap.containsKey(topicID)){				
				if(currV > topicLambdaMap.get(topicID).second){
					topicLambdaMap.put(topicID, new StrDouble(lambdaStr, currV));
				}
			}else{
				topicLambdaMap.put(topicID, new StrDouble(lambdaStr, currV));
			}
		}
		
		return topicLambdaMap;
	}
	private static Double getDouble(String alphaNDCG20Str){
		return Double.parseDouble(alphaNDCG20Str.substring(alphaNDCG20Str.indexOf(":")));		
	}

}
