package org.archive.ntcir.sm;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.dataset.ntcir.sm.SMSubtopicItem;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.dataset.ntcir.sm.SubtopicInstance;
import org.archive.ml.clustering.ap.abs.ClusterInteger;
import org.archive.ml.clustering.ap.abs.ClusterString;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.ufl.K_UFL;
import org.archive.ml.ufl.K_UFL.UFLMode;
import org.archive.nlp.chunk.lpt.ltpService.LTML;
import org.archive.nlp.chunk.lpt.ltpService.Word;
import org.archive.nlp.lcs.LCSScaner;
import org.archive.nlp.qpunctuation.QueryPreParser;
import org.archive.ntcir.sm.RunParameter.ClusteringFunction;
import org.archive.ntcir.sm.RunParameter.SimilarityFunction;
import org.archive.ntcir.sm.clustering.ap.APClustering;
import org.archive.ntcir.sm.similarity.editdistance.GregorEditDistance;
import org.archive.ntcir.sm.similarity.editdistance.StandardEditDistance;
import org.archive.ntcir.sm.similarity.editdistance.definition.SuperString;
import org.archive.ntcir.sm.similarity.hownet.concept.LiuConceptParser;
import org.archive.ntcir.sm.similarity.wordnet.WordNetSimilarity;
import org.archive.util.Pair;
import org.archive.util.Language.Lang;
import org.archive.util.io.IOText;
import org.archive.util.tuple.StrDouble;
import org.archive.util.tuple.StrInt;
import org.archive.util.tuple.StrStr;

public class SubtopicMining {
	private final static boolean DEBUG = true;
	
	public static HashMap<Pair,Double> _simCache = new HashMap<Pair, Double>();
	
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
						
