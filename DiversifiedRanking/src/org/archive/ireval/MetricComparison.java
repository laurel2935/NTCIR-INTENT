package org.archive.ireval;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR11_TOPIC_TYPE;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.util.io.IOText;
import org.archive.util.tuple.Pair;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.Triple;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;


/**
 * 
 * For the comparative analysis of metrics for novelty and diversity
 * 
 * **/

public class MetricComparison {
	
	public static final boolean debug = false;
	//subtopic-level
	private static enum STLevel{FLS, SLS};	
	
	//alphaNDCg
	private static double ALPHA = 0.5;
	//D#-nDCG, DIN#-nDCG
	private static double GAMA = 0.5;
	
	//
	public static ArrayList<TwoLevelTopic> _2LTList = new ArrayList<TwoLevelTopic>();
	//{queryid -> TwoLevelTopic}
	public static HashMap<String, TwoLevelTopic> _2LTMap = new HashMap<String, TwoLevelTopic>();

	/**
	 * Two-level hierarchy of subtopics
	 * **/
	public static void load2LT(String file){
		try {
			
			SAXBuilder saxBuilder = new SAXBuilder();
		    Document xmlDoc = saxBuilder.build(new File(file)); 
		    //new InputStreamReader(new FileInputStream(targetFile), DEFAULT_ENCODING)
		    
		    Element rootElement = xmlDoc.getRootElement();
		    
		    List<Element> topicElementList = rootElement.getChildren("topic");
		    
		    for(Element topicElement: topicElementList){
		    	String id = topicElement.getAttributeValue("id");
		    	String topic = topicElement.getAttributeValue("content");
		    	
		    	TwoLevelTopic twoLT = new TwoLevelTopic(id, topic);
		    	
		    	List<Element> flsElementList = topicElement.getChildren("fls");
		    	
		    	int flsID=1;
		    	//fls
		    	ArrayList<Pair<String, Double>> flsList = new ArrayList<Pair<String,Double>>(); 
		    	ArrayList<ArrayList<String>> flsExampleSetList = new ArrayList<ArrayList<String>>();
		    	HashMap<String, HashSet<Integer>> flsStrMap = new HashMap<String, HashSet<Integer>>();
		    	//sls
		    	ArrayList<ArrayList<Pair<String, Double>>> slsSetList = new ArrayList<ArrayList<Pair<String,Double>>>();
		    	HashMap<Pair<Integer, Integer>,	ArrayList<String>> slsExampleSetMap = new HashMap<Pair<Integer,Integer>, ArrayList<String>>();		    	
		    	HashMap<String, HashSet<Pair<Integer, Integer>>> slsExampleMap = new HashMap<String, HashSet<Pair<Integer, Integer>>>();
		    	
		    	for(; flsID<=flsElementList.size(); flsID++){
		    		Element flsElement = flsElementList.get(flsID-1);
		    		//fls and its possibility
		    		flsList.add(new Pair<String, Double>(flsElement.getAttributeValue("content"),
		    				Double.parseDouble(flsElement.getAttributeValue("poss"))));
		    		
		    		//exampleSet of fls
		    		Element examplesElement = flsElement.getChild("examples");
		    		ArrayList<String> exampleSet = new ArrayList<String>();
		    		List<Element> exampleElementList = examplesElement.getChildren("example");
		    		for(Element exampleElement: exampleElementList){
		    			String flsExample = exampleElement.getText(); 
		    			exampleSet.add(flsExample);
		    			
		    			if(flsStrMap.containsKey(flsExample)){
		    				flsStrMap.get(flsExample).add(flsID);
		    			}else{
		    				HashSet<Integer> flsIDSet = new HashSet<Integer>();
		    				flsIDSet.add(flsID);
		    				flsStrMap.put(flsExample, flsIDSet);
		    			}
		    		}
		    		flsExampleSetList.add(exampleSet);
		    		
		    		//
		    		int slsID = 1;
		    		ArrayList<Pair<String, Double>> slsSet = new ArrayList<Pair<String,Double>>();
		    		List<Element> slsElementList = flsElement.getChildren("sls");
		    		for(; slsID<=slsElementList.size(); slsID++){
		    			Element slsElement = slsElementList.get(slsID-1);
		    			slsSet.add(new Pair<String, Double>(slsElement.getAttributeValue("content"),
		    					Double.parseDouble(slsElement.getAttributeValue("poss"))));
		    			//
		    			Pair<Integer, Integer> idPair = new Pair<Integer, Integer>(flsID, slsID);
		    			ArrayList<String> slsExampleList = new ArrayList<String>();
		    			List<Element> slsExampleElementList = slsElement.getChildren("example");
		    			for(Element slsExampleElement: slsExampleElementList){
		    				String slsExample = slsExampleElement.getText();
		    				slsExampleList.add(slsExample);
		    				
		    				if(slsExampleMap.containsKey(slsExample)){
		    					slsExampleMap.get(slsExample).add(idPair);
		    				}else{
		    					HashSet<Pair<Integer, Integer>> pairSet = new HashSet<Pair<Integer,Integer>>();
		    					pairSet.add(idPair);
		    					slsExampleMap.put(slsExample, pairSet);
		    				}	    				
		    			}
		    			slsExampleSetMap.put(idPair, slsExampleList);
		    		}
		    		slsSetList.add(slsSet);
		    	}
		    	
		    	twoLT.setFlsList(flsList);
		    	twoLT.setFlsExampleSetList(flsExampleSetList);
		    	twoLT.setFlsStrMap(flsStrMap);
		    	
		    	twoLT.setSlsSetList(slsSetList);
		    	twoLT.setSlsExampleSetMap(slsExampleSetMap);
		    	twoLT.setSlsStrMap(slsExampleMap);
		    	
		    	twoLT.getSlsContentMap();
		    	
		    	_2LTList.add(twoLT);
		    	_2LTMap.put(id, twoLT);
		    }
		    
		    if(debug){
		    	System.out.println(_2LTMap.size());
		    	System.out.println();
		    	/*
		    	System.out.println(_2LTMap.get("0001").toString());
		    	
		    	//
		    	TwoLevelTopic t = _2LTMap.get("0001");
		    	ArrayList<Pair<String, Double>> flsList = t._flsList;
		    	ArrayList<ArrayList<Pair<String, Double>>> slsSetList = t._slsSetList;
		    	
		    	int flsID = 1;
		    	
		    	double sumFls = 0.0, sumSls = 0.0;
		    	
		    	for(; flsID<=flsList.size(); flsID++){
		    		Pair<String, Double> fls = flsList.get(flsID-1);
		    		sumFls += fls.getSecond();
		    		System.out.println(fls.getSecond());
		    		
		    		ArrayList<Pair<String, Double>> slsSet = slsSetList.get(flsID-1);
		    		Double slsSum = 0.0;
		    		for(Pair<String, Double> sls: slsSet){
		    			slsSum += sls.getSecond();
		    		}
		    		sumSls += slsSum;
		    		System.out.println("\t"+slsSum);
		    	}	   
		    	
		    	System.out.println("sumFls:\t"+sumFls);
		    	System.out.println("sumSls:\t"+sumSls);
		    	*/
		    }		    
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	
	////R, I, V:	mapping functions from graded relevance assessments to relevance probabilities or numerical values
	//R:editorial relevance probability
	private static double R(int grade, int mode){
		return ((Math.pow(2, grade)-1)/Math.pow(2, mode));		
	}
	//I: binary mapping
	private static double I(int grade){
		return grade>0?1:0;
	}
	//V: 
	private static double V(int grade){
		return grade;
	}
	
	
	//{docid -> {queryid -> {flsID -> relevanceLevel}}}
	private static HashMap<String, HashMap<String, HashMap<Integer, Integer>>> fls_DocReleMap = null;
	//{docid -> {queryid -> {slsID -> relevanceLevel}}}
	private static HashMap<String, HashMap<String, HashMap<Integer, Integer>>> sls_DocReleMap = null;
	
	/**
	 * Extract qrel xml file
	 * Qrel's xml file consists of two parts:
	 * <1> first relevance part corresponds to relevance judgments for all topics
	 * <2> second relevance part merely corresponds to broad and ambiguous queries, besides the same component,
	 *     it also indicates the 2nd-level subtopic to which a document is in fact relevant.
	 *     And a document may be relevant to several 2nd-level subtopics. 
	 *     
	 * Return
	 * <1>  docid | queryid | releLevel
	 * <2>  docid | queryid+"\t"+slsStr | releLevel
	 * **/
	public static ArrayList<Triple<String, String, Integer>> getXmlQrel(String qrelDir, 
			NTCIRLoader.NTCIR_EVAL_TASK eval, NTCIRLoader.NTCIR11_TOPIC_TYPE type){
		
		//String dir = "H:/v-haiyu/TaskPreparation/Ntcir11-IMine/Eval-IMine/20140830/";
		
		String drFile = null;
		
		if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
			drFile = qrelDir+"IMine.Qrel.DRC/IMine.Qrel.DRC.xml";
		}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){
			drFile = qrelDir+"IMine.Qrel.DRE/IMine.Qrel.DRE.xml";
		}else{
			System.err.println("Type Error!");
			System.exit(0);
		}
		
		try {			
			SAXBuilder saxBuilder = new SAXBuilder();
		    Document xmlDoc = saxBuilder.build(new File(drFile)); 
		    //new InputStreamReader(new FileInputStream(targetFile), DEFAULT_ENCODING)
		    
		    Element rootElement = xmlDoc.getRootElement();
		    
		    List<Element> relevanceElementList = rootElement.getChildren("relevance");
		    
		    if(type == NTCIR11_TOPIC_TYPE.CLEAR){
		    	
		    	Element firstPart = relevanceElementList.get(0);
		    	
		    	List<Element> docElementList = firstPart.getChildren("doc");
		    	ArrayList<Triple<String, String, Integer>> triList = new ArrayList<Triple<String,String,Integer>>();
		    	
		    	for(Element docElement: docElementList){
		    		String docid = docElement.getAttributeValue("docid");
		    		String queryid = docElement.getAttributeValue("queryid");
		    		int releLevel = Integer.parseInt(docElement.getAttributeValue("relevance"));
		    		releLevel = releLevel-1;
		    		
		    		if(releLevel>0){
		    			triList.add(new Triple<String, String, Integer>(docid, queryid, releLevel));
		    		}
		    	}
		        
		    	return triList;  	
		    	
		    }else if(type == NTCIR11_TOPIC_TYPE.UNCLEAR){
		    	
		    	Element secondPart = relevanceElementList.get(1);
		    	
		    	List<Element> docElementList = secondPart.getChildren("doc");
		    	ArrayList<Triple<String, String, Integer>> triList = new ArrayList<Triple<String,String,Integer>>();
		    	
		    	for(Element docElement: docElementList){
		    		String docid = docElement.getAttributeValue("docid");
		    		String queryid = docElement.getAttributeValue("queryid"); 
		    		int releLevel = Integer.parseInt(docElement.getAttributeValue("relevance"));
		    		releLevel = releLevel-1;	
		    		
		    		String slsStr = docElement.getAttributeValue("sls");
		    		
		    		if(releLevel > 0){
		    			triList.add(new Triple<String, String, Integer>(docid, queryid+"\t"+slsStr, releLevel));
		    		}		    		
		    	}
		    	
		    	return triList;
		    	
		    }else{
		    	System.err.println("Type Error!");
		    	System.exit(0);
		    }		    
		}catch(Exception e){
			e.printStackTrace();			
		}
		
		return null;
	}
	//get the relevance assessments of a document w.r.t. second-level subtopics
	private static void getDocUsageMap(String xmlDir, NTCIR_EVAL_TASK eval, NTCIR11_TOPIC_TYPE type){
		//1 ini
		fls_DocReleMap = new HashMap<String, HashMap<String, HashMap<Integer, Integer>>>();
		sls_DocReleMap = new HashMap<String, HashMap<String, HashMap<Integer, Integer>>>();
		
		ArrayList<Triple<String, String, Integer>> triList = null;
		
		if(type == NTCIR11_TOPIC_TYPE.CLEAR){
			System.err.println("Input type error!");
		}else{
			triList = getXmlQrel(xmlDir, eval, type);
		}
		
		//2 parse
		for(Triple<String, String, Integer> triple: triList){
			String docid = triple.getFirst();
			Integer releLevel = triple.getThird();
			
			String [] array = triple.getSecond().split("\t");
			String queryid = array[0];
			String slsStr = array[1];
			
			//mapping from sls's content to fls's id
			Pair<Integer, Integer> e = _2LTMap.get(queryid)._slsContentMap.get(slsStr);
						
			//fls
			Integer flsID = e.getFirst();
			if(fls_DocReleMap.containsKey(docid)){
				HashMap<String, HashMap<Integer, Integer>> docUsage = fls_DocReleMap.get(docid);
				
				if(docUsage.containsKey(queryid)){					
					docUsage.get(queryid).put(flsID, releLevel);
				}else{
					HashMap<Integer, Integer> flsReleMap = new HashMap<Integer, Integer>();
					flsReleMap.put(flsID, releLevel);
					
					docUsage.put(queryid, flsReleMap);
				}				
			}else{
				HashMap<String, HashMap<Integer, Integer>> docUsage = new HashMap<String, HashMap<Integer, Integer>>();
				HashMap<Integer, Integer> flsReleMap = new HashMap<Integer, Integer>();
				flsReleMap.put(flsID, releLevel);				
				docUsage.put(queryid, flsReleMap);
				
				fls_DocReleMap.put(docid, docUsage);
			}
			
			//sls
			Integer slsID = e.getSecond();
			if(sls_DocReleMap.containsKey(docid)){
				HashMap<String, HashMap<Integer, Integer>> docUsage = sls_DocReleMap.get(docid);
				
				if(docUsage.containsKey(queryid)){
					docUsage.get(queryid).put(slsID, releLevel);
				}else{
					HashMap<Integer, Integer> slsReleMap = new HashMap<Integer, Integer>();
					slsReleMap.put(slsID, releLevel);
					
					docUsage.put(queryid, slsReleMap);
				}
			}else{
				HashMap<String, HashMap<Integer, Integer>> docUsage = new HashMap<String, HashMap<Integer,Integer>>();
				HashMap<Integer, Integer> slsReleMap = new HashMap<Integer, Integer>();
				slsReleMap.put(slsID, releLevel);
				docUsage.put(queryid, slsReleMap);
				
				sls_DocReleMap.put(docid, docUsage);
			}
		}		
	}
	
