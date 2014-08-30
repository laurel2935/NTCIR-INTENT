package org.archive.ireval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.archive.util.tuple.Pair;

public class TwoLevelTopic {
	
	String _id;
	String _topic;
	
	//element: fls and its probability
	ArrayList<Pair<String, Double>> _flsList;
	//element: a set of example subtopic string, i-th element corresponds to the i-th element of _flsList
	ArrayList<ArrayList<String>> _flsExampleSetList;
	//key: flsStr, value: flsID
	HashMap<String, HashSet<Integer>> _flsStrMap;
	
	//element: second-level subtopic set, i-the element corresponds to the i-th element of _flsList
	ArrayList<ArrayList<Pair<String, Double>>> _slsSetList;
	//key: pair of flsID & slsID, value: set of example subtopic strings	
	HashMap<Pair<Integer, Integer>,	ArrayList<String>> _slsExampleSetMap;
	//key: slsStr, value: pair of flsID & slsID
	HashMap<String, HashSet<Pair<Integer, Integer>>> _slsExampleMap;
	
	//mapping from sls's content to fls's content 
	//key: sls's content, value: pair of flsID, slsID(itself)
	HashMap<String, Pair<Integer, Integer>> _slsContentMap;
	//
	TwoLevelTopic(String id, String topic){
		this._id = id;
		this._topic = topic;
		
		this._flsList = new ArrayList<Pair<String,Double>>();
		this._flsExampleSetList = new ArrayList<ArrayList<String>>();
		this._flsStrMap = new HashMap<String, HashSet<Integer>>();
		
		this._slsSetList = new ArrayList<ArrayList<Pair<String,Double>>>();
		this._slsExampleSetMap = new HashMap<Pair<Integer,Integer>, ArrayList<String>>();
		this._slsExampleMap = new HashMap<String, HashSet<Pair<Integer, Integer>>>();
	}
	
	public void setFlsList(ArrayList<Pair<String, Double>> flsList){
		this._flsList = flsList;
	}
	
	public void setFlsExampleSetList(ArrayList<ArrayList<String>> flsExampleSetList){
		this._flsExampleSetList = flsExampleSetList;
	}
	
	public void setFlsStrMap(HashMap<String, HashSet<Integer>> flsStrMap){
		this._flsStrMap = flsStrMap;
	}
	
	public void setSlsSetList(ArrayList<ArrayList<Pair<String, Double>>> slsSetList){
		this._slsSetList = slsSetList;
	}
	
	public void setSlsExampleSetMap(HashMap<Pair<Integer, Integer>,	ArrayList<String>> slsExampleSetMap){
		this._slsExampleSetMap = slsExampleSetMap;
	}
	
	public void setSlsStrMap(HashMap<String, HashSet<Pair<Integer, Integer>>> slsExampleMap){
		this._slsExampleMap = slsExampleMap;
	}
	
	public void getSlsContentMap(){
		_slsContentMap = new HashMap<String, Pair<Integer,Integer>>();
		
		int flsID=1;
		for(; flsID<=_flsList.size(); flsID++){
			ArrayList<Pair<String, Double>> slsSet = _slsSetList.get(flsID-1);
			
			int slsID = 1;
			for(; slsID<=slsSet.size(); slsID++){
				_slsContentMap.put(slsSet.get(slsID-1).getFirst(), new Pair<Integer, Integer>(flsID, slsID));
			}
		}
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append(_id+"\t"+_topic+"\n");
		int flsID = 1;
		for(; flsID <= _flsList.size(); flsID++){
			Pair<String, Double> fls = _flsList.get(flsID-1);
			buffer.append("  "+flsID+"-"+fls.getFirst()+"\t"+fls.getSecond()+"\n");
			
			ArrayList<String> flsExampleList = _flsExampleSetList.get(flsID-1);
			buffer.append("  fls-example: "+flsExampleList.size()+" : "+flsExampleList.toString()+"\n");
			
			ArrayList<Pair<String, Double>> slsSet = _slsSetList.get(flsID-1);
			int slsID = 1;
			for(; slsID<= slsSet.size(); slsID++){
				Pair<String, Double> sls = slsSet.get(slsID-1);
				buffer.append("\t"+slsID+"-"+sls.getFirst()+"\t"+sls.getSecond()+"\n");
				
				Pair<Integer, Integer> pair = new Pair<Integer, Integer>(flsID, slsID);
				int sCount = 1;
				for(Entry<String, HashSet<Pair<Integer, Integer>>> slsEntry: _slsExampleMap.entrySet()){
					HashSet<Pair<Integer, Integer>> slsESet = slsEntry.getValue();
					if(slsESet.contains(pair)){
						buffer.append("\tslsExample-"+(sCount++)+": "+slsEntry.getKey()+"\n");
					}
				}				
			}			
		}
		
		return buffer.toString();
	}
}