			chRun(runParameter, writer);
			
		}else{
			
			enRun(runParameter, writer);
			
		}
	}	
	//
	private static double getMedian(ArrayList<InteractionData> releMatrix){	
		if(0 == releMatrix.size()){
			System.err.println("Zero ReleMatrix Error!");
			return 0.0;
		}
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
	
	private void chRun(RunParameter runParameter, BufferedWriter writer) throws Exception{
			
		for(int t=0; t<runParameter.topicList.size(); t++){
			
			SMTopic smTopic = runParameter.topicList.get(t);
			
			if(DEBUG){
				System.out.println("Processing topic "+ smTopic.getID()+"\t"+smTopic.getTopicText());
			}
			
			if(smTopic.belongToBadCase()){
				continue;
			}
			
			RankedList rankedList = null;
			
			if(null != smTopic.polysemyList){
				rankedList = getRankedListForPolyTopic(smTopic, runParameter.runTitle);
			}else if(smTopic.CompleteSentence){
				rankedList = new RankedList();
				
				RankedRecord record = new RankedRecord(smTopic.getID(), smTopic.getTopicText(), 1, 1.0,
						smTopic.getTopicText(), 1, 1.0, runParameter.runTitle);
				
				rankedList.addRecord(record);				
			}else{
				if(ClusteringFunction.StandardAP == runParameter.cFunction){
					ArrayList<ItemCluster> itemClusterList = apCluster(smTopic.smSubtopicItemList, Lang.Chinese);
					for(ItemCluster itemCluster: itemClusterList){
						itemCluster.calInstanceNumber(smTopic.getTopicText());
						Collections.sort(itemCluster.itemList);
					}
					Collections.sort(itemClusterList);
					double sum = 0.0;
					for(ItemCluster itemCluster: itemClusterList){
						sum += itemCluster.instanceNumber;
					}
					for(ItemCluster itemCluster: itemClusterList){
						itemCluster.weight = itemCluster.instanceNumber/sum;
						for(SMSubtopicItem item: itemCluster.itemList){
							item.weight = itemCluster.weight*(item.subtopicInstanceGroup.size()*1.0/itemCluster.instanceNumber);
						}
					}
					//--
					int itemN = (int)sum;					
					int seatN = Math.min(50, itemN);	
					
					Vector<Double> quotient = new Vector<Double>(itemClusterList.size());
					Vector<Double> voteArray = new Vector<Double>(itemClusterList.size());
					Vector<Double> doneSeatArray = new Vector<Double>(itemClusterList.size());
					
					for(int k=0; k<itemClusterList.size(); k++){
						quotient.add(0.0);
						voteArray.add(itemClusterList.get(k).weight);
						//voteArray[k] = itemClusterList.get(k).weight;
						doneSeatArray.add(0.0);
					}
					
					rankedList = new RankedList();
					
					for(int s=0; s<seatN; s++){
						calQuotient(quotient, voteArray, doneSeatArray);
						int i = getMaxIndex(quotient);
						if(itemClusterList.get(i).itemList.size() > 0){
							SMSubtopicItem item = itemClusterList.get(i).itemList.get(0);
							RankedRecord record = new RankedRecord(smTopic.getID(), itemClusterList.get(i).exemplar.subtopicInstanceGroup.get(0)._text,
									(i+1), itemClusterList.get(i).weight, item.subtopicInstanceGroup.get(0)._text, (s+1), item.weight, runParameter.runTitle);
							
							rankedList.addRecord(record);
							
							itemClusterList.get(i).itemList.remove(0);
							
							doneSeatArray.set(i, doneSeatArray.get(i)+1);
						}else {
							//quotient[i] = Double.MIN_VALUE;
							quotient.set(i, -1.0);
						}	
					}					
					
				}else if(ClusteringFunction.K_UFL == runParameter.cFunction){
					ArrayList<InteractionData> releMatrix = getChReleMatrix(smTopic, runParameter);
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
			}
			///*
			if(null != rankedList && null!=rankedList.recordList){				
				for(RankedRecord record: rankedList.recordList){
					writer.write(record.toString());
					writer.newLine();
				}
			}else{
				new Exception("Null RankedList Error!").printStackTrace();
			}
			//*/
		}
		//
		///*
		writer.flush();
		writer.close();
		//*/
	}
		
	public RankedList getRankedListForPolyTopic(SMTopic smTopic, String runTitle){
		
		System.out.println("processing polysemy topic:\t"+smTopic.toString());
		System.out.println("item number:\t"+smTopic.smSubtopicItemList.size());
		
		ArrayList<String> polysemyList = smTopic.polysemyList;
		
		HashMap<String, PolyCluster> polyClusterMap = new HashMap<String, PolyCluster>();
		PolyCluster otherPolyCluster = new PolyCluster("unk");
		
		for(SMSubtopicItem item: smTopic.smSubtopicItemList){
			String bestPolyStr = bestMatch(polysemyList, item, smTopic.getTopicText());
			if(null != bestPolyStr){
				if(polyClusterMap.containsKey(bestPolyStr)){
					polyClusterMap.get(bestPolyStr).addSMSubtopicItem(item);
				}else {
					PolyCluster polyCluster = new PolyCluster(bestPolyStr);
					polyCluster.addSMSubtopicItem(item);
					polyClusterMap.put(bestPolyStr, polyCluster);
				}
			}else {
				otherPolyCluster.addSMSubtopicItem(item);
			}
			
		}
		
		ArrayList<PolyCluster> polyClusterList = new ArrayList<PolyCluster>();
		for(Entry<String, PolyCluster> entry: polyClusterMap.entrySet()){
			PolyCluster polyCluster = entry.getValue();
			polyCluster.calInstanceNumber(smTopic.getTopicText());
			Collections.sort(polyCluster.smSubtopicItemList);
			polyClusterList.add(polyCluster);
		}
		
		Collections.sort(polyClusterList);	
		
		ArrayList<PolyCluster> clusterList = new ArrayList<PolyCluster>();
		
		if(polyClusterList.size() >= 5){
			for(int i=0; i<5; i++){
				clusterList.add(polyClusterList.get(i));
			}		
		}else{
			clusterList.addAll(polyClusterList);
			if(otherPolyCluster.smSubtopicItemList.size() > 0){
				Collections.sort(otherPolyCluster.smSubtopicItemList);
				clusterList.add(otherPolyCluster);
			}			
		}					
		
		if(DEBUG){
			int id = 1;
			for(PolyCluster polyCluster: polyClusterList){
				System.out.println("cluster-"+id+": instance number: "+polyCluster.instanceNumber);
				for(SMSubtopicItem item: polyCluster.smSubtopicItemList){
					System.out.println(item.subtopicInstanceGroup.size()+"\t"+item.itemDelegater._text);
				}
				System.out.println();
				id++;
			}
		}
		
		for(PolyCluster polyCluster: clusterList){
			polyCluster.delegaterItem = polyCluster.smSubtopicItemList.get(0);		
		}
		
		double sum = 0.0;		
		
		for(PolyCluster polyCluster: clusterList){
			sum += polyCluster.instanceNumber;
		}
		
		for(PolyCluster polyCluster: clusterList){
			polyCluster.weight = polyCluster.instanceNumber/sum;
		}
		
		for(PolyCluster polyCluster: clusterList){
			for(SMSubtopicItem item: polyCluster.smSubtopicItemList){
				item.weight = polyCluster.weight*(item.subtopicInstanceGroup.size()/(polyCluster.instanceNumber*1.0));
			}
		}
		
		return generateRankedList(smTopic, clusterList, runTitle);
		
	}
	
	public static RankedList generateRankedList(SMTopic smTopic, ArrayList<PolyCluster> clusterList, String runTitle){
		
		int itemN = 0;
		for(PolyCluster c: clusterList){
			itemN += c.smSubtopicItemList.size();			
		}
		int seatN = Math.min(50, itemN);
		System.out.println("seats:\t"+seatN);
		
		Vector<Double> quotient = new Vector<Double>(clusterList.size());
		//double [] quotient = new double[clusterList.size()];
		Vector<Double> voteVector = new Vector<Double>(clusterList.size());
		//double [] voteArray = new double[clusterList.size()];
		Vector<Double> doneSeatVector = new Vector<Double>(clusterList.size());
		//double [] doneSeatArray = new double[clusterList.size()];
		
		for(int k=0; k<clusterList.size(); k++){
			quotient.add(0.0);
			//voteArray[k] = clusterList.get(k).weight;
			voteVector.add(clusterList.get(k).weight);
			doneSeatVector.add(0.0);
		}
		//System.out.println("votes:\t"+voteVector);
		RankedList rankedList = new RankedList();
		
		for(int s=0; s<seatN; s++){
			calQuotient(quotient, voteVector, doneSeatVector);
			//System.out.println(quotient);
			int i = getMaxIndex(quotient);
			//System.out.println((i+1)+" seat for party "+(s+1));
			if(-1 == i){
				break;
			}
			if(clusterList.get(i).smSubtopicItemList.size() > 0){
				SMSubtopicItem item = clusterList.get(i).smSubtopicItemList.get(0);
				RankedRecord record = new RankedRecord(smTopic.getID(), clusterList.get(i).delegaterItem.subtopicInstanceGroup.get(0)._text,
						(i+1), clusterList.get(i).weight, item.subtopicInstanceGroup.get(0)._text, (s+1), item.weight, runTitle);
				
				rankedList.addRecord(record);
				
				clusterList.get(i).smSubtopicItemList.remove(0);
				
				//doneSeatArray[i] += 1;
				doneSeatVector.set(i, doneSeatVector.get(i)+1);
			}else {
				quotient.set(i, -1.0);
				//quotient[i] = Double.MIN_VALUE;
			}	
		}
		
		return rankedList;		
	}
	
	private static void calQuotient(Vector<Double> quotient, Vector<Double> voteArray, Vector<Double> doneSeatArray){
		//System.out.println("before Quo:\t"+quotient);
		for(int i=0; i<quotient.size(); i++){
			if(quotient.get(i) > -1.0){
				quotient.set(i, voteArray.get(i)/(doneSeatArray.get(i)+1));
				//quotient[i] = voteArray[i]/(doneSeatArray[i]+1);
			}		
		}		
		//System.out.println("after Quo:\t"+quotient);
	}
	
	private static int getMaxIndex(Vector<Double> array){
		//System.out.println(array);
		int i=-1;
		double maxValue = -1.0;
		for(int k=0; k<array.size(); k++){
			if(array.get(k) > maxValue){
				maxValue = array.get(k);
				i = k;
			}
		}
		return i;
	}
	
	public ArrayList<ItemCluster> apCluster(ArrayList<SMSubtopicItem> ItemList, Lang lang){
		double termIRWeight = 0.4; double phraseIRWeight = 0.6;
		
		ArrayList<InteractionData> releMatrix = new ArrayList<InteractionData>();
		
		for(int i=0; i<ItemList.size()-1; i++){
			SMSubtopicItem iItem = ItemList.get(i);			
			ArrayList<ArrayList<String>> iTermModifierGroupList = iItem.termModifierGroupList;
			ArrayList<ArrayList<String>> iPhraseModifierGroupList = iItem.phraseModifierGroupList;
			
			for(int j=i+1; j<ItemList.size(); j++){
				SMSubtopicItem jItem = ItemList.get(j);
				ArrayList<ArrayList<String>> jTermModifierGroupList = jItem.termModifierGroupList;
				ArrayList<ArrayList<String>> jPhraseModifierGroupList = jItem.phraseModifierGroupList;
				
				double simValue = 0.0;

				for(int k=0; k<iTermModifierGroupList.size(); k++){
					simValue += ((termIRWeight/iTermModifierGroupList.size())*
							getSimilarity(iTermModifierGroupList.get(k), jTermModifierGroupList.get(k), lang));
				}
				
				for(int k=0; k<iPhraseModifierGroupList.size(); k++){
					simValue += ((phraseIRWeight/iPhraseModifierGroupList.size())*
							getSimilarity(iPhraseModifierGroupList.get(k), jPhraseModifierGroupList.get(k), lang));
				}
				
				releMatrix.add(new InteractionData(Integer.toString(i), Integer.toString(j), simValue));
			}
		}
		
    	double lambda = 0.5;
    	int iterations = 20000;
    	int convits = 20;
    	double preferences = getMedian(releMatrix);    	
    	APClustering apClustering = new APClustering(lambda, iterations, convits, preferences, releMatrix);
    	apClustering.setParemeters();
    	
    	Map<Integer, ClusterInteger> clusterMap = (Map<Integer, ClusterInteger>)apClustering.run();
    	
    	//System.out.println(clusterMap.size());
    	
    	ArrayList<ItemCluster> itemClusterList = new ArrayList<ItemCluster>();
    	
    	for(Entry<Integer, ClusterInteger> entry: clusterMap.entrySet()){
    		ItemCluster itemCluster = new ItemCluster(ItemList.get(entry.getKey()));
    		Integer [] elementArray = entry.getValue().elements.toArray(new Integer[0]);
    		for(Integer e: elementArray){
    			itemCluster.addItem(ItemList.get(e));
    		}
    		itemClusterList.add(itemCluster);
    	}
    	
    	Collections.sort(itemClusterList);   	
    	
    	return itemClusterList;
	}
	
	class ItemCluster implements Comparable{
		SMSubtopicItem exemplar;
		ArrayList<SMSubtopicItem> itemList;
		int instanceNumber = 0;
		double weight;
		
		ItemCluster(SMSubtopicItem exemplar){
			this.exemplar = exemplar;
		}
		
		public void addItem(SMSubtopicItem item){
			if(null == this.itemList){
				this.itemList = new ArrayList<SMSubtopicItem>();
			}
			this.itemList.add(item);
		}
		
		public void calInstanceNumber(String topicText){
			if(null == this.itemList){
				this.instanceNumber = 0;
				for(SubtopicInstance instance: this.exemplar.subtopicInstanceGroup){
					if(instance._text.length()>0 && !instance._text.equals(topicText)){
						instanceNumber++;
					}
				}
			}else{
				this.instanceNumber = 0;
				for(SubtopicInstance instance: this.exemplar.subtopicInstanceGroup){
					if(instance._text.length()>0 && !instance._text.equals(topicText)){
						instanceNumber++;
					}
				}
				for(SMSubtopicItem item: this.itemList){
					for(SubtopicInstance instance: item.subtopicInstanceGroup){
						if(instance._text.length()>0 && !instance._text.equals(topicText)){
							instanceNumber++;
						}
					}
				}
			}
			
		}
		
		public int compareTo(Object o) {
			ItemCluster cmp = (ItemCluster) o;			
			if(this.instanceNumber > cmp.instanceNumber){
				return -1;
			}else if(this.instanceNumber < cmp.instanceNumber){
				return 1;
			}else{
				return 0;
			}
		}			
	}
	
	
	public static String bestMatch(ArrayList<String> polysemyList, SMSubtopicItem item, String exceptText){
		String bestPolyString = null;
		int maxsize = 0;
		for(String polyString: polysemyList){
			HashSet<String> matSet = matchedStr(polyString, item, exceptText);
			if(null != matSet){
				if(matSet.size() > maxsize){
					maxsize = matSet.size();
					bestPolyString = polyString;
				}
			}			
		}
		return bestPolyString;
	}
	
	public static HashSet<String> matchedStr(String polyString, SMSubtopicItem item, String exceptText){
		HashSet<String> set = new HashSet<String>();
		
		for(SubtopicInstance instance: item.subtopicInstanceGroup){
			Vector<String> strSet = new Vector<String>();
			strSet.add(instance._text);
			strSet.add(polyString);
			LCSScaner lcsScaner = new LCSScaner(strSet, Lang.Chinese);
			ArrayList<StrInt> lcsList = lcsScaner.enumerateLCS_AtLeastK(2);
			for(StrInt lcs: lcsList){
				if(lcs.getFirst().length() >= 2 && !lcs.getFirst().equals(exceptText)){
					set.add(lcs.getFirst());
				}
			}
		}
		if(set.size() > 0){
			return set;
		}else{
			return null;
		}		
	}
	
	private SMSubtopicItem getItem(SMTopic smTopic, String key){		
		int id = Integer.parseInt(key);
		return smTopic.smSubtopicItemList.get(id);		
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
		}else if(runParameter.simFunction == SimilarityFunction.AveragedSemanticSimilarity){
			double termIRWeight = 0.4; double phraseIRWeight = 0.6;	
			ArrayList<SMSubtopicItem> itemList = smTopic.smSubtopicItemList;
			
			//WordNetSimilarity.JCSimilarity_Average(word_1, word_2);
			for(int i=0; i<itemList.size()-1; i++){
				SMSubtopicItem iItem = itemList.get(i);			
				ArrayList<ArrayList<String>> iTermModifierGroupList = iItem.termModifierGroupList;
				ArrayList<ArrayList<String>> iPhraseModifierGroupList = iItem.phraseModifierGroupList;
				
				for(int j=i+1; j<itemList.size(); j++){
					SMSubtopicItem jItem = itemList.get(j);
					ArrayList<ArrayList<String>> jTermModifierGroupList = jItem.termModifierGroupList;
					ArrayList<ArrayList<String>> jPhraseModifierGroupList = jItem.phraseModifierGroupList;
					
					double simValue = 0.0;

					for(int k=0; k<iTermModifierGroupList.size(); k++){
						simValue += ((termIRWeight/iTermModifierGroupList.size())*
								getSimilarity(iTermModifierGroupList.get(k), jTermModifierGroupList.get(k), Lang.Chinese));
					}
					
					for(int k=0; k<iPhraseModifierGroupList.size(); k++){
						simValue += ((phraseIRWeight/iPhraseModifierGroupList.size())*
								getSimilarity(iPhraseModifierGroupList.get(k), jPhraseModifierGroupList.get(k), Lang.Chinese));
					}
					
					chReleMatrix.add(new InteractionData(Integer.toString(i), Integer.toString(j), simValue));
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
			
			if(smTopic.belongToBadCase()){
				continue;
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
		    			System.out.println("Exemplar-"+(id++)+":\t"+getItem(smTopic, key).itemDelegater.toString());
		    			Collection<String> memberSet = clusterMap.get(key).getElements();
		    			int mID = 1;
		    			for(String memKey: memberSet){
		    				System.out.println("\t"+(mID++)+":\t"+getItem(smTopic, memKey).itemDelegater.toString());
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
		}else if(runParameter.simFunction == SimilarityFunction.AveragedSemanticSimilarity){
			//WordNetSimilarity.JCSimilarity_Average(word_1, word_2);
			for(int i=0; i<itemList.size()-1; i++){
				SMSubtopicItem iItem = itemList.get(i);			
				ArrayList<ArrayList<String>> iTermModifierGroupList = iItem.termModifierGroupList;
				ArrayList<ArrayList<String>> iPhraseModifierGroupList = iItem.phraseModifierGroupList;
				
				for(int j=i+1; j<itemList.size(); j++){
					SMSubtopicItem jItem = itemList.get(j);
					ArrayList<ArrayList<String>> jTermModifierGroupList = jItem.termModifierGroupList;
					ArrayList<ArrayList<String>> jPhraseModifierGroupList = jItem.phraseModifierGroupList;
					
					double simValue = 0.0;

					for(int k=0; k<iTermModifierGroupList.size(); k++){
						simValue += ((termIRWeight/iTermModifierGroupList.size())*
								getSimilarity(iTermModifierGroupList.get(k), jTermModifierGroupList.get(k), Lang.English));
					}
					
					for(int k=0; k<iPhraseModifierGroupList.size(); k++){
						simValue += ((phraseIRWeight/iPhraseModifierGroupList.size())*
								getSimilarity(iPhraseModifierGroupList.get(k), jPhraseModifierGroupList.get(k), Lang.English));
					}
					
					enReleMatrix.add(new InteractionData(Integer.toString(i), Integer.toString(j), simValue));
				}
			}
			
			return enReleMatrix;
		}else{
			return null;
		}		
	}
	private double getSimilarity(ArrayList<String> aTermList, ArrayList<String> bTermList, Lang lang)
	{
		if(0==aTermList.size() || 0==bTermList.size()){
			return 0.0;
		}
		
		double simSum = 0.0;
		for(String a: aTermList){
			for(String b: bTermList){
				simSum += getSimilarity(a, b, lang);
			}
		}
		
		return simSum/(aTermList.size()+bTermList.size());
		
	}
	private static double getSimilarity(String aTerm, String bTerm, Lang lang){
		Pair sim_key = new Pair(aTerm, bTerm);
		Double simScore = null;
		if (null == (simScore=_simCache.get(sim_key))) {
			if(Lang.English == lang){
				simScore = WordNetSimilarity.JCSimilarity_Average(aTerm, bTerm);
			}else{
				simScore = LiuConceptParser.getInstance().getSimilarity(aTerm, bTerm);
			}
			
			_simCache.put(sim_key, simScore);
		}
		return simScore;
	}
	
	private static ArrayList<InteractionData> getCostMatrix(ArrayList<InteractionData> interList){
		ArrayList<InteractionData> costMatrix = new ArrayList<InteractionData>();
		for(InteractionData itr: interList){
			costMatrix.add(new InteractionData(itr.getFrom(), itr.getTo(), -itr.getSim()));
		}
		return costMatrix;		
	}
	
	private static Random noiseGenerator = new Random();
	private static final double epsilon = 0.000001;
	protected static double generateNoiseHelp() {			
        double ran_tmp = noiseGenerator.nextDouble();
        double noise_tmp = epsilon * ran_tmp;
        return noise_tmp;
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
				SimilarityFunction.AveragedSemanticSimilarity, ClusteringFunction.StandardAP);
		try {
			smMining.run(runParameter);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}	
	}
	

}