	//per file, queryid -> ranked results
	private static HashMap<String, ArrayList<String>> loadSysRun(String sysFile){
		HashMap<String, ArrayList<String>> sysRun = new HashMap<String, ArrayList<String>>();
		
		try {
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(sysFile);
			
			String []array;
			for(int i=0; i<lineList.size(); i++){
				if(lineList.get(i).indexOf("SYSDESC") >= 0){
					continue;
				}
				//array[0]:topic-id / array[1]:0 / array[2]:doc-name / array[3]:rank / array[4]:score / array[5]:run-name
				array = lineList.get(i).split("\\s");
				String topicid = array[0];
				String item = array[2];
				
				if(sysRun.containsKey(topicid)){
					sysRun.get(topicid).add(item);
				}else{
					ArrayList<String> itemList = new ArrayList<String>();
					itemList.add(item);
					sysRun.put(topicid, itemList);
				}
			}			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		//test
		/*
		for(Entry<String, ArrayList<String>> runPerTopic: sysRun.entrySet()){
			System.out.println(runPerTopic.getKey()+"\t"+runPerTopic.getValue().size());
		}
		*/
		
		return sysRun;
	}
	//runid -> runMap
	public static HashMap<String, HashMap<String, ArrayList<String>>> loadSysRuns(String dir){
		
		HashMap<String, HashMap<String, ArrayList<String>>> allRuns = 
				new HashMap<String, HashMap<String,ArrayList<String>>>();
		
		try {
			File fileDir = new File(dir);
			File [] sysRuns = fileDir.listFiles();
			
			for(File oneSysRun: sysRuns){
				String fileName = oneSysRun.getName();
				String runID = fileName.substring(0, fileName.indexOf("."));
				HashMap<String, ArrayList<String>> runMap = loadSysRun(oneSysRun.getAbsolutePath());
				
				allRuns.put(runID, runMap);
			}			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		//test
		/*
		for(Entry<String, HashMap<String, ArrayList<String>>> oneSubmittedRun: allRuns.entrySet()){
			System.out.println(oneSubmittedRun.getKey()+"\t"+oneSubmittedRun.getValue().size());
		}
		*/
		/*
		HashMap<String, ArrayList<String>> run1 = allRuns.get("FRDC-D-C-2A");
		System.out.println(run1.get("0001").get(0));
		System.out.println(run1.get("0050").get(12));
		System.out.println();
		
		HashMap<String, ArrayList<String>> run2 = allRuns.get("THUSAM-D-C-1A");
		System.out.println(run2.get("0001").get(0));
		System.out.println(run2.get("0050").get(199));
		*/
		
		
		return allRuns;
	}
	////////////
	//Metrics
	////////////
	
	/////	Subtopic-recall	////
	private static HashSet<Integer> SRecall_i(String queryid, STLevel stLevel, int sID, 
			ArrayList<String> sysList, int cutoff, TwoLevelTopic twoLevelTopic){
		
		if(null==sysList || sysList.size() == 0){
			return null;
		}
		
		//without enough documents provided
		int onCutoff = Math.min(sysList.size(), cutoff);
		
		HashSet<Integer> releDocSet_i = new HashSet<Integer>();		
		
		for(int k=1; k<=onCutoff; k++){
			String docid = sysList.get(k-1);
			
			if(STLevel.FLS == stLevel){
				if(fls_DocReleMap.containsKey(docid)){
					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){
						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(sID)){
							
							releDocSet_i.add(sID);
						}										
					}					
				}
			}else{				
				System.err.println("Non-accepted STLevel!");
				System.exit(0);
				return null;		
			}
		}
		
		return releDocSet_i;
	}
	
