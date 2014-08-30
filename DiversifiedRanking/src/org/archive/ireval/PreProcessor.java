package org.archive.ireval;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR11_TOPIC_LEVEL;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR11_TOPIC_TYPE;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.util.io.IOText;
import org.archive.util.tuple.Pair;
import org.archive.util.tuple.Triple;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.sun.xml.internal.ws.addressing.ProblemAction;

public class PreProcessor {
	
	public static final boolean debug = true;
	
	//
	public static ArrayList<TwoLevelTopic> _2LTList = new ArrayList<TwoLevelTopic>();
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
	
	/**
	 * Extract qrel xml file
	 * **/
	public static ArrayList<Triple<String, String, Integer>> getXmlQrel(NTCIRLoader.NTCIR_EVAL_TASK eval, NTCIRLoader.NTCIR11_TOPIC_TYPE type){
		String dir = "H:/v-haiyu/TaskPreparation/Ntcir11-IMine/Eval-IMine/20140830/";
		
		String drFile = null;
		
		if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
			drFile = dir+"IMine.Qrel.DRC/IMine.Qrel.DRC.xml";
		}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){
			drFile = dir+"IMine.Qrel.DRE/IMine.Qrel.DRE.xml";
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
	
	/**
	 * generate standard qrel file
	 * **/
	public static void generateQrelFile(String dir, NTCIR_EVAL_TASK eval, NTCIR11_TOPIC_TYPE type, NTCIR11_TOPIC_LEVEL stLevel){
		
		ArrayList<Triple<String, String, Integer>> triList = getXmlQrel(eval, type);
		
		HashSet<String> topicSet = new HashSet<String>();
		
		try {						
			if(type == NTCIR11_TOPIC_TYPE.CLEAR){
				
				String file = null;
				
				if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
					file = dir+"IMine-DR-Qrel-C-Clear";
				}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){
					file = dir+"IMine-DR-Qrel-E-Clear";
				}
				//used topic
				ArrayList<String> topicList = NTCIRLoader.loadNTCIR11Topic(eval, type);
				topicSet.addAll(topicList);
				
				BufferedWriter writer = IOText.getBufferedWriter_UTF8(file);
				
				for(Triple<String, String, Integer> triple: triList){
					if(topicSet.contains(triple.getSecond())){
						writer.write(triple.getSecond()+" "+triple.getFirst()+" "+"L"+triple.getThird());
						writer.newLine();
					}					
				}				
				writer.flush();
				writer.close();
				
			}else if(type == NTCIR11_TOPIC_TYPE.UNCLEAR){
				
				String dFile = null;
				String iFile = null;
				
				if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){					
					dFile = dir+"IMine-DR-C-Unclear-Dqrels-";
					iFile = dir+"IMine-DR-C-Unclear-Iprob-";
					
					String chLevelFile = "H:/v-haiyu/TaskPreparation/Ntcir11-IMine/Eval-IMine/20140830/IMine.Qrel.SMC/IMine.Qrel.SMC.xml";
					load2LT(chLevelFile);					
				}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){					
					dFile = dir+"IMine-DR-E-Unclear-Dqrels-";
					iFile = dir+"IMine-DR-E-Unclear-Iprob-";
					
					String enLevelFile = "H:/v-haiyu/TaskPreparation/Ntcir11-IMine/Eval-IMine/20140830/IMine.Qrel.SME/IMine.Qrel.SME.xml";
					load2LT(enLevelFile);
				}
				
				ArrayList<String> topicList = NTCIRLoader.loadNTCIR11Topic(eval, type);
				topicSet.addAll(topicList);
				
				if(stLevel == NTCIR11_TOPIC_LEVEL.FLS){
					
					dFile += NTCIR11_TOPIC_LEVEL.FLS.toString();
					iFile += NTCIR11_TOPIC_LEVEL.FLS.toString();
					
					BufferedWriter dWriter = IOText.getBufferedWriter_UTF8(dFile);					
					for(Triple<String, String, Integer> triple: triList){
						String [] array = triple.getSecond().split("\t");
						String queryid = array[0];
						String slsStr = array[1];
						
						if(topicSet.contains(queryid)){							
							Pair<Integer, Integer> e = _2LTMap.get(queryid)._slsContentMap.get(slsStr);
							if(null == e){
								System.err.println(queryid+"\t"+slsStr);
								continue;
							}
							dWriter.write(queryid+" "+e.getFirst()+" "+triple.getFirst()+" "+"L"+triple.getThird());
							dWriter.newLine();							
						}					
					}					
					dWriter.flush();
					dWriter.close();
					
					BufferedWriter iWriter = IOText.getBufferedWriter_UTF8(iFile);					
					for(String id: topicList){
						TwoLevelTopic t = _2LTMap.get(id);
						int flsID = 1;
						for(; flsID<=t._flsList.size(); flsID++){
							Pair<String, Double> fls = t._flsList.get(flsID-1);
							iWriter.write(id+" "+flsID+" "+fls.getSecond());
							iWriter.newLine();
						}
					}
					iWriter.flush();
					iWriter.close();
					
				}else{
					
					dFile += NTCIR11_TOPIC_LEVEL.SLS.toString();
					iFile += NTCIR11_TOPIC_LEVEL.SLS.toString();
					
					BufferedWriter dWriter = IOText.getBufferedWriter_UTF8(dFile);					
					for(Triple<String, String, Integer> triple: triList){
						String [] array = triple.getSecond().split("\t");
						String queryid = array[0];
						String slsStr = array[1];
						
						if(topicSet.contains(queryid)){							
							Pair<Integer, Integer> e = _2LTMap.get(queryid)._slsContentMap.get(slsStr);
							if(null == e){
								System.err.println(queryid+"\t"+slsStr);
								continue;
							}
							dWriter.write(queryid+" "+e.getSecond()+" "+triple.getFirst()+" "+"L"+triple.getThird());
							dWriter.newLine();							
						}					
					}					
					dWriter.flush();
					dWriter.close();
					
					BufferedWriter iWriter = IOText.getBufferedWriter_UTF8(iFile);					
					for(String id: topicList){
						TwoLevelTopic t = _2LTMap.get(id);
						
						int slsID = 1;						 
						for(ArrayList<Pair<String, Double>> slsSet: t._slsSetList){
							for(Pair<String, Double> sls: slsSet){
								iWriter.write(id+" "+(slsID++)+" "+sls.getSecond());
								iWriter.newLine();
							}							
						}
					}
					iWriter.flush();
					iWriter.close();
				}				
			} else{
				System.err.println("Type Error!");
				System.exit(0);
			}			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	
	//
	public static void main(String []args){
		//1
		/*
		String file = "H:/v-haiyu/TaskPreparation/Ntcir11-IMine/Eval-IMine/IMine.Qrel.SMC/IMine.Qrel.SMC.xml";
		PreProcessor.load2LT(file);
		*/
		
		//2
		String dir = "H:/v-haiyu/TaskPreparation/Ntcir11-IMine/Eval-IMine/20140830/CheckEval/";
		//ch, clear
		//PreProcessor.generateQrelFile(dir, NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.CLEAR, null);
		
		//ch, unclear, fls
		//PreProcessor.generateQrelFile(dir, NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.UNCLEAR, NTCIR11_TOPIC_LEVEL.FLS);
		
		//ch, unclear, sls
		//PreProcessor.generateQrelFile(dir, NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.UNCLEAR, NTCIR11_TOPIC_LEVEL.SLS);
		
		//en, clear
		//PreProcessor.generateQrelFile(dir, NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.CLEAR, null);
		//en, unclear, fls
		//PreProcessor.generateQrelFile(dir, NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, NTCIR11_TOPIC_LEVEL.FLS);
		//en, unclear, sls
		//PreProcessor.generateQrelFile(dir, NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, NTCIR11_TOPIC_LEVEL.SLS);
	}
}
