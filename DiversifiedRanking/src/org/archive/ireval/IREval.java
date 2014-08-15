package org.archive.ireval;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Vector;

import org.archive.util.io.IOText;
import org.archive.util.tuple.Pair;
import org.archive.util.tuple.PairComparatorBySecond_Desc;

/**
 * 
 * 
 * **/

public class IREval {
	private static final boolean debug = true;
	
	public static enum EVAL_TYPE{DIV_NTCIR, DIV_TREC, TIR_NTCIR11};
	
	/////////////////////
	//Common
	/////////////////////
	
	//a system run, i.e., topic -> list of <rank, item>
	HashMap<String, ArrayList<Integer>> _sysRun;
	
	//topic list
	ArrayList<String> _topicList;
	
	//here item refers to subtopic strings or doc_names
	ArrayList<String> _itemPooL;
	//item -> item index
	HashMap<String, Integer> _itemToIndex;
	
	
	////////////////////
	//Without subtopic consideration
	////////////////////
	
	//relevance assessment, i.e., topic -> list of <item-id, relevance-level>, the list is ranked in decreasing order of relevance-level
	HashMap<String, ArrayList<Pair<Integer, Integer>>> _topicToReleItemMap;	
	//relevance assessment, i.e., item-id -> Map of <topic, relevance-level>
	HashMap<Integer, HashMap<String, Integer>> _itemReleMap;
	
	
	
	////////////////////
	//With subtopic consideration
	////////////////////
	
	//topic -> list of <subtopic index, probability>
	HashMap<String, ArrayList<Pair<Integer, Integer>>> _topicToSubtopicMap;
	
	//relevance assessment, i.e., topic -> subtopic -> list of <item-id, relevance-level>, the list is ranked in an decreasing order of relevance-level 
	HashMap<String, HashMap<Integer, ArrayList<Pair<Integer, Integer>>>> _topicToSubtopicToReleItemMap;	
	//relevance assessment, i.e., item-id -> map of <topicid, set of <subtopic-id, relevance level>>
	HashMap<Integer, HashMap<String, HashSet<Pair<Integer, Integer>>>> _divItemReleMap;
	
	
	IREval(){
		this._sysRun = new HashMap<String, ArrayList<Integer>>();
		
		this._itemPooL = new ArrayList<String>();
		this._itemToIndex = new HashMap<String, Integer>();
		
		this._topicList = new ArrayList<String>();
	}
	
	
	/////////////////////
	//Preliminaries
	/////////////////////
	/**
	 * 
	 * **/
	private int getItemIndex(String item)
	{
		Integer itemIndex = this._itemToIndex.get(item);
		if(null == itemIndex)
		{
			int index = this._itemPooL.size();
			this._itemPooL.add(item);
			this._itemToIndex.put(item, index);
			//
			return index;
		}else{
			return itemIndex;
		}
	}
	
	private String getItem(Integer itemID){
		if(0<=itemID && itemID<_itemPooL.size()){
			return _itemPooL.get(itemID);
		}else{
			return null;
		}		
	}
	