	private static double SRecall(String queryid, STLevel stLevel, 
			ArrayList<String> sysList, int cutoff){	
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		//fls
		if(STLevel.FLS == stLevel){
			int flsCount = topic._flsList.size();			
			
			HashSet<Integer> releFlsSet = new HashSet<Integer>();
			
			for(int i=1; i<=flsCount; i++){
				HashSet<Integer> releFlsSet_i = SRecall_i(queryid,stLevel, i, sysList,cutoff, topic);
				if(null != releFlsSet_i){
					releFlsSet.addAll(releFlsSet_i);
				}
			}
			
			double sRecall = (1.0*releFlsSet.size())/flsCount;
			
			return sRecall;
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return Double.NaN;
		}
	}
	
	////	AP-IA	////
	private static double AP_i(String queryid, STLevel stLevel, int sID, 
			ArrayList<String> sysList, int cutoff, TwoLevelTopic twoLevelTopic){
		
		if(null==sysList || sysList.size() == 0){
			return 0.0;
		}
		
		//without enough documents provided
		int onCutoff = Math.min(sysList.size(), cutoff);
				
		double  ap_i = 0.0;
		double  relesCountByNow = 0;
		
		for(int k=1; k<=onCutoff; k++){
			String docid = sysList.get(k-1);
			
			if(STLevel.FLS == stLevel){
				
				if(fls_DocReleMap.containsKey(docid)){
					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){
						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(sID)){
							
							double satPro = I(qReleMap.get(sID));
							
							relesCountByNow += I(qReleMap.get(sID));
							
							ap_i += satPro*(1.0/k)*relesCountByNow;
						}										
					}					
				}				
			}else{				
				System.err.println("Non-accepted STLevel!");
				System.exit(0);
				return Double.NaN;		
			}
		}
		
		ap_i = ap_i/cutoff;
		
		return ap_i;		
	}
	
	private static double APIA(boolean equal, String queryid, STLevel stLevel, 
			ArrayList<String> sysList, int cutoff){
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		//fls
		if(STLevel.FLS == stLevel){	
			int flsCount = topic._flsList.size();
			
			double apIA = 0.0;
			for(int i=1; i<=flsCount; i++){
				apIA += (topic.getFlsPro(i, equal)*AP_i(queryid, stLevel, i, sysList, cutoff, topic));
			}
			
			return apIA;
			
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return Double.NaN;
		}
		
	}
	
	////	ERR-IA	////
	/**
	 * ERR for a given first-level subtopic i
	 * 
	 * @param equal true means that subtopics are equally distributed, otherwise biased, i.e, the official probability
	 * @param flsID	first-level subtopic id
	 * @param sysList system ranking results for a specific query
	 * **/
	private static double ERRIA_i(String queryid, STLevel stLevel, int sID, 
			ArrayList<String> sysList, int cutoff, TwoLevelTopic twoLevelTopic){
		
		if(null==sysList || sysList.size() == 0){
			return 0.0;
		}
		
		double err_i = 0.0;
		double disPro = 1.0;		
		
		//without enough documents provided
		int onCutoff = Math.min(sysList.size(), cutoff);
		
		for(int k=1; k<=onCutoff; k++){
			String docid = sysList.get(k-1);
			
			if(STLevel.FLS == stLevel){
				if(fls_DocReleMap.containsKey(docid)){
					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){
						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(sID)){
							
							double satPro = R(qReleMap.get(sID), 2);
																	
							double tem = (1.0/k)*satPro*disPro;
							
							err_i += tem;
							
							disPro *= (1-satPro);	
						}										
					}					
				}
			}else{
				if(sls_DocReleMap.containsKey(docid)){
					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = sls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){
						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(sID)){
							double satPro = R(qReleMap.get(sID), 2);
							
							double tem = (1.0/k)*satPro*disPro;
							
							err_i += tem;
							
							disPro *= (1-satPro);
						}											
					}					
				}
			}			
		}
		
		if(Double.isNaN(err_i)){
			System.out.println(queryid);
		}
		
		return err_i;		
	}
	/**
	 * ERR-IA
	 * **/
	private static double ERRIA(boolean equal, String queryid, STLevel stLevel, 
			ArrayList<String> sysList, int cutoff){
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		//fls
		if(STLevel.FLS == stLevel){			
			
			int flsCount = topic._flsList.size();
			
			double errIA = 0.0;
			for(int i=1; i<=flsCount; i++){
				errIA += (topic.getFlsPro(i, equal)*ERRIA_i(queryid, stLevel, i, sysList, cutoff, topic));
			}
			
			return errIA;
			
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return Double.NaN;
		}		
	}
		
	////	alpha-nDCg	////
	/**
	 * alpha-nDCG_i
	 * **/
	private static double alpha_DCG_i(String queryid, STLevel stLevel, int sID, 
			ArrayList<String> sysList, int cutoff, TwoLevelTopic twoLevelTopic){
		
		if(null==sysList || sysList.size() == 0){
			return 0.0;
		}
		
		//without enough documents provided
		int onCutoff = Math.min(sysList.size(), cutoff);
		
		double  alpha_DCG_i = 0.0;
		double  countPriorReles = 0;
		
		for(int k=1; k<=onCutoff; k++){
			String docid = sysList.get(k-1);
			
			if(STLevel.FLS == stLevel){
				
				if(fls_DocReleMap.containsKey(docid)){
					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){
						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(sID)){
							
							double satPro = I(qReleMap.get(sID));
							
							alpha_DCG_i += (1.0/(Math.log10(k+1)/Math.log10(2)))*satPro*Math.pow((1-ALPHA), countPriorReles);
							
							countPriorReles += I(qReleMap.get(sID));
						}										
					}					
				}
				
			}else{				
				System.err.println("Non-accepted STLevel!");
				System.exit(0);
				return Double.NaN;		
			}
		}
		
		if(Double.isNaN(alpha_DCG_i)){
			System.out.println(queryid);
		}
		
		return alpha_DCG_i;		
	}
	/**
	 * alpha-nDCG
	 * **/
	private static double alpha_DCG(String queryid, STLevel stLevel, 
			ArrayList<String> sysList, int cutoff){
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		//fls
		if(STLevel.FLS == stLevel){
			int flsCount = topic._flsList.size();
			
			double alpha_DCG = 0.0;
			for(int i=1; i<=flsCount; i++){
				alpha_DCG += alpha_DCG_i(queryid,stLevel, i, sysList,cutoff, topic);
			}
			
			return alpha_DCG;
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return Double.NaN;
		}
	}
	//
	private static double alpha_n_DCG(String queryid, STLevel stLevel, 
			ArrayList<String> sysList, int cutoff){
		
		return alpha_DCG(queryid, stLevel, sysList, cutoff)/alpha_DCG(queryid, stLevel, 
				idealList_alpha_DCG(queryid, stLevel, cutoff), cutoff);
		
	}
	
	/**
	 * find the ideal list by greedy search
	 * **/
	private static ArrayList<String> idealList_alpha_DCG(String queryid, STLevel stLevel, int cutoff){
		//list of relevant doc
		ArrayList<String> releDocList = new ArrayList<String>();
		
		ArrayList<String> idealList = new ArrayList<String>();
		
		HashSet<String> selectedDocSet = new HashSet<String>();
		
		if(STLevel.FLS == stLevel){
			//list preparation
			for(Entry<String, HashMap<String, HashMap<Integer, Integer>>> flsEntry: fls_DocReleMap.entrySet()){				
				if(flsEntry.getValue().containsKey(queryid)){
					releDocList.add(flsEntry.getKey());
				}				
			}
			
			TwoLevelTopic topic = _2LTMap.get(queryid);
			int flsCount = topic._flsList.size();
			//size as the count of fls, flsID corresponds to the (flsID-1)-th element
			ArrayList<Integer> priorReleCountList = new ArrayList<Integer>();
			for(int i=1; i<=flsCount; i++){
				priorReleCountList.add(0);
			}
			//buffer the releList of each doc
			HashMap<String, ArrayList<Integer>> releListMap = new HashMap<String, ArrayList<Integer>>();
			//
			int availableCut = Math.min(cutoff, releDocList.size());
			//
			while(idealList.size() < availableCut){
				ArrayList<Pair<String, Double>> desDocListByGain = new ArrayList<Pair<String,Double>>();
				
				
				for(String candidateDoc: releDocList){
					if(!selectedDocSet.contains(candidateDoc)){
						double gain = 0.0;
						
						if(releListMap.containsKey(candidateDoc)){
							ArrayList<Integer> releList = releListMap.get(candidateDoc);
							
							for(int i=1; i<=flsCount; i++){
								gain += releList.get(i-1)*(Math.pow((1-ALPHA), priorReleCountList.get(i-1)));							
							}
							
						}else{
							
							ArrayList<Integer> releList = new ArrayList<Integer>();
							for(int i=1; i<=flsCount; i++){
								int binRele = 0;
								if(fls_DocReleMap.get(candidateDoc).get(queryid).containsKey(i)){
									binRele = fls_DocReleMap.get(candidateDoc).get(queryid).get(i);
								}
								releList.add(binRele);
								//
								gain += binRele*(Math.pow((1-ALPHA), priorReleCountList.get(i-1)));							
							}
							releListMap.put(candidateDoc, releList);
						}						
						
						desDocListByGain.add(new Pair<String, Double>(candidateDoc, gain));												
					}
				}
				
				Collections.sort(desDocListByGain, new PairComparatorBySecond_Desc<String, Double>());
				
				//the ideal one
				idealList.add(desDocListByGain.get(0).getFirst());
				selectedDocSet.add(desDocListByGain.get(0).getFirst());
				//fls-specific rele count
				ArrayList<Integer> addedReleList = releListMap.get(desDocListByGain.get(0).getFirst());
				for(int i=1; i<=flsCount; i++){
					priorReleCountList.set(i-1, addedReleList.get(i-1)+priorReleCountList.get(i-1));
				}				
			}
			
			return idealList;
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return null;
		}		
	}
	
	////	D#-nDCG	////
	private static double D_Sharp_n_DCG(boolean equal, String queryid, STLevel stLevel, 
			ArrayList<String> sysList, int cutoff){
		
		double sRecall = SRecall(queryid, stLevel, sysList, cutoff);
		
		ArrayList<String> idealList = idealList_D_Sharp_DCG(equal, queryid, stLevel, cutoff);
		
		double d_Sharp_n_DCG = D_Sharp_DCG(equal, queryid, stLevel, sysList, cutoff)/D_Sharp_DCG(
				equal, queryid, stLevel, idealList, cutoff);
		
		return GAMA*sRecall + (1-GAMA)*d_Sharp_n_DCG;		
	}	
	
	private static double D_Sharp_DCG(boolean equal, String queryid, STLevel stLevel, 
			ArrayList<String> sysList, int cutoff){
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		//fls
		if(STLevel.FLS == stLevel){			
			
			int flsCount = topic._flsList.size();
			
			double d_Sharp_DCG = 0.0;
			for(int i=1; i<=flsCount; i++){
				d_Sharp_DCG += (topic.getFlsPro(i, equal)*D_Sharp_DCG_i(queryid, stLevel, i, sysList, cutoff, topic));
			}
			
			return d_Sharp_DCG;
			
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return Double.NaN;
		}
	}
	
	private static double D_Sharp_DCG_i(String queryid, STLevel stLevel, int sID, 
			ArrayList<String> sysList, int cutoff, TwoLevelTopic twoLevelTopic){
		
		if(null==sysList || sysList.size() == 0){
			return 0.0;
		}
		
		//without enough documents provided
		int onCutoff = Math.min(sysList.size(), cutoff);
		
		double  d_Sharp_DCG_i = 0.0;
		
		for(int k=1; k<=onCutoff; k++){
			String docid = sysList.get(k-1);
			
			if(STLevel.FLS == stLevel){
				
				if(fls_DocReleMap.containsKey(docid)){
					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){
						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(sID)){
							
							double gain = V(qReleMap.get(sID));
							
							d_Sharp_DCG_i += (1.0/(Math.log10(k+1)/Math.log10(2)))*gain;							
						}										
					}					
				}
				
			}else{				
				System.err.println("Non-accepted STLevel!");
				System.exit(0);
				return Double.NaN;		
			}
		}
		
		return d_Sharp_DCG_i;		
	}
	
	/** 
	 * find the ideal list by greedy search
	 * **/
	private static ArrayList<String> idealList_D_Sharp_DCG(boolean equal, String queryid, STLevel stLevel, int cutoff){
		
		//list of relevant doc
		ArrayList<String> releDocList = new ArrayList<String>();
		
		ArrayList<String> idealList = new ArrayList<String>();
				
		if(STLevel.FLS == stLevel){
			
			TwoLevelTopic topic = _2LTMap.get(queryid);
			int flsCount = topic._flsList.size();
			
			//list preparation
			for(Entry<String, HashMap<String, HashMap<Integer, Integer>>> flsEntry: fls_DocReleMap.entrySet()){				
				if(flsEntry.getValue().containsKey(queryid)){
					releDocList.add(flsEntry.getKey());
				}				
			}
			
			ArrayList<Pair<String, Double>> desDocListByGG = new ArrayList<Pair<String,Double>>();
			
			for(String releDoc: releDocList){
				double globalGain = 0.0;
				
				for(int i=1; i<=flsCount; i++){					
					if(fls_DocReleMap.get(releDoc).get(queryid).containsKey(i)){
						globalGain += (topic.getFlsPro(i, equal))*V(fls_DocReleMap.get(releDoc).get(queryid).get(i));
					}					
				}
				
				desDocListByGG.add(new Pair<String, Double>(releDoc, globalGain));
			}
			
			Collections.sort(desDocListByGG, new PairComparatorBySecond_Desc<String, Double>());
			
			int availableCutoff = Math.min(cutoff, desDocListByGG.size());
			
			for(int k=1; k<=availableCutoff; k++){
				idealList.add(desDocListByGG.get(k-1).getFirst());
			}
			
			return idealList;
			
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return null;
		}	
	}
	
	//test
	public static void test(boolean equal){
		String queryid = "0001";
		//topic
		TwoLevelTopic aTwoLevelTopic = new TwoLevelTopic(queryid, "test");
		
		ArrayList<Pair<String, Double>> flsList = new ArrayList<Pair<String,Double>>();
		if(equal){
			flsList.add(new Pair<String, Double>("i", 0.5));
			flsList.add(new Pair<String, Double>("j", 0.5));
		}else{
			flsList.add(new Pair<String, Double>("i", 0.7));
			flsList.add(new Pair<String, Double>("j", 0.3));
		}
		
		aTwoLevelTopic.setFlsList(flsList);
		
		_2LTMap.put("0001", aTwoLevelTopic);
		
		//doc
		fls_DocReleMap = new HashMap<String, HashMap<String,HashMap<Integer,Integer>>>();
		
		//doc_1		
		String doc_1 = "doc_1";
		HashMap<Integer,Integer> qMap_1 = new HashMap<Integer, Integer>();
		qMap_1.put(1, 1);
		
		HashMap<String,HashMap<Integer,Integer>> docMap_1 = new HashMap<String, HashMap<Integer,Integer>>();
		docMap_1.put(queryid, qMap_1);
		
		fls_DocReleMap.put(doc_1, docMap_1);
		
		//doc_2		
		String doc_2 = "doc_2";
		HashMap<Integer,Integer> qMap_2 = new HashMap<Integer, Integer>();
		qMap_2.put(1, 1);
		
		HashMap<String,HashMap<Integer,Integer>> docMap_2 = new HashMap<String, HashMap<Integer,Integer>>();
		docMap_2.put(queryid, qMap_2);
		
		fls_DocReleMap.put(doc_2, docMap_2);
		
		
		//doc_3
		String doc_3 = "doc_3";
		HashMap<Integer,Integer> qMap_3 = new HashMap<Integer, Integer>();
		qMap_3.put(2, 1);
		
		HashMap<String,HashMap<Integer,Integer>> docMap_3 = new HashMap<String, HashMap<Integer,Integer>>();
		docMap_3.put(queryid, qMap_3);
		
		fls_DocReleMap.put(doc_3, docMap_3);
		
		ArrayList<String> rankedList = new ArrayList<String>();
		rankedList.add(doc_1);
		rankedList.add(doc_2);
		rankedList.add(doc_3);
		
		//
		int cutoff = 3;
		System.out.println("SRecall:\t"+SRecall(queryid, STLevel.FLS, rankedList, cutoff));
		System.out.println("APIA:\t"+APIA(equal, queryid, STLevel.FLS, rankedList, cutoff));
		System.out.println("ERRIA:\t"+ERRIA(equal, queryid, STLevel.FLS, rankedList, cutoff));
		System.out.println("alpha_n_DCG:\t"+alpha_n_DCG(queryid, STLevel.FLS, rankedList, cutoff));
		System.out.println("D#-nDCG:\t"+D_Sharp_n_DCG(equal, queryid, STLevel.FLS, rankedList, cutoff));
		//System.out.println(":\t"+);	
		
		
	}
	
	////////
	//Comparison
	////////
	
	private static final String SRecall = "SRecall";
	private static final String AP_IA = "AP_IA";
	private static final String ERR_IA = "ERR_IA";
	private static final String Alpha_n_DCG = "Alpha_n_DCG";
	private static final String D_Sharp_n_DCG = "D_Sharp_n_DCG";
	private static final String DIN_Sharp_n_DCGl = "DIN_Sharp_n_DCGl";
	
	public static void metricValue(NTCIR_EVAL_TASK eval, NTCIR11_TOPIC_TYPE type, boolean equal, STLevel stLevel, int cutoff){
		
		String xmlDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/20150428/";
		String runDir = null;
		
		//filtered due to no sls, i.e., 0003(4), 0017(2) for Ch
		HashSet<String> filteredTopicSet = new HashSet<String>();
		
		if(type == NTCIR11_TOPIC_TYPE.UNCLEAR){		
			//
			if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
				
				String chLevelFile = xmlDir + "IMine.Qrel.SMC/IMine.Qrel.SMC.xml";
				load2LT(chLevelFile);					
				
				runDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/IMine.submitted/DR-Run/C/";
				
				filteredTopicSet.add("0003");
				filteredTopicSet.add("0017");
				
			}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){
				
				String enLevelFile = xmlDir + "IMine.Qrel.SME/IMine.Qrel.SME.xml";
				load2LT(enLevelFile);
				
				runDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/IMine.submitted/DR-Run/E/";
				
				filteredTopicSet.add("0070");
			}
			
			//runs to be evaluated
			HashMap<String, HashMap<String, ArrayList<String>>> allRuns = loadSysRuns(runDir);
			
			//document relevance
			getDocUsageMap(xmlDir, eval, type);
			
			//metricStr -> averageMetricValue
			HashMap<String, Double> SRecall_AvgMap = new HashMap<String, Double>();
			//metricStr -> list of per query result
			HashMap<String, ArrayList<Double>> SRecall_PerQueryEvalMap = new HashMap<String, ArrayList<Double>>();
			
			HashMap<String, Double> APIA_AvgMap = new HashMap<String, Double>();
			HashMap<String, ArrayList<Double>>  APIA_PerQueryEvalMap = new HashMap<String, ArrayList<Double>>();
			
			HashMap<String, Double> ERRIA_AvgMap = new HashMap<String, Double>();
			HashMap<String, ArrayList<Double>> ERRIA_PerQueryEvalMap = new HashMap<String, ArrayList<Double>>();
			
			HashMap<String, Double> AlphaNDCG_AvgMap = new HashMap<String, Double>();
			HashMap<String, ArrayList<Double>> AlphaNDCG_PerQueryEvalMap = new HashMap<String, ArrayList<Double>>();
			
			HashMap<String, Double> DSharpNDCG_AvgMap = new HashMap<String, Double>();
			HashMap<String, ArrayList<Double>> DSharpNDCG_PerQueryEvalMap = new HashMap<String, ArrayList<Double>>();

			
			for(Entry<String, HashMap<String, ArrayList<String>>> run: allRuns.entrySet()){
				String runID = run.getKey();
				//queryid -> ranked list
				HashMap<String, ArrayList<String>> rankedListMap = run.getValue();
				
				ArrayList<Double> SRecall_PerQueryEvalList = new ArrayList<Double>();	
				ArrayList<Double> APIA_PerQueryEvalList = new ArrayList<Double>();
				ArrayList<Double> ERRIA_PerQueryEvalList = new ArrayList<Double>();
				ArrayList<Double> AlphaNDCG_PerQueryEvalList = new ArrayList<Double>();
				ArrayList<Double> DSharpNDCG_PerQueryEvalList = new ArrayList<Double>();
				
				double SRecall_sum = 0.0;
				double APIA_sum = 0.0;
				double ERRIA_sum = 0.0;
				double AlphaNDCG_sum = 0.0;
				double DSharpNDCG_sum = 0.0;
				
				int countOfUsedTopics = 0;
				
				for(TwoLevelTopic topic: _2LTList){
					String queryid = topic.getTopicID();
					
					//filter the topics if it includes a subtopic that has no second-level subtopic
					if(filteredTopicSet.contains(queryid)){
						continue;
					}
					
					ArrayList<String> rankedList = rankedListMap.get(queryid);
					
					countOfUsedTopics++;
					
					//SRecall
					double sRecall = SRecall(queryid, stLevel, rankedList, cutoff);
					SRecall_PerQueryEvalList.add(sRecall);
					SRecall_sum += sRecall;
					
					//APIA
					double apIA = APIA(equal, queryid, stLevel, rankedList, cutoff);
					APIA_PerQueryEvalList.add(apIA);
					APIA_sum += apIA;
					
					//ERRIA
					double errIA = ERRIA(equal, queryid, stLevel, rankedList, cutoff);
					ERRIA_PerQueryEvalList.add(errIA);
					ERRIA_sum += errIA;
					
					//alpha-nDCG
					double alphaNDCG = alpha_n_DCG(queryid, stLevel, rankedList, cutoff);
					AlphaNDCG_PerQueryEvalList.add(alphaNDCG);
					AlphaNDCG_sum += alphaNDCG;
					
					//D#-nDCG
					double dSharpNDCG = D_Sharp_n_DCG(equal, queryid, stLevel, rankedList, cutoff);
					DSharpNDCG_PerQueryEvalList.add(dSharpNDCG);
					DSharpNDCG_sum += dSharpNDCG;
					
				}
				
				SRecall_AvgMap.put(SRecall, SRecall_sum/countOfUsedTopics);
				SRecall_PerQueryEvalMap.put(SRecall, SRecall_PerQueryEvalList);
				
				APIA_AvgMap.put(AP_IA, APIA_sum/countOfUsedTopics);
				APIA_PerQueryEvalMap.put(AP_IA, APIA_PerQueryEvalList);
				
				ERRIA_AvgMap.put(ERR_IA, ERRIA_sum/countOfUsedTopics);
				ERRIA_PerQueryEvalMap.put(ERR_IA, ERRIA_PerQueryEvalList);
				
				AlphaNDCG_AvgMap.put(Alpha_n_DCG, AlphaNDCG_sum/countOfUsedTopics);
				AlphaNDCG_PerQueryEvalMap.put(Alpha_n_DCG, AlphaNDCG_PerQueryEvalList);
				
				DSharpNDCG_AvgMap.put(D_Sharp_n_DCG, DSharpNDCG_sum/countOfUsedTopics);
				DSharpNDCG_PerQueryEvalMap.put(D_Sharp_n_DCG, DSharpNDCG_PerQueryEvalList);				
			}
			
			
		}else {
			System.err.println("Unsupported NTCIR11_TOPIC_TYPE error!");
		}
	}
	
	
	
	///////////////////////////////
	public static void main(String []args){
		//1
		//MetricComparison.test(false);
		
		//2
		boolean equal = true;
		int cutoff = 20;
		MetricComparison.metricValue(NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, equal, STLevel.FLS, cutoff);
	}
	
}