	/**
	 * 
	 * **/
	private void loadSysRun(EVAL_TYPE evalType, String sysRunFile){	
		
		try {
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(sysRunFile);
			
			//skip head-description section, i.e., the first line, thus we start from index=1
			
			if(evalType == EVAL_TYPE.DIV_NTCIR){				
				
				String []array;
				for(int i=1; i<lineList.size(); i++){
					//array[0]:topic-id / array[1]:0 / array[2]:item-string / array[3]:rank / array[4]:score / array[5]:run-name
					array = lineList.get(i).split(";");
					String topicid = array[0];
					String item = array[2];
					
					if(_sysRun.containsKey(topicid)){
						ArrayList<Integer> itemList = _sysRun.get(topicid);
						itemList.add(getItemIndex(item));
					}else{
						ArrayList<Integer> itemList = new ArrayList<Integer>();
						itemList.add(getItemIndex(item));
						_sysRun.put(topicid, itemList);
					}
				}
				
			}else if(evalType == EVAL_TYPE.TIR_NTCIR11){
				
				String []array;
				for(int i=1; i<lineList.size(); i++){
					//array[0]:subtopic-id / array[1]:rank / array[2]:item-string / array[3]:group-id / array[4]:run-name
					array = lineList.get(i).split("\\s");
					String subtopicid = array[0];
					String item = array[2];
					
					if(_sysRun.containsKey(subtopicid)){
						ArrayList<Integer> itemList = _sysRun.get(subtopicid);
						itemList.add(getItemIndex(item));
					}else{
						ArrayList<Integer> itemList = new ArrayList<Integer>();
						itemList.add(getItemIndex(item));
						_sysRun.put(subtopicid, itemList);
					}
				}
				
			}else{
				System.err.println("Unexcepted Eval-Type!");
				System.exit(0);
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * **/
	private void loadStandardReleFile(EVAL_TYPE evalType, String standardReleFile){
		
		try {
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(standardReleFile);
			
			HashSet<String> topicSet = new HashSet<String>();
			
			if(evalType == EVAL_TYPE.TIR_NTCIR11){
				
				this._topicToReleItemMap = new HashMap<String, ArrayList<Pair<Integer,Integer>>>();
				this._itemReleMap = new HashMap<Integer, HashMap<String,Integer>>();
				
				String []array;
				for(String line: lineList){
					//array[0]:subtopic-id / array[1]:item-string / array[3]:relevance-level
					array = line.split("\\s");
					String subtopicid = array[0];
					//String item = array[1];
					Integer itemID = getItemIndex(array[1]);
					Integer releLevel = Integer.parseInt(array[2].substring(1));
					
					//for _topicList
					if(!topicSet.contains(subtopicid)){
						topicSet.add(subtopicid);
						
						_topicList.add(subtopicid);
					}
					
					//for _topicToReleItemMap
					if(_topicToReleItemMap.containsKey(subtopicid)){
						ArrayList<Pair<Integer,Integer>> itemPairList = _topicToReleItemMap.get(subtopicid);
						itemPairList.add(new Pair<Integer, Integer>(itemID, releLevel));
					}else{
						ArrayList<Pair<Integer,Integer>> itemPairList = new ArrayList<Pair<Integer,Integer>>();
						itemPairList.add(new Pair<Integer, Integer>(itemID, releLevel));
						_topicToReleItemMap.put(subtopicid, itemPairList);
					}
					
					//for _itemReleMap
					if(_itemReleMap.containsKey(itemID)){
						_itemReleMap.get(itemID).put(subtopicid, releLevel);
					}else{
						HashMap<String, Integer> releMap = new HashMap<String, Integer>();
						releMap.put(subtopicid, releLevel);
						
						_itemReleMap.put(itemID, releMap);
					}
				}	
				
				//sort item list
				for(Entry<String, ArrayList<Pair<Integer,Integer>>> entry: _topicToReleItemMap.entrySet()){
					ArrayList<Pair<Integer,Integer>> itemPairList = entry.getValue();
					Collections.sort(itemPairList, new PairComparatorBySecond_Desc<Integer, Integer>());
				}				
			}						
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Metric: P@k
	 * **/
	private ArrayList<Pair<String, Double>> P(int cutoff){
		ArrayList<Pair<String, Double>> precisionList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){
			
			if(debug){
				System.out.print("#syslen="+_sysRun.get(topic).size()+"\t");
				
				ArrayList<Pair<Integer,Integer>> itemPairList = _topicToReleItemMap.get(topic);
				int releCount = 0; int nonReleCount = 0;
				for(Pair<Integer, Integer> pair: itemPairList){
					if(pair.second > 0){
						releCount ++;
					}else {
						nonReleCount++;
					}
				}
				System.out.println("#jrel="+releCount+"\t#jnonrele="+nonReleCount);
				
				//
				if(topic.equals("001a")){
					ArrayList<Integer> itemList = _sysRun.get(topic);
					for(int i=0; i<20; i++){
						System.out.println((i+1)+":\t"+getItem(itemList.get(i)));
					}
				}
			}
			
			if(_sysRun.containsKey(topic)){
				ArrayList<Integer> sysList = _sysRun.get(topic);
				
				int k = Math.min(cutoff, sysList.size());
				
				int releCount = 0;
				for(int i=0; i<k; i++){
					Integer itemID = sysList.get(i);
					if(_itemReleMap.containsKey(itemID)){
						HashMap<String, Integer> releMap = _itemReleMap.get(itemID);
						if(releMap.containsKey(topic) && releMap.get(topic)>0){
							releCount++;
						}
					}
				}
				
				precisionList.add(new Pair<String, Double>(topic, releCount*1.0/cutoff));
				
			}else{
				System.err.println("No result for topic:\t"+topic);
				precisionList.add(new Pair<String, Double>(topic, 0.0));
			}
		}
		
		if(debug){
			for(Pair<String, Double> p: precisionList){
				System.out.println(p.toString());
			}
		}
		
		return precisionList;		
	}
	
	private ArrayList<Pair<String, Double>> avgP(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> pList, int cutoff){
		ArrayList<Pair<String, Double>> avgList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.TIR_NTCIR11){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: pList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			avgList.add(new Pair<String, Double>("all\t\tP@"+Integer.toString(cutoff), allSum/pList.size()));
			avgList.add(new Pair<String, Double>("past\t\tP@"+Integer.toString(cutoff), pastSum/pastCount));
			avgList.add(new Pair<String, Double>("recency\t\tP@"+Integer.toString(cutoff), recencySum/recencyCount));
			avgList.add(new Pair<String, Double>("future\t\tP@"+Integer.toString(cutoff), futureSum/futureCount));
			avgList.add(new Pair<String, Double>("atemporal\tP@"+Integer.toString(cutoff), atemporalSum/atemporalCount));
			
			if(debug){
				for(Pair<String, Double> p: avgList){
					System.out.println(p.toString());
				}
			}
		}
		
		return avgList;
	}
	
	/**
	 * Metric: AP
	 * **/
	private ArrayList<Pair<String, Double>> AP(int cutoff){
		ArrayList<Pair<String, Double>> apList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){

			ArrayList<Integer> sysList = _sysRun.get(topic);
						
			int releCount = 0;
			ArrayList<Double> pList = new ArrayList<Double>();
			for(int i=0; i<cutoff; i++){
				Integer itemID = sysList.get(i);
				if(_itemReleMap.containsKey(itemID)){
					HashMap<String, Integer> releMap = _itemReleMap.get(itemID);
					if(releMap.containsKey(topic) && releMap.get(topic)>0){
						releCount++;
						pList.add(releCount*1.0/(i+1));
					}else {
						pList.add(0.0);
					}
				}else{
					pList.add(0.0);
				}
			}
			
			double sum = 0.0;
			for(Double p: pList){
				sum += p;
			}
			
			apList.add(new Pair<String, Double>(topic, sum/cutoff));			
		}
		
		if(debug){
			for(Pair<String, Double> ap: apList){
				System.out.println(ap.toString());
			}
		}
		
		return apList;
	}
	
	private ArrayList<Pair<String, Double>> MAP(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> apList, int cutoff){
		ArrayList<Pair<String, Double>> mapList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.TIR_NTCIR11){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: apList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			mapList.add(new Pair<String, Double>("all\t\tMAP@"+Integer.toString(cutoff), allSum/apList.size()));
			mapList.add(new Pair<String, Double>("past\t\tMAP@"+Integer.toString(cutoff), pastSum/pastCount));
			mapList.add(new Pair<String, Double>("recency\t\tMAP@"+Integer.toString(cutoff), recencySum/recencyCount));
			mapList.add(new Pair<String, Double>("future\t\tMAP@"+Integer.toString(cutoff), futureSum/futureCount));
			mapList.add(new Pair<String, Double>("atemporal\tMAP@"+Integer.toString(cutoff), atemporalSum/atemporalCount));
			
			if(debug){
				for(Pair<String, Double> map: mapList){
					System.out.println(map.toString());
				}
			}
		}
		
		return mapList;
	}
	
	/**
	 * Metric: MSnDCG@k
	 * **/
	private ArrayList<Pair<String, Double>> MSnDCG(int cutoff){
		ArrayList<Pair<String, Double>> MSnDCGList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){
			//run
			ArrayList<Integer> runItemList = _sysRun.get(topic);
			double runCumuGainValue = 0.0;
			for(int i=0; i<cutoff; i++){
				Integer runItemID = runItemList.get(i);
				if(_itemReleMap.containsKey(runItemID)){
					HashMap<String, Integer> releMap = _itemReleMap.get(runItemID);
					if(releMap.containsKey(topic)){
						Integer runGainValue = _itemReleMap.get(runItemID).get(topic);
						runCumuGainValue += (runGainValue*1.0/Math.log10(i+2));
					}					
				}				
			}
			
			//ideal
			ArrayList<Pair<Integer, Integer>> idealItemList = _topicToReleItemMap.get(topic);
			double idealCumuGainValue = 0.0;
			for(int j=0; j<cutoff; j++){
				Integer idealGainValue = idealItemList.get(j).second;
				idealCumuGainValue += (idealGainValue*1.0/Math.log10(j+2));
			}
			
			//MSnDCG@k
			MSnDCGList.add(new Pair<String, Double>(topic, runCumuGainValue/idealCumuGainValue));			
		}
		
		if(debug){
			for(Pair<String, Double> p: MSnDCGList){
				System.out.println(p.toString());
			}
		}
		
		return MSnDCGList;
	}
	
	private ArrayList<Pair<String, Double>> avgMSnDCG(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> MSnDCGList, int cutoff){
		ArrayList<Pair<String, Double>> avgList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.TIR_NTCIR11){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: MSnDCGList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			avgList.add(new Pair<String, Double>("all\t\tMSnDCG@"+Integer.toString(cutoff), allSum/MSnDCGList.size()));
			avgList.add(new Pair<String, Double>("past\t\tMSnDCG@"+Integer.toString(cutoff), pastSum/pastCount));
			avgList.add(new Pair<String, Double>("recency\t\tMSnDCG@"+Integer.toString(cutoff), recencySum/recencyCount));
			avgList.add(new Pair<String, Double>("future\t\tMSnDCG@"+Integer.toString(cutoff), futureSum/futureCount));
			avgList.add(new Pair<String, Double>("atemporal\tMSnDCG@"+Integer.toString(cutoff), atemporalSum/atemporalCount));
			
			if(debug){
				for(Pair<String, Double> p: avgList){
					System.out.println(p.toString());
				}
			}
		}
		
		return avgList;
		
	}
	
	/**
	 * Metric: nDCG@k
	 * **/
	private ArrayList<Pair<String, Double>> nDCG(int base, int cutoff){
		ArrayList<Pair<String, Double>> nDCGList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){
			//run i<b
			ArrayList<Integer> runItemList = _sysRun.get(topic);
			double runCumuGainValue = 0.0;
			for(int i=0; i<base; i++){
				Integer runItemID = runItemList.get(i);
				if(_itemReleMap.containsKey(runItemID)){
					HashMap<String, Integer> releMap = _itemReleMap.get(runItemID);
					if(releMap.containsKey(topic)){
						Integer runGainValue = _itemReleMap.get(runItemID).get(topic);
						runCumuGainValue += runGainValue;
					}					
				}
			}
			//i>=b
			for(int i=base; i<cutoff; i++){
				Integer runItemID = runItemList.get(i);
				if(_itemReleMap.containsKey(runItemID)){
					HashMap<String, Integer> releMap = _itemReleMap.get(runItemID);
					if(releMap.containsKey(topic)){
						Integer runGainValue = _itemReleMap.get(runItemID).get(topic);
						runCumuGainValue += (runGainValue*1.0/(Math.log10(i+1)/Math.log10(base)));
					}					
				}				
			}
			
			//ideal 
			ArrayList<Pair<Integer, Integer>> idealItemList = _topicToReleItemMap.get(topic);
			double idealCumuGainValue = 0.0;
			//i<b			
			for(int j=0; j<base; j++){
				Integer idealGainValue = idealItemList.get(j).second;
				idealCumuGainValue += idealGainValue;
			}
			//i>=b
			for(int j=base; j<cutoff; j++){
				Integer idealGainValue = idealItemList.get(j).second;
				idealCumuGainValue += (idealGainValue*1.0/(Math.log10(j+1)/Math.log10(base)));
			}
			
			//MSnDCG@k
			nDCGList.add(new Pair<String, Double>(topic, runCumuGainValue/idealCumuGainValue));			
		}
		
		if(debug){
			for(Pair<String, Double> p: nDCGList){
				System.out.println(p.toString());
			}
		}
		
		return nDCGList;
	}
	
	private ArrayList<Pair<String, Double>> avgNDCG(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> nDCGList, int cutoff){
		ArrayList<Pair<String, Double>> avgList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.TIR_NTCIR11){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: nDCGList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			avgList.add(new Pair<String, Double>("all\t\tnDCG@"+Integer.toString(cutoff), allSum/nDCGList.size()));
			avgList.add(new Pair<String, Double>("past\t\tnDCG@"+Integer.toString(cutoff), pastSum/pastCount));
			avgList.add(new Pair<String, Double>("recency\t\tnDCG@"+Integer.toString(cutoff), recencySum/recencyCount));
			avgList.add(new Pair<String, Double>("future\t\tnDCG@"+Integer.toString(cutoff), futureSum/futureCount));
			avgList.add(new Pair<String, Double>("atemporal\tnDCG@"+Integer.toString(cutoff), atemporalSum/atemporalCount));
			
			if(debug){
				for(Pair<String, Double> p: avgList){
					System.out.println(p.toString());
				}
			}
		}
		
		return avgList;
		
	}
	
	/**
	 * Metric: ERR@k
	 * **/
	private double satisPro(int releInt){
		//version: power
		//return (Math.pow(2, releInt)-1)/4;
		//version: use highest relevance level
		return releInt*1.0/(2+1);
	}
	private ArrayList<Pair<String, Double>> ERR(int cutoff){
		ArrayList<Pair<String, Double>> errList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){

			ArrayList<Integer> sysList = _sysRun.get(topic);
						
			double err = 0.0;
			double disPro = 1.0;
			
			for(int i=0; i<cutoff; i++){
				Integer itemID = sysList.get(i);
				if(_itemReleMap.containsKey(itemID)){
					HashMap<String, Integer> releMap = _itemReleMap.get(itemID);
					if(releMap.containsKey(topic) && releMap.get(topic)>0){
						double satPro = satisPro(releMap.get(topic));
						double tem = 1.0/(i+1)*satPro*disPro;
						
						err += tem;
						disPro *= (1-satPro);
					}
				}
			}
			
			errList.add(new Pair<String, Double>(topic, err));			
		}
		
		if(debug){
			for(Pair<String, Double> ap: errList){
				System.out.println(ap.toString());
			}
		}
		
		return errList;
	}
	
	private ArrayList<Pair<String, Double>> avgERR(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> errList, int cutoff){
		ArrayList<Pair<String, Double>> avgList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.TIR_NTCIR11){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: errList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			avgList.add(new Pair<String, Double>("all\t\tERR@"+Integer.toString(cutoff), allSum/errList.size()));
			avgList.add(new Pair<String, Double>("past\t\tERR@"+Integer.toString(cutoff), pastSum/pastCount));
			avgList.add(new Pair<String, Double>("recency\t\tERR@"+Integer.toString(cutoff), recencySum/recencyCount));
			avgList.add(new Pair<String, Double>("future\t\tERR@"+Integer.toString(cutoff), futureSum/futureCount));
			avgList.add(new Pair<String, Double>("atemporal\tERR@"+Integer.toString(cutoff), atemporalSum/atemporalCount));
			
			if(debug){
				for(Pair<String, Double> p: avgList){
					System.out.println(p.toString());
				}
			}
		}
		
		return avgList;
		
	}
	
	/**
	 * Metric: nERR@k
	 * **/
	private ArrayList<Pair<String, Double>> nERR(int cutoff){
		ArrayList<Pair<String, Double>> nERRList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){

			ArrayList<Integer> sysList = _sysRun.get(topic);
						
			//for run
			double runERR = 0.0;
			double runDisPro = 1.0;			
			for(int i=0; i<cutoff; i++){
				Integer runItemID = sysList.get(i);
				if(_itemReleMap.containsKey(runItemID)){
					HashMap<String, Integer> runReleMap = _itemReleMap.get(runItemID);
					if(runReleMap.containsKey(topic) && runReleMap.get(topic)>0){
						double runSatPro = satisPro(runReleMap.get(topic));
						double tem = 1.0/(i+1)*runSatPro*runDisPro;
						
						runERR += tem;
						runDisPro *= (1-runSatPro);
					}
				}
			}
			
			//for ideal
			ArrayList<Pair<Integer, Integer>> idealReleMap = _topicToReleItemMap.get(topic);
			
			double idealERR = 0.0;
			double idealDisPro = 1.0;
			for(int i=0; i<cutoff; i++){
				Pair<Integer, Integer> idealItem = idealReleMap.get(i);
				double idealSatPro = satisPro(idealItem.second);
				double tem = 1.0/(i+1)*idealSatPro*idealDisPro;
				
				idealERR += tem;
				idealDisPro *= (1-idealSatPro);
			}
						
			nERRList.add(new Pair<String, Double>(topic, runERR/idealERR));			
		}
		
		if(debug){
			for(Pair<String, Double> ap: nERRList){
				System.out.println(ap.toString());
			}
		}
		
		return nERRList;
	}
	
	private ArrayList<Pair<String, Double>> avgNERR(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> nERRList, int cutoff){
		ArrayList<Pair<String, Double>> avgList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.TIR_NTCIR11){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: nERRList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			avgList.add(new Pair<String, Double>("all\t\tnERR@"+Integer.toString(cutoff), allSum/nERRList.size()));
			avgList.add(new Pair<String, Double>("past\t\tnERR@"+Integer.toString(cutoff), pastSum/pastCount));
			avgList.add(new Pair<String, Double>("recency\t\tnERR@"+Integer.toString(cutoff), recencySum/recencyCount));
			avgList.add(new Pair<String, Double>("future\t\tnERR@"+Integer.toString(cutoff), futureSum/futureCount));
			avgList.add(new Pair<String, Double>("atemporal\tnERR@"+Integer.toString(cutoff), atemporalSum/atemporalCount));
			
			if(debug){
				for(Pair<String, Double> p: avgList){
					System.out.println(p.toString());
				}
			}
		}
		
		return avgList;
		
	}
	
	/**
	 * 
	 * **/
	
	//check with ntcireval
	public void check(ArrayList<Pair<String, Double>> apList){
		try {
			Vector<Double> teVector = new Vector<Double>();
			Vector<Double> myVector = new Vector<Double>();
			//
			String teFile = "C:/Users/cicee/Desktop/TIR-Eval/TUTA1-TIR-RUN-1.20140808clean.nev";
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(teFile);			
		
			for(String lineString: lineList){
				
				if(lineString.indexOf("AP@0020=")>=0){
					
					//arrayStrings = lineString.split(" ");
					if(lineString.indexOf("0.") >= 0){
						//System.out.println(lineString.substring(lineString.indexOf("0.")));
						teVector.add(Double.parseDouble(lineString.substring(lineString.indexOf("0."))));
					}else{
						//System.out.println(lineString.substring(lineString.indexOf("1.")));
						teVector.add(Double.parseDouble(lineString.substring(lineString.indexOf("1."))));
					}			
				}
			}		
						
			DecimalFormat df = new DecimalFormat("0.0000");
			for(Pair<String, Double> ap: apList){
				myVector.add(Double.parseDouble(df.format(ap.second)));
			}
			//
			System.out.println("te size:\t"+teVector.size());
			System.out.println("my size:\t"+myVector.size());
			//
			int size = Math.min(teVector.size(), myVector.size());
			for(int i=0; i<size; i++){
				if(!teVector.get(i).equals(myVector.get(i))){
					System.out.println(teVector.get(i));
					System.out.println(myVector.get(i));
					System.out.println("un:\t"+(i+1)+"\t"+apList.get(i).toString());
					//break;
				}
			}			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	/////
	
	public static void main(String []args){
		//1 precision test
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_Temporalia/FinalRuns/TUTA1-TIR/TUTA1-TIR-RUN-1";
		String standardReleFile = "C:/Users/cicee/Desktop/TIR-Eval/tir_formalrun_20140808clean.qrels";
		
		IREval irEval = new IREval();
		irEval.loadSysRun(EVAL_TYPE.TIR_NTCIR11, sysRunFile);
		irEval.loadStandardReleFile(EVAL_TYPE.TIR_NTCIR11, standardReleFile);
		
		//P@k
		//ArrayList<Pair<String, Double>> precisionList = irEval.precision(20);
		//irEval.avgPrecison(EVAL_TYPE.TIR_NTCIR11, precisionList, 20);
		
		//MSnDCG@k
		//ArrayList<Pair<String, Double>> MSnDCGList = irEval.MSnDCG(20);
		//irEval.avgMSnDCG(EVAL_TYPE.TIR_NTCIR11, MSnDCGList, 20);
		
		//nDCG@k
		//ArrayList<Pair<String, Double>> nDCGList = irEval.nDCG(2, 20);
		//irEval.avgNDCG(EVAL_TYPE.TIR_NTCIR11, nDCGList, 20);
		
		//AP & MAP ! ntcireval may be wrong
		//ArrayList<Pair<String, Double>> apList = irEval.AP(20);
		//irEval.check(apList);
		//irEval.MAP(EVAL_TYPE.TIR_NTCIR11, apList, 20);
		
		//ERR@k ！
		//ArrayList<Pair<String, Double>> errList = irEval.ERR(20);
		//irEval.avgERR(EVAL_TYPE.TIR_NTCIR11, errList, 20);
		
		//nERR@k
		ArrayList<Pair<String, Double>> nERRList = irEval.nERR(20);
		irEval.avgERR(EVAL_TYPE.TIR_NTCIR11, nERRList, 20);
	}
	

}
