package org.archive.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.lucene.util.Version;
import org.archive.clickgraph.LogEdge;
import org.archive.clickgraph.LogNode;
import org.archive.clickgraph.QueryEdge;
import org.archive.clickgraph.WordEdge;
import org.archive.comon.ClickThroughDataVersion;
import org.archive.comon.ClickThroughDataVersion.ElementType;
import org.archive.comon.ClickThroughDataVersion.LogVersion;
import org.archive.comon.DataDirectory;
import org.archive.structure.Record;
import org.archive.structure.AOLRecord;
import org.archive.structure.SogouQRecord2008;
import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;
import org.archive.util.tuple.IntStrInt;
import org.archive.util.tuple.StrInt;
import org.archive.util.tuple.StrStrEdge;
import org.archive.util.tuple.StrStrInt;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;


public class ClickThroughAnalyzer {
	private static String TabSeparator = "\t";
	//unique files: AOL, SogouQ2008, SogouQ2012
	private static HashMap<String, IntStrInt> UniqueQTextMap = null;
	private static HashMap<String, StrInt> UniqueUserIDMap = null;
	private static HashMap<String, IntStrInt> UniqueClickUrlMap = null;
	private static HashMap<String, IntStrInt> UniqueWordMap = null;
	
	//
	//query-document bipartite graph, records click information between clicked documents and queries
	//thus the recorded file: the first column is query node!
	private static Graph<LogNode, LogEdge> Q_D_Graph = new UndirectedSparseGraph<LogNode, LogEdge>();
	//query-query, records co-session information
	private static Graph<LogNode, LogEdge> Q_Q_CoSession_Graph = new UndirectedSparseGraph<LogNode, LogEdge>();
	//query-query graph, record co-session, co-click information
	public static Graph<LogNode, QueryEdge> Q_Q_Graph = new UndirectedSparseGraph<LogNode, QueryEdge>();
	//query-word bi-graph, records query and its consisting words information
	private static Graph<LogNode, LogEdge> Q_W_Graph = new UndirectedSparseGraph<LogNode, LogEdge>();
	//
	private static Graph<LogNode, LogEdge> CoClick_Q_Q_Graph = new UndirectedSparseGraph<LogNode, LogEdge>();
	
	//word-word graph, record co-parent, co-session, co-click information
	private static Graph<LogNode, WordEdge> W_W_Graph = new UndirectedSparseGraph<LogNode, WordEdge>();
	
	
	//////////////////////////
	//part-1 generate unique files: attributes corresponding to :session-id(unit file), query, document(url)
	//////////////////////////	
	/**
	 * recordMap of one unit file
	 * **/
	private static HashMap<String, Vector<Record>> getRecordMapPerUnit(int unitSerial, LogVersion version){
		//target file
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		String dir = DataDirectory.RawDataRoot+DataDirectory.RawData[version.ordinal()];
		String unitFile = null;
		if(LogVersion.AOL == version){
			unitFile = dir + "user-ct-test-collection-"+unit+".txt";	
		}else if(LogVersion.SogouQ2008 == version){
			unitFile = dir + "access_log.200608"+unit+".decode.filter";
		}		
		//recordMap of one unit file
		HashMap<String, Vector<Record>> unitRecordMap = new HashMap<String, Vector<Record>>();
		//
		try{				
    		File file = new File(unitFile);
			if(file.exists()){	
				System.out.println("loading...\t"+unitFile);
				BufferedReader reader = IOText.getBufferedReader(unitFile, "GBK");
				//
				String recordLine = null;
				//int count = 0;
				Record record = null;
				//overlook the first line, which is attribute names
				if(LogVersion.AOL == version){
					reader.readLine();
				}
				while(null!=(recordLine=reader.readLine())){
					//System.out.println(count++);					
					try{							
						if(LogVersion.AOL == version){
							record = new AOLRecord(unit, recordLine);	
						}else if(LogVersion.SogouQ2008 == version){
							record = new SogouQRecord2008(unit, recordLine);
						}
					}catch(Exception ee){
						System.out.println("invalid record-line exist in "+unit);
						System.out.println(recordLine);
						System.out.println();
						recordLine=null;
						record=null;
						continue;
					}
					//
					if(null!= record && record.validRecord()){
						if(unitRecordMap.containsKey(record.getUserID())){
							unitRecordMap.get(record.getUserID()).add(record);
						}else{
							Vector<Record> recordVec = new Vector<Record>();
							recordVec.add(record);
							unitRecordMap.put(record.getUserID(), recordVec);
						}
					}																
				}
				reader.close();
				reader=null;				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		//
		return unitRecordMap;
	}
	/**
	 * get unique elements per unit folder
	 * **/
	private static void getUniqueElementsPerUnit(int unitSerial, LogVersion version){		
		//output
		String dir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_PerUnit[version.ordinal()];	
		//
		String qDir = dir +"Query/";		
		File queryDirFile = new File(qDir);
		if(!queryDirFile.exists()){
			//System.out.println(queryDirFile.mkdir());
			queryDirFile.mkdirs();
		}
		String userIDDir = dir + "UserID/";
		File userIDDirFile = new File(userIDDir);
		if(!userIDDirFile.exists()){
			userIDDirFile.mkdirs();
		}
		String urlDir = dir + "Url/";
		File urlDirFile = new File(urlDir);
		if(!urlDirFile.exists()){
			urlDirFile.mkdirs();
		}		
		//
		String qFile = qDir+version.toString()+"_UniqueQuery_"+StandardFormat.serialFormat(unitSerial, "00")+".txt";		
		String userIDFile = userIDDir+version.toString()+"_UniqueUserID_"+StandardFormat.serialFormat(unitSerial, "00")+".txt";		
		String urlFile = urlDir+version.toString()+"_UniqueUrl_"+StandardFormat.serialFormat(unitSerial, "00")+".txt";
		//
		try{
			BufferedWriter queryWriter = IOText.getBufferedWriter_UTF8(qFile);
			BufferedWriter userIDWriter = IOText.getBufferedWriter_UTF8(userIDFile);
			BufferedWriter urlWriter = IOText.getBufferedWriter_UTF8(urlFile);
			//
			Hashtable<String, StrInt> queryTable = new Hashtable<String, StrInt>();
			Hashtable<String, StrInt> urlTable = new Hashtable<String, StrInt>();
			//
			HashMap<String, Vector<Record>> unitRecordMap = getRecordMapPerUnit(unitSerial, version);
			//
			for(Entry<String, Vector<Record>> entry: unitRecordMap.entrySet()){
				//for userID
				userIDWriter.write(entry.getKey().toString());
				userIDWriter.newLine();
				//
				Vector<Record> recordVec = entry.getValue();
				//no specific session segmentation, just for the same user
				HashSet<String> distinctQPerSession = new HashSet<String>();
				//
				for(Record record: recordVec){					
					//for query
					String queryText = record.getQueryText();
					if(!distinctQPerSession.contains(queryText)){
						distinctQPerSession.add(queryText);
					}
					//for clickUrl
					String clickUrl = record.getClickUrl();
					if(null != clickUrl){
						if(urlTable.containsKey(clickUrl)){
							urlTable.get(clickUrl).intPlus1();						
						}else{
							urlTable.put(clickUrl, new StrInt(clickUrl));
						}
					}					
				}				
				//up only once
				for(Iterator<String> itr = distinctQPerSession.iterator(); itr.hasNext(); ){
					String queryText = itr.next();
					if(queryTable.containsKey(queryText)){
						queryTable.get(queryText).intPlus1();						
					}else{
						queryTable.put(queryText, new StrInt(queryText));						
					}
				}				
			}
			//for userID
			userIDWriter.flush();
			userIDWriter.close();
			//for query			
			for(StrInt queryInstance: queryTable.values()){
				queryWriter.write(queryInstance.second+TabSeparator+queryInstance.first);
				queryWriter.newLine();
			}			
			queryWriter.flush();
			queryWriter.close();
			//for clicked url
			for(StrInt urlInstance: urlTable.values()){
				urlWriter.write(urlInstance.second+TabSeparator+urlInstance.first);
				urlWriter.newLine();
			}
			urlWriter.flush();
			urlWriter.close();			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * get the distinct element per unit file from the whole query log
	 * **/
	public static void getUniqueElementsPerUnit(LogVersion version){
		if(LogVersion.AOL == version){
			for(int i=1; i<=10; i++){
				getUniqueElementsPerUnit(i, LogVersion.AOL);
			}
		}else if(LogVersion.SogouQ2008 == version){
			for(int i=1; i<=31; i++){
				getUniqueElementsPerUnit(i, LogVersion.SogouQ2008);
			}
		}		
	}
	//
	private static Vector<StrInt> loadUniqueElementsPerUnit(String targetFile, ElementType elementType){
		Vector<StrInt> elementVector = new Vector<StrInt>();
		//
		BufferedReader unitReader;
		String elementLine;
		try {
			System.out.println("loading "+targetFile);
			unitReader = IOText.getBufferedReader_UTF8(targetFile);
			//
			while(null != (elementLine=unitReader.readLine())){
				try{
					if(ElementType.UserID == elementType){
						elementVector.add(new StrInt(elementLine));
					}else{
						int elementFre = Integer.parseInt(elementLine.substring(0, elementLine.indexOf(TabSeparator)));
						String elementText = elementLine.substring(elementLine.indexOf(TabSeparator)+1);
						//
						elementVector.add(new StrInt(elementText, elementFre));
					}										
				}catch(Exception ee){						
					System.out.println("Bad Line "+elementLine);						
					continue;
				}				
			}
			unitReader.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		//
		return elementVector;
	}
	//
	private static Vector<StrInt> getUniqueElementsPerSpan(int unitStart, int unitEnd, LogVersion version, ElementType elementType){
		//input
		String inputDir = null, inputFilePrefix = null;
		String rootDir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_PerUnit[version.ordinal()];	
		//
		if(ElementType.UserID == elementType){
			inputDir = rootDir + "UserID/";
			inputFilePrefix = version.toString()+"_UniqueUserID_";	
		}else if(ElementType.Query == elementType){
			inputDir = rootDir + "Query/";
			inputFilePrefix = version.toString()+"_UniqueQuery_";
		}else{
			inputDir = rootDir + "Url/";
			inputFilePrefix = version.toString()+"_UniqueUrl_";
		}
		//collector
		int maxStrID = 0;
		HashMap<String, Integer> elementMapPerSpan = new HashMap<String, Integer>();
		Vector<StrInt> elementVectorPerSpan = new Vector<StrInt>();
		//
		inputFilePrefix = inputDir + inputFilePrefix;
		for(int i=unitStart; i<=unitEnd; i++){
			String inputFile = inputFilePrefix + StandardFormat.serialFormat(i, "00")+".txt";
			//System.out.println("Loading ... "+inputFile);
			//
			Vector<StrInt> elementVectorPerUnit = loadUniqueElementsPerUnit(inputFile, elementType);
			//
			for(StrInt element : elementVectorPerUnit){
				if(elementMapPerSpan.containsKey(element.first)){
					int index = elementMapPerSpan.get(element.first);
					elementVectorPerSpan.get(index).intPlusk(element.second);						
				}else{
					elementMapPerSpan.put(element.first, maxStrID++);
					//
					elementVectorPerSpan.add(new StrInt(element.first, element.second));						
				}
			}
		}
		//
		return elementVectorPerSpan;
	}
	//get the distinct elements at the level of the whole query log
	public static void getUniqueElementsForAll(LogVersion version){
		String outputDir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_All[version.ordinal()];		
		String uniqueUserID_AllFile = version.toString()+"_UniqueUserID_All.txt";
		String uniqueQuery_AllFile = version.toString()+"_UniqueQuery_All.txt";
		String uniqueClickUrl_AllFile = version.toString()+"_UniqueClickUrl_All.txt";  
		//
		BufferedWriter userIDWriter, queryWriter, urlWriter;
		Vector<StrInt> uniqueUserID_All=null, uniqueQuery_All=null, uniqueClickUrl_All=null;
		if(LogVersion.AOL == version){
			uniqueUserID_All = getUniqueElementsPerSpan(1, 10, LogVersion.AOL, ElementType.UserID);			
			uniqueQuery_All = getUniqueElementsPerSpan(1, 10, LogVersion.AOL, ElementType.Query);			
			uniqueClickUrl_All = getUniqueElementsPerSpan(1, 10, LogVersion.AOL, ElementType.ClickUrl);						
		}else if(LogVersion.SogouQ2008 == version){
			uniqueUserID_All = getUniqueElementsPerSpan(1, 31, LogVersion.SogouQ2008, ElementType.UserID);
			uniqueQuery_All = getUniqueElementsPerSpan(1, 31, LogVersion.SogouQ2008, ElementType.Query);
			uniqueClickUrl_All = getUniqueElementsPerSpan(1, 31, LogVersion.SogouQ2008, ElementType.ClickUrl);			
		}
		//output
		try {
			//for userID
			File outputDirFile = new File(outputDir);
			if(!outputDirFile.exists()){				
				outputDirFile.mkdirs();
			}
			userIDWriter = IOText.getBufferedWriter_UTF8(outputDir+uniqueUserID_AllFile);
			for(StrInt element: uniqueUserID_All){
				userIDWriter.write(element.getFirst());
				userIDWriter.newLine();
			}
			userIDWriter.flush();
			userIDWriter.close();
			//for query
			queryWriter = IOText.getBufferedWriter_UTF8(outputDir+uniqueQuery_AllFile);
			for(StrInt element: uniqueQuery_All){
				queryWriter.write(element.getSecond()+TabSeparator+element.getFirst());
				queryWriter.newLine();
			}
			queryWriter.flush();
			queryWriter.close();
			//for clickUrl
			urlWriter = IOText.getBufferedWriter_UTF8(outputDir+uniqueClickUrl_AllFile);
			for(StrInt element: uniqueClickUrl_All){
				urlWriter.write(element.getSecond()+TabSeparator+element.getFirst());
				urlWriter.newLine();
			}
			urlWriter.flush();
			urlWriter.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	//////////////////////////
	//access unique files
	//////////////////////////
	private static void loadUniqueQText(LogVersion version, String encoding){
		String dir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_All[version.ordinal()];		
		String uniqueQuery_AllFile = version.toString()+"_UniqueQuery_All.txt";
		//
		UniqueQTextMap = IOText.loadUniqueElements_LineFormat_IntTabStr(dir+uniqueQuery_AllFile, encoding);
		//
		if(null == UniqueQTextMap){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	private static void loadUniqueUserID(LogVersion version, String encoding){
		String dir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_All[version.ordinal()];		
		String uniqueUserID_AllFile = version.toString()+"_UniqueUserID_All.txt";
		//
		UniqueUserIDMap = IOText.loadStrInts_LineFormat_Str(dir+uniqueUserID_AllFile, encoding);
		//
		if(null == UniqueUserIDMap){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	private static void loadUniqueClickUrlMap(LogVersion version, String encoding){
		String dir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_All[version.ordinal()];		
		String uniqueClickUrl_AllFile = version.toString()+"_UniqueClickUrl_All.txt"; 
		//
		UniqueClickUrlMap = IOText.loadUniqueElements_LineFormat_IntTabStr(dir+uniqueClickUrl_AllFile, encoding);
		//
		if(null == UniqueClickUrlMap){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	//
	private static Integer getSessionID(LogVersion version, String userIDStr){		
		if(null == UniqueUserIDMap){
			loadUniqueUserID(version, "UTF-8");
		}
		StrInt uniqueUserID = UniqueUserIDMap.get(userIDStr);
		return null==uniqueUserID? null:uniqueUserID.getSecond();					
	}
	private static Integer getQTextID(LogVersion version, String qText){		
		if(null == UniqueQTextMap){
			loadUniqueQText(version, "UTF-8");
		}
		//
		IntStrInt uniqueQ = UniqueQTextMap.get(qText);
		return null==uniqueQ? null:uniqueQ.getFirst();
	}
	private static Integer getClickUrlID(LogVersion version, String urlStr){
		if(null == UniqueClickUrlMap){
			loadUniqueClickUrlMap(version, "UTF-8");
		}
		//
		IntStrInt uniqueClickUrl = UniqueClickUrlMap.get(urlStr);
		return null==uniqueClickUrl? null:uniqueClickUrl.getFirst();					
	}
	private static Integer getWordID(LogVersion version, String wordStr){
		if(null == UniqueWordMap){
			loadUniqueWord(version, "UTF-8");
			return UniqueWordMap.get(wordStr).getFirst();
		}else{
			return UniqueWordMap.get(wordStr).getFirst();
		}
	}
	//
	private static int getUniqueNumberOfQuery(LogVersion version){
		if(null == UniqueQTextMap){
			loadUniqueQText(version, "UTF-8");
			return UniqueQTextMap.size();
		}else{
			return UniqueQTextMap.size();
		}	
	}
	private static int getUniqueNumberOfUserID(LogVersion version){
		if(null == UniqueUserIDMap){
			loadUniqueUserID(version, "UTF-8");
			return UniqueUserIDMap.size();
		}else{
			return UniqueUserIDMap.size();
		}		
	}
	private static int getUniqueNumberOfClickUrl(LogVersion version){
		if(null == UniqueClickUrlMap){
			loadUniqueClickUrlMap(version, "UTF-8");
			return UniqueClickUrlMap.size();
		}else{
			return UniqueClickUrlMap.size();
		}		
	}
	
	
	//////////////////////////
	//part-2 convert to digital unit file, essentially a compression
	//////////////////////////	
	public static void convertToDigitalUnitClickThrough(LogVersion version){
		if(LogVersion.AOL == version){
			for(int i=1; i<=10; i++){
				convertToDigitalUnitClickThrough(i, version);
			}
		}else if(LogVersion.SogouQ2008 == version){
			for(int i=1; i<=31; i++){
				convertToDigitalUnitClickThrough(i, version);
			}
		}else if(LogVersion.SogouQ2012 == version){
			
		}		
	}
	//
	private static void convertToDigitalUnitClickThrough(int unitSerial, LogVersion version){
		//target file
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		String dir = DataDirectory.RawDataRoot+DataDirectory.RawData[version.ordinal()];
		String unitFile = null;
		if(LogVersion.AOL == version){
			unitFile = dir + "user-ct-test-collection-"+unit+".txt";	
		}else if(LogVersion.SogouQ2008 == version){
			unitFile = dir + "access_log.200608"+unit+".decode.filter";
		}else if(LogVersion.SogouQ2012 == version){
			
		}
		//output
		String outputDir = DataDirectory.DigitalFormatRoot+DataDirectory.DigitalFormat[version.ordinal()];	
		//
		try{
			//output			
			File outputDirFile = new File(outputDir);
			if(!outputDirFile.exists()){
				outputDirFile.mkdirs();
			}
			String digitalUnitFileName = outputDir+version.toString()+"_DigitalLog_"+unit+".txt";
			BufferedWriter dWriter = IOText.getBufferedWriter_UTF8(digitalUnitFileName);
			//
    		File fileExist = new File(unitFile);
			if(fileExist.exists()){	
				System.out.println("reading...\t"+unitFile);
				BufferedReader reader = IOText.getBufferedReader(unitFile, "GBK");
				//
				String recordLine = null;	
				//jump the first line which is attribute names
				if(LogVersion.AOL == version){
					recordLine=reader.readLine();
				}
				while(null != (recordLine=reader.readLine())){
					if(LogVersion.AOL == version){
						AOLRecord aolRecord = null;
						try{
							aolRecord = new AOLRecord(unit, recordLine);
						}catch(Exception ee){
							System.out.println("invalid record-line exist in "+unit);
							System.out.println(recordLine);																
							continue;
						}		
						if(null!=aolRecord && aolRecord.validRecord()){
							Integer sessionID = getSessionID(version, aolRecord.getUserID().toString());						
							Integer qID = getQTextID(version, aolRecord.getQueryText());							
							//
							if(null!=sessionID && null!=qID){
								if(null != aolRecord.getItemRank()){
									Integer clickUrlID = getClickUrlID(version, aolRecord.getClickUrl());
									dWriter.write(sessionID+TabSeparator
											+qID+TabSeparator
											+aolRecord.getQueryTime()+TabSeparator
											+aolRecord.getItemRank()+TabSeparator
											+clickUrlID);
									dWriter.newLine();
								}else{
									dWriter.write(sessionID+TabSeparator
											+qID+TabSeparator
											+aolRecord.getQueryTime());
									dWriter.newLine();
								}								
							}else{
								if(null==sessionID){
									System.out.println("Null sessionid:\t"+recordLine);
								}else if(null == qID){
									System.out.println("Null qid:\t"+recordLine);
								}							
							}
						}													
					}else if(LogVersion.SogouQ2008 == version){
						SogouQRecord2008 record2008 = null;
						try {
							record2008 = new SogouQRecord2008(unit, recordLine);
						}catch(Exception ee){
							System.out.println("invalid record-line exist in "+unit);
							System.out.println(recordLine);								
							continue;
						}
						//				
						if(null!=record2008 && record2008.validRecord()){
							Integer sessionID = getSessionID(version, record2008.getUserID());						
							Integer qID = getQTextID(version, record2008.getQueryText());
							Integer docID = getClickUrlID(version, record2008.getClickUrl());
							//
							if(null!=sessionID && null!=qID && null!=docID){
								dWriter.write(sessionID+TabSeparator
										+qID+TabSeparator
										+record2008.getItemRank()+TabSeparator
										+record2008.getClickOrder()+TabSeparator
										+docID);
								dWriter.newLine();
							}else{
								if(null==sessionID){
									System.out.println("Null sessionid:\t"+recordLine);
								}else if(null == qID){
									System.out.println("Null qid:\t"+recordLine);
								}else{
									System.out.println("Null docid:\t"+recordLine);
								}							
							}
						}						
					}else if(LogVersion.SogouQ2012 == version){	
						/*
						if(null!= record && record.validRecord()){
							
						}
						*/
						//
						//Integer sessionID = this.getSessionID(version, record.getUserID().toString());						
						//Integer qID = this.getQTextID(version, record.getQueryText());
						//Integer docID = this.getClickUrlID(version, record.getClickUrl());
						//							
					}else{
						new Exception("Version Error!").printStackTrace();
					}
				}
				reader.close();
				reader=null;				
			}
			dWriter.flush();
			dWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//session-id -> set of records as a unit
	private static HashMap<String, Vector<Record>> loadDigitalUnitClickThrough(int unitSerial, LogVersion version){
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		String outputDir = DataDirectory.DigitalFormatRoot+DataDirectory.DigitalFormat[version.ordinal()];		
		String digitalUnitFileName = outputDir+version.toString()+"_DigitalLog_"+unit+".txt";
		//
		HashMap<String, Vector<Record>> digitalUnitClickThroughMap = new HashMap<String, Vector<Record>>();
		//		
		File file = new File(digitalUnitFileName);
		if(file.exists()){
			try{
				System.out.println("reading...\t"+file.getAbsolutePath());
				BufferedReader reader = IOText.getBufferedReader_UTF8(file.getAbsolutePath());
				//
				String line = null;				
				Record record = null;
				//
				while(null != (line=reader.readLine())){
					if(LogVersion.AOL == version){
						//digital construct
						record = new AOLRecord(line);	
					}else if(LogVersion.SogouQ2008 == version){
						//digital construct
						record = new SogouQRecord2008(line);
					}else if(LogVersion.SogouQ2012 == version){
						//using digital construct
					}else{
						new Exception("Version Error!").printStackTrace();
					}				
					//
					if(null != record){
						if(digitalUnitClickThroughMap.containsKey(record.getUserID())){
							digitalUnitClickThroughMap.get(record.getUserID()).add(record);
						}else{
							Vector<Record> drVec = new Vector<Record>();
							drVec.add(record);
							digitalUnitClickThroughMap.put(record.getUserID(), drVec);
						}
					}else{
						System.out.println("Null DigitalRecord!");
					}
				}
				//
				reader.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		//
		return digitalUnitClickThroughMap;				
	}
	///////////////////////////
	//query-level 
	///////////////////////////
	private static void ini_D_Q_GraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int i=0; i<queryNodeNumber; i++){			
			Q_D_Graph.addVertex(new LogNode(Integer.toString(i), LogNode.NodeType.Query));
		}
		//
		int docNodeNumber = getUniqueNumberOfClickUrl(version);
		for(int j=0; j<docNodeNumber; j++){
			Q_D_Graph.addVertex(new LogNode(Integer.toString(j), LogNode.NodeType.Doc));
		}
	}
	//for aol due to day by day operation
	private static void refresh_D_Q_GraphNodes(LogVersion version){
		Q_D_Graph = null;
		Q_D_Graph = new UndirectedSparseGraph<LogNode, LogEdge>(); 
		//
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int i=0; i<queryNodeNumber; i++){			
			Q_D_Graph.addVertex(new LogNode(Integer.toString(i), LogNode.NodeType.Query));
		}
		//
		int docNodeNumber = getUniqueNumberOfClickUrl(version);
		for(int j=0; j<docNodeNumber; j++){
			Q_D_Graph.addVertex(new LogNode(Integer.toString(j), LogNode.NodeType.Doc));
		}
	}
	private static void ini_Q_Q_CoSessionGraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int i=0; i<queryNodeNumber; i++){
			Q_Q_CoSession_Graph.addVertex(new LogNode(Integer.toString(i), LogNode.NodeType.Query));
		}
	}
	//for aol due to day by day operation
	private static void refresh_Q_Q_CoSessionGraphNodes(LogVersion version){
		Q_Q_CoSession_Graph = null;
		Q_Q_CoSession_Graph = new UndirectedSparseGraph<LogNode, LogEdge>(); 
		//
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int i=0; i<queryNodeNumber; i++){
			Q_Q_CoSession_Graph.addVertex(new LogNode(Integer.toString(i), LogNode.NodeType.Query));
		}
	}
	//
	private static void ini_QQAttributeGraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int i=0; i<queryNodeNumber; i++){
			Q_Q_Graph.addVertex(new LogNode(Integer.toString(i), LogNode.NodeType.Query));
		}
	}
	//click information between query-query query-document given the time span
	private static void buildGraph_QQCoSession_QQCoClick_DQ(LogVersion version, int fromDay, int toDay){		
		//
		HashMap<String, Vector<Record>> digitalUnitClickThroughMap;
		//
		LogNode qNode=null, docNode=null;
		for(int k=fromDay; k<=toDay; k++){
			digitalUnitClickThroughMap = loadDigitalUnitClickThrough(k, version);
			//
			int count = 1;
			for(Entry<String, Vector<Record>> entry: digitalUnitClickThroughMap.entrySet()){
				count++;
				if(count%100000 == 0){
					System.out.println((count));
				}				
				//session-range queries
				HashSet<String> sessionQSet = new HashSet<String>();
				//
				Vector<Record> drVec = entry.getValue();
				for(Record dr: drVec){
					//
					if(!sessionQSet.contains(dr.getQueryText())){
						sessionQSet.add(dr.getQueryText());
					}
					//1 query - clicked document
					if(null != dr.getClickUrl()){
						qNode = new LogNode(dr.getQueryText(), LogNode.NodeType.Query);
						docNode = new LogNode(dr.getClickUrl(), LogNode.NodeType.Doc);
						//
						LogEdge d_qEdge = Q_D_Graph.findEdge(qNode, docNode);
						if(null==d_qEdge){
							d_qEdge = new LogEdge(LogEdge.EdgeType.QDoc);
							Q_D_Graph.addEdge(d_qEdge, qNode, docNode);
						}else{
							d_qEdge.upCount();
						}
					}
				}
				//
				String [] coSessionQArray = sessionQSet.toArray(new String[1]);
				for(int i=0; i<coSessionQArray.length-1; i++){
					LogNode fNode = new LogNode(coSessionQArray[i], LogNode.NodeType.Query);
					for(int j=i+1; j<coSessionQArray.length; j++){
						LogNode lNode = new LogNode(coSessionQArray[j], LogNode.NodeType.Query);
						//
						LogEdge q_qEdge = Q_Q_CoSession_Graph.findEdge(fNode, lNode);
						if(null == q_qEdge){
							q_qEdge = new LogEdge(LogEdge.EdgeType.QQ);
							Q_Q_CoSession_Graph.addEdge(q_qEdge, fNode, lNode);
						}else{
							q_qEdge.upCount();
						}				
					}
				}
			}
		}			
	}
	private static void outputGraph_QQCoSession_QQCoClick_DQ(LogVersion version){
		String dir = DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()];				
		//
		try{
			File dirFile = new File(dir);
			if(!dirFile.exists()){
				dirFile.mkdirs();
			}
			String q_q_CoSessionFile = 
				dir+"Query_Query_CoSession.txt";
			String q_q_CoClickFile = 
				dir+"Query_Query_CoClick.txt";		
			String d_q_GraphFile = 
				dir+"Query_Doc_Graph.txt";			
			
			//co-session
			BufferedWriter q_q_CoSessionWriter = IOText.getBufferedWriter_UTF8(q_q_CoSessionFile);			
			//++
			for(LogEdge edge: Q_Q_CoSession_Graph.getEdges()){
				Pair<LogNode> pair = Q_Q_CoSession_Graph.getEndpoints(edge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				//
				q_q_CoSessionWriter.write(firstNode.getID()+TabSeparator
						+secondNode.getID()+TabSeparator
						+Integer.toString(edge.getCount()));
				q_q_CoSessionWriter.newLine();				
			}
			//++			
			q_q_CoSessionWriter.flush();
			q_q_CoSessionWriter.close();
			//co-click
			BufferedWriter q_q_CoClickWriter = IOText.getBufferedWriter_UTF8(q_q_CoClickFile);
			for(LogNode node: Q_D_Graph.getVertices()){
				//doc node
				if(node.getType() == LogNode.NodeType.Doc){
					LogNode [] coClickQArray = Q_D_Graph.getNeighbors(node).toArray(new LogNode[1]);
					//
					for(int i=0; i<coClickQArray.length-1; i++){
						//
						LogEdge formerEdge = Q_D_Graph.findEdge(node, coClickQArray[i]);
						//
						for(int j=i+1; j<coClickQArray.length; j++){
							//
							LogEdge latterEdge = Q_D_Graph.findEdge(node, coClickQArray[j]);
							//
							int coFre = Math.min(formerEdge.getCount(), latterEdge.getCount());
							//
							q_q_CoClickWriter.write(coClickQArray[i].getID()+TabSeparator
									+coClickQArray[j].getID()+TabSeparator
									+Integer.toString(coFre));
							q_q_CoClickWriter.newLine();
						}				
					}
				}
			}
			//
			q_q_CoClickWriter.flush();
			q_q_CoClickWriter.close();
			//--
			//d-q file
			BufferedWriter d_q_Writer = IOText.getBufferedWriter_UTF8(d_q_GraphFile);
			for(LogEdge edge: Q_D_Graph.getEdges()){
				Pair<LogNode> pair = Q_D_Graph.getEndpoints(edge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				//put query at the first position
				if(firstNode.getType() == LogNode.NodeType.Query){
					d_q_Writer.write(firstNode.getID()+TabSeparator
							+secondNode.getID()+TabSeparator
							+edge.getCount());
					d_q_Writer.newLine();
				}else{
					d_q_Writer.write(secondNode.getID()+TabSeparator
							+firstNode.getID()+TabSeparator
							+edge.getCount());
					d_q_Writer.newLine();
				}
			}
			//--
			/*
			for(LogNode node: this.D_Q_Graph.getVertices()){
				if(node.getType() == LogNode.NodeType.Doc){
					d_q_Writer.write(node.getID());
					for(LogNode nNode: this.D_Q_Graph.getNeighbors(node)){
						LogEdge edge = this.D_Q_Graph.findEdge(node, nNode);
						d_q_Writer.write(Separator+nNode.getID()+":"+edge.getCount());
					}
					d_q_Writer.newLine();
				}				
			}
			*/
			//--			
			d_q_Writer.flush();
			d_q_Writer.close();			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//
	private static void mergeUnitGraph_QQCoSession_QQCoClick_DQ(LogVersion version){		
		if(LogVersion.AOL == version){
			//total
			HashMap<StrStrEdge, StrStrInt> qqCoSessionMap = new HashMap<StrStrEdge, StrStrInt>();
			HashMap<StrStrEdge, StrStrInt> qqCoClickMap = new HashMap<StrStrEdge, StrStrInt>();
			//because of query-document order
			HashMap<org.archive.util.tuple.Pair<String, String>, StrStrInt> dqMap = 
				new HashMap<org.archive.util.tuple.Pair<String, String>, StrStrInt>();					
			//unit files
			String unitDir = DataDirectory.ClickThroughGraphRoot+DataDirectory.UnitGraphFile[version.ordinal()];
			String line;
			String [] array;		
			File unitDirFile = new File(unitDir);
			File [] unitFileList = unitDirFile.listFiles();
			for(File unitFile: unitFileList){
				String pathString = unitFile.getAbsolutePath();
				try {
					if(pathString.indexOf("Query_Query_CoSession") > 0){
						System.out.println("merging:\t"+pathString);
						BufferedReader unitQQCoSessionReader = IOText.getBufferedReader_UTF8(pathString);			
						while(null != (line=unitQQCoSessionReader.readLine())){
							if(line.length() > 0){					
								array = line.split(TabSeparator);
								StrStrEdge edge = new StrStrEdge(array[0], array[1]);
								if(qqCoSessionMap.containsKey(edge)){
									qqCoSessionMap.get(edge).upThird(Integer.parseInt(array[2]));
								}else{
									qqCoSessionMap.put(edge, new StrStrInt(array[0], array[1], Integer.parseInt(array[2])));
								}											
							}				
						}
						unitQQCoSessionReader.close();
					}else if(pathString.indexOf("Query_Query_CoClick") > 0){
						System.out.println("merging:\t"+pathString);
						BufferedReader unitQQCoClickReader = IOText.getBufferedReader_UTF8(pathString);
						while(null != (line=unitQQCoClickReader.readLine())){
							if(line.length() > 0){
								array = line.split(TabSeparator);
								StrStrEdge edge = new StrStrEdge(array[0], array[1]);
								if(qqCoClickMap.containsKey(edge)){
									qqCoClickMap.get(edge).upThird(Integer.parseInt(array[2]));
								}else{
									qqCoClickMap.put(edge, new StrStrInt(array[0], array[1], Integer.parseInt(array[2])));
								}
							}
						}
						unitQQCoClickReader.close();
					}else if(pathString.indexOf("Query_Doc_Graph") > 0){
						System.out.println("merging:\t"+pathString);
						BufferedReader unitDQReader = IOText.getBufferedReader_UTF8(pathString);
						while(null != (line=unitDQReader.readLine())){
							if(line.length() > 0){
								array = line.split(TabSeparator);
								org.archive.util.tuple.Pair<String, String> edge = 
									new org.archive.util.tuple.Pair<String, String>(array[0], array[1]);
								if(dqMap.containsKey(edge)){
									dqMap.get(edge).upThird(Integer.parseInt(array[2]));
								}else{
									dqMap.put(edge, new StrStrInt(array[0], array[1], Integer.parseInt(array[2])));
								}
							}
						}
						unitDQReader.close();	
					}else{
						new Exception("Unexcepted File Error!").printStackTrace();
					}
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}				
			}			
			//final output			
			String outputDir = DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()];
			try {
				File dirFile = new File(outputDir);
				if(!dirFile.exists()){
					dirFile.mkdirs();
				}
				//
				String qqCoSessionFile = outputDir+"Query_Query_CoSession.txt";
				BufferedWriter qqCoSessionWriter = IOText.getBufferedWriter_UTF8(qqCoSessionFile);
				for(Entry<StrStrEdge, StrStrInt> entry: qqCoSessionMap.entrySet()){
					StrStrInt e = entry.getValue();
					qqCoSessionWriter.write(e.first+TabSeparator
							+e.second+TabSeparator
							+e.third);
					qqCoSessionWriter.newLine();
				}
				qqCoSessionWriter.flush();
				qqCoSessionWriter.close();
				//
				String qqCoClickFile = outputDir+"Query_Query_CoClick.txt";
				BufferedWriter qqCoClickWriter = IOText.getBufferedWriter_UTF8(qqCoClickFile);
				for(Entry<StrStrEdge, StrStrInt> entry: qqCoClickMap.entrySet()){
					StrStrInt e = entry.getValue();
					qqCoClickWriter.write(e.first+TabSeparator
							+e.second+TabSeparator
							+e.third);
					qqCoClickWriter.newLine();
				}
				qqCoClickWriter.flush();
				qqCoClickWriter.close();
				//
				String dqGraphFile = outputDir+"Query_Doc_Graph.txt";
				BufferedWriter dqWriter = IOText.getBufferedWriter_UTF8(dqGraphFile);
				for(Entry<org.archive.util.tuple.Pair<String, String>, StrStrInt> entry: dqMap.entrySet()){
					StrStrInt e = entry.getValue();
					dqWriter.write(e.first+TabSeparator
							+e.second+TabSeparator
							+e.third);
					dqWriter.newLine();
				}
				dqWriter.flush();
				dqWriter.close();				
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}else{
			new Exception("Versin Error!").printStackTrace();
		}
	}
	
	//
	private static void buildUnitGraph_QQCoSession_QQCoClick_DQ(LogVersion version, int unitSerial){
		//load unit digital clickthrough file
		HashMap<String, Vector<Record>>  digitalUnitClickThroughMap = loadDigitalUnitClickThrough(unitSerial, version);		
		//
		//unit files output
		String outputDir = DataDirectory.ClickThroughGraphRoot+DataDirectory.UnitGraphFile[version.ordinal()];
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		//
		int count = 1;
		int segmentSerial = 1;
		LogNode qNode=null, docNode=null;
		for(Entry<String, Vector<Record>> entry: digitalUnitClickThroughMap.entrySet()){
			//segment file
			if(count%10000 == 0){
				//generate segment files
				try{
					File outputDirFile = new File(outputDir);
					if(!outputDirFile.exists()){
						outputDirFile.mkdirs();
					}
					//
					String segment = StandardFormat.serialFormat(segmentSerial, "00");
					segmentSerial++;
					//
					String qqCoSessionFile = outputDir+"Query_Query_CoSession_"+unit+"_"+segment+".txt";
					String qqCoClickFile = outputDir+"Query_Query_CoClick_"+unit+"_"+segment+".txt";		
					String dqGraphFile = outputDir+"Query_Doc_Graph_"+unit+"_"+segment+".txt";			
					
					//co-session
					BufferedWriter qqCoSessionWriter = IOText.getBufferedWriter_UTF8(qqCoSessionFile);					
					for(LogEdge edge: Q_Q_CoSession_Graph.getEdges()){
						Pair<LogNode> pair = Q_Q_CoSession_Graph.getEndpoints(edge);
						LogNode firstNode = pair.getFirst();
						LogNode secondNode = pair.getSecond();
						//
						qqCoSessionWriter.write(firstNode.getID()+TabSeparator
								+secondNode.getID()+TabSeparator
								+Integer.toString(edge.getCount()));
						qqCoSessionWriter.newLine();				
					}								
					qqCoSessionWriter.flush();
					qqCoSessionWriter.close();
					//co-click
					BufferedWriter qqCoClickWriter = IOText.getBufferedWriter_UTF8(qqCoClickFile);
					for(LogNode node: Q_D_Graph.getVertices()){
						//doc node
						if(node.getType() == LogNode.NodeType.Doc){
							LogNode [] coClickQArray = Q_D_Graph.getNeighbors(node).toArray(new LogNode[1]);
							//
							for(int i=0; i<coClickQArray.length-1; i++){
								//
								LogEdge formerEdge = Q_D_Graph.findEdge(node, coClickQArray[i]);
								//
								for(int j=i+1; j<coClickQArray.length; j++){
									//
									LogEdge latterEdge = Q_D_Graph.findEdge(node, coClickQArray[j]);
									//
									int coFre = Math.min(formerEdge.getCount(), latterEdge.getCount());
									//
									qqCoClickWriter.write(coClickQArray[i].getID()+TabSeparator
											+coClickQArray[j].getID()+TabSeparator
											+Integer.toString(coFre));
									qqCoClickWriter.newLine();
								}				
							}
						}
					}					
					qqCoClickWriter.flush();
					qqCoClickWriter.close();
					//--
					//d-q file
					BufferedWriter dqWriter = IOText.getBufferedWriter_UTF8(dqGraphFile);
					for(LogEdge edge: Q_D_Graph.getEdges()){
						Pair<LogNode> pair = Q_D_Graph.getEndpoints(edge);
						LogNode firstNode = pair.getFirst();
						LogNode secondNode = pair.getSecond();
						//put query at the first position
						if(firstNode.getType() == LogNode.NodeType.Query){
							dqWriter.write(firstNode.getID()+TabSeparator
									+secondNode.getID()+TabSeparator
									+edge.getCount());
							dqWriter.newLine();
						}else{
							dqWriter.write(secondNode.getID()+TabSeparator
									+firstNode.getID()+TabSeparator
									+edge.getCount());
							dqWriter.newLine();
						}
					}							
					dqWriter.flush();
					dqWriter.close();			
				}catch(Exception e){
					e.printStackTrace();
				}	
				//
				refresh_D_Q_GraphNodes(version);
				refresh_Q_Q_CoSessionGraphNodes(version);
			}
			//--
			//session-range queries
			HashSet<String> sessionQSet = new HashSet<String>();
			//
			Vector<Record> drVec = entry.getValue();
			for(Record dr: drVec){
				//
				if(!sessionQSet.contains(dr.getQueryText())){
					sessionQSet.add(dr.getQueryText());
				}
				//1 query - clicked document
				if(null != dr.getClickUrl()){
					qNode = new LogNode(dr.getQueryText(), LogNode.NodeType.Query);
					docNode = new LogNode(dr.getClickUrl(), LogNode.NodeType.Doc);
					//
					LogEdge d_qEdge = Q_D_Graph.findEdge(qNode, docNode);
					if(null==d_qEdge){
						d_qEdge = new LogEdge(LogEdge.EdgeType.QDoc);
						Q_D_Graph.addEdge(d_qEdge, qNode, docNode);
					}else{
						d_qEdge.upCount();
					}
				}
			}
			//
			String [] coSessionQArray = sessionQSet.toArray(new String[1]);
			for(int i=0; i<coSessionQArray.length-1; i++){
				LogNode fNode = new LogNode(coSessionQArray[i], LogNode.NodeType.Query);
				for(int j=i+1; j<coSessionQArray.length; j++){
					LogNode lNode = new LogNode(coSessionQArray[j], LogNode.NodeType.Query);
					//
					LogEdge q_qEdge = Q_Q_CoSession_Graph.findEdge(fNode, lNode);
					if(null == q_qEdge){
						q_qEdge = new LogEdge(LogEdge.EdgeType.QQ);
						Q_Q_CoSession_Graph.addEdge(q_qEdge, fNode, lNode);
					}else{
						q_qEdge.upCount();
					}				
				}
			}
		}		
		//remaining output
		try{			
			String segment = StandardFormat.serialFormat(segmentSerial, "00");
			//
			String q_q_CoSessionFile = 
				outputDir+"Query_Query_CoSession_"+unit+"_"+segment+".txt";
			String q_q_CoClickFile = 
				outputDir+"Query_Query_CoClick_"+unit+"_"+segment+".txt";		
			String d_q_GraphFile = 
				outputDir+"Query_Doc_Graph_"+unit+"_"+segment+".txt";			
			
			//co-session
			BufferedWriter q_q_CoSessionWriter = IOText.getBufferedWriter_UTF8(q_q_CoSessionFile);			
			//++
			for(LogEdge edge: Q_Q_CoSession_Graph.getEdges()){
				Pair<LogNode> pair = Q_Q_CoSession_Graph.getEndpoints(edge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				//
				q_q_CoSessionWriter.write(firstNode.getID()+TabSeparator
						+secondNode.getID()+TabSeparator
						+Integer.toString(edge.getCount()));
				q_q_CoSessionWriter.newLine();				
			}
			//++			
			q_q_CoSessionWriter.flush();
			q_q_CoSessionWriter.close();
			//co-click
			BufferedWriter q_q_CoClickWriter = IOText.getBufferedWriter_UTF8(q_q_CoClickFile);
			for(LogNode node: Q_D_Graph.getVertices()){
				//doc node
				if(node.getType() == LogNode.NodeType.Doc){
					LogNode [] coClickQArray = Q_D_Graph.getNeighbors(node).toArray(new LogNode[1]);
					//
					for(int i=0; i<coClickQArray.length-1; i++){
						//
						LogEdge formerEdge = Q_D_Graph.findEdge(node, coClickQArray[i]);
						//
						for(int j=i+1; j<coClickQArray.length; j++){
							//
							LogEdge latterEdge = Q_D_Graph.findEdge(node, coClickQArray[j]);
							//
							int coFre = Math.min(formerEdge.getCount(), latterEdge.getCount());
							//
							q_q_CoClickWriter.write(coClickQArray[i].getID()+TabSeparator
									+coClickQArray[j].getID()+TabSeparator
									+Integer.toString(coFre));
							q_q_CoClickWriter.newLine();
						}				
					}
				}
			}
			//
			q_q_CoClickWriter.flush();
			q_q_CoClickWriter.close();
			//--
			//d-q file
			BufferedWriter d_q_Writer = IOText.getBufferedWriter_UTF8(d_q_GraphFile);
			for(LogEdge edge: Q_D_Graph.getEdges()){
				Pair<LogNode> pair = Q_D_Graph.getEndpoints(edge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				//put query at the first position
				if(firstNode.getType() == LogNode.NodeType.Query){
					d_q_Writer.write(firstNode.getID()+TabSeparator
							+secondNode.getID()+TabSeparator
							+edge.getCount());
					d_q_Writer.newLine();
				}else{
					d_q_Writer.write(secondNode.getID()+TabSeparator
							+firstNode.getID()+TabSeparator
							+edge.getCount());
					d_q_Writer.newLine();
				}
			}
			//--
			/*
			for(LogNode node: this.D_Q_Graph.getVertices()){
				if(node.getType() == LogNode.NodeType.Doc){
					d_q_Writer.write(node.getID());
					for(LogNode nNode: this.D_Q_Graph.getNeighbors(node)){
						LogEdge edge = this.D_Q_Graph.findEdge(node, nNode);
						d_q_Writer.write(Separator+nNode.getID()+":"+edge.getCount());
					}
					d_q_Writer.newLine();
				}				
			}
			*/
			//--			
			d_q_Writer.flush();
			d_q_Writer.close();			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//
	/**
	 * generate the QueryCoSessioinGraph, QueryDocGraph
	 * **/
	public static void generateFiles_QQCoSessioin_QQCoClick_QDGraph(LogVersion version){
		
		if(LogVersion.AOL == version){			
			//step-1: generate unit graph files
			for(int unitSerial=1; unitSerial<=10; unitSerial++){
				refresh_D_Q_GraphNodes(version);
				refresh_Q_Q_CoSessionGraphNodes(version);
				//
				buildUnitGraph_QQCoSession_QQCoClick_DQ(version, unitSerial);
			}
			//step-2: aggregate the unit graph files	
			
		}else if(LogVersion.SogouQ2008 == version){
			ini_D_Q_GraphNodes(version);
			ini_Q_Q_CoSessionGraphNodes(version);
			//all days in one operation
			buildGraph_QQCoSession_QQCoClick_DQ(version, 1, 31);
			//
			outputGraph_QQCoSession_QQCoClick_DQ(version);
		}else if(LogVersion.SogouQ2012 == version){
			ini_D_Q_GraphNodes(version);
			ini_Q_Q_CoSessionGraphNodes(version);
			//all days in one operation
			buildGraph_QQCoSession_QQCoClick_DQ(version, -1, -1);
			//
			outputGraph_QQCoSession_QQCoClick_DQ(version);
		}else{
			new Exception("Version Error!").printStackTrace();
		}
	}
	//
	private static void generateFiles_QQAttributeGraph(LogVersion version){
		ini_QQAttributeGraphNodes(version);
		//
		String line;
		String[] array;
		try{
			//co-session attribute
			String coSessionFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_CoSession.txt";
			BufferedReader coSessionReader = IOText.getBufferedReader_UTF8(coSessionFile);
			//			
			while(null != (line=coSessionReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					if(array.length > 1){
						LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Query);
						LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Query);						
						int fre = Integer.parseInt(array[2]);
						//
						QueryEdge edge = Q_Q_Graph.findEdge(firstNode, secondNode);						
						if(null != edge){
							edge.upAttributeCount(QueryEdge.QCoType.CoSession, fre);
						}else{
							edge = new QueryEdge();
							edge.upAttributeCount(QueryEdge.QCoType.CoSession, fre);
							Q_Q_Graph.addEdge(edge, firstNode, secondNode);
						}
					}
				}
			}
			//
			coSessionReader.close();
			//co-click attribute
			String coClickFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_CoClick.txt";	
			BufferedReader coClickReader = IOText.getBufferedReader_UTF8(coClickFile);
			//	
			while(null != (line=coClickReader.readLine())){
				if(line.length() > 0){
					array = line.split(TabSeparator);
					if(array.length > 1){
						LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Query);
						LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Query);
						int fre = Integer.parseInt(array[2]);
						//
						QueryEdge edge = Q_Q_Graph.findEdge(firstNode, secondNode);
						if(null != edge){
							edge.upAttributeCount(QueryEdge.QCoType.CoClick, fre);
						}else{
							edge = new QueryEdge();
							edge.upAttributeCount(QueryEdge.QCoType.CoClick, fre);
							Q_Q_Graph.addEdge(edge, firstNode, secondNode);
						}
					}
				}
			}
			coClickReader.close();
			//
			String qqAttributeFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_Attribute.txt";
			BufferedWriter qqWriter = IOText.getBufferedWriter_UTF8(qqAttributeFile);
			//
			for(QueryEdge edge: Q_Q_Graph.getEdges()){
				Pair<LogNode> pair = Q_Q_Graph.getEndpoints(edge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				int [] qCoArray = edge.getQCoArray();
				//
				qqWriter.write(firstNode.getID()+TabSeparator
						+secondNode.getID());
				//
				for(int i=0; i<qCoArray.length; i++){
					qqWriter.write(":"+Integer.toString(qCoArray[i]));
				}
				qqWriter.newLine();
			}
			qqWriter.flush();
			qqWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//
	public static void loadQ_Q_AttributeGraph(LogVersion version){
		ini_QQAttributeGraphNodes(version);
		//
		try{
			String qqAttributeFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_Attribute.txt";
			BufferedReader qqAttributeReader = IOText.getBufferedReader_UTF8(qqAttributeFile);
			//
			String line;
			String[] array;
			int count = 0;
			while(null != (line=qqAttributeReader.readLine())){
				if(line.length() > 0){
					//System.out.println(count++);
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Query);
					//
					String [] parts = array[1].split(":");
					LogNode secondNode = new LogNode(parts[0], LogNode.NodeType.Query);
					//
					QueryEdge edge = new QueryEdge();
					edge.upAttributeCount(QueryEdge.QCoType.CoSession, Integer.parseInt(parts[1]));
					edge.upAttributeCount(QueryEdge.QCoType.CoClick, Integer.parseInt(parts[2]));
					//
					Q_Q_Graph.addEdge(edge, firstNode, secondNode);
					//
					line = null;
				}				
			}
			qqAttributeReader.close();			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Interface for q-q attributes
	 * **/
	public static int [] getCoInfoOfQToQ(LogVersion version, String qStr_1, String qStr_2){
		Integer qID_1 = getQTextID(version, qStr_1);
		Integer qID_2 = getQTextID(version, qStr_2);
		if(null!=qID_1 && null!=qID_2 && qID_1!=qID_2){
			//
			if(null == Q_Q_Graph){
				ini_QQAttributeGraphNodes(version);
			}
			//
			LogNode firstNode = new LogNode(qID_1.toString(), LogNode.NodeType.Query);
			LogNode secondNode = new LogNode(qID_2.toString(), LogNode.NodeType.Query);
			QueryEdge edge = Q_Q_Graph.findEdge(firstNode, secondNode);
			if(null != edge){
				return edge.getQCoArray();
			}
		}
		//
		return null;
	}
	
	////////////////////////////////
	//word-level
	///////////////////////////////	
	public static void parsingQueriesToWords(LogVersion version){		
		loadUniqueQText(version, "UTF-8");		
		//
		String uniqueWordFile = 
			DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"UniqueWord.txt";
    	String wordToSourceQFile = 
    		DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_SourceQueries.txt";
    	String queryToMemberWordsFile = 
    		DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_MemberWords.txt";
		//word id
    	int wordID = 1;
    	Hashtable<String, Integer> wordTable = new Hashtable<String, Integer>();
    	Vector<UserWord> wordNodeVec = new Vector<UserWord>();
    	//    	
    	Vector<String> words;
		//
    	try {
    		//query to member words
    		BufferedWriter queryToMemberWordsWriter = IOText.getBufferedWriter_UTF8(queryToMemberWordsFile);
    		//
    		for(Entry<String, IntStrInt> entry: UniqueQTextMap.entrySet()){
    			IntStrInt uniqueQ = entry.getValue();
    			//
    			words = UserQuery.getWords(uniqueQ.getSecond());
    			if(null!=words){
    				//distinct composing words
    				HashSet<String> wordSet = new HashSet<String>();
    				for(String word: words){
    					if(!wordSet.contains(word)){
    						wordSet.add(word);
    					}
    				}
    				//record source query and log frequency
    				for(Iterator<String> itr = wordSet.iterator(); itr.hasNext();){
    					String w = itr.next();
    					if(wordTable.containsKey(w)){
    						int id = wordTable.get(w);
    						//
    						UserWord wNode = wordNodeVec.get(id);
    						//
    						wNode.addFre(uniqueQ.getThird());
    						wNode.addSourceQ(uniqueQ.getFirst());
    					}else{    						
    						wordTable.put(w, wordID++);
    						//
    						UserWord wNode = new UserWord(w, uniqueQ.getThird(), uniqueQ.getFirst());
    						wordNodeVec.add(wNode);
    					}
    				}
    				//record q to words
    				queryToMemberWordsWriter.write(Integer.toString(uniqueQ.getFirst()));
    				for(Iterator<String> itr=wordSet.iterator(); itr.hasNext();){
    					String w = itr.next();
    					int wid = wordTable.get(w);
    					queryToMemberWordsWriter.write(TabSeparator+Integer.toString(wid));        					
    				}
    				queryToMemberWordsWriter.newLine();
    			} 
    		}
    		queryToMemberWordsWriter.flush();
    		queryToMemberWordsWriter.close();
    		//unique words and word to source queries    		
    		BufferedWriter uniqueWordsWriter = IOText.getBufferedWriter_UTF8(uniqueWordFile);
    		BufferedWriter wordToSourceQWriter = IOText.getBufferedWriter_UTF8(wordToSourceQFile);
			//
    		UserWord wNode;
    		Vector<Integer> sourceQList;
    		for(int i=0; i<wordNodeVec.size(); i++){
    			wNode = wordNodeVec.get(i);
    			sourceQList = wNode.sourceQList;
    			//to word small text
    			uniqueWordsWriter.write(wNode.logFre+TabSeparator+wNode.word);
    			uniqueWordsWriter.newLine();
    			//to word-query small text
    			//wordToQuerySmallTextWriter.write(Integer.toString(i));
    			for(Integer sQID: sourceQList){
    				wordToSourceQWriter.write(Integer.toString(i)+TabSeparator+sQID.toString());
    				wordToSourceQWriter.newLine();
    			}
    			//wordToQuerySmallTextWriter.newLine();
    		}    		
    		//
    		uniqueWordsWriter.flush();
    		uniqueWordsWriter.close();
    		//
    		wordToSourceQWriter.flush();
    		wordToSourceQWriter.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	//
	private static void loadUniqueWord(LogVersion version, String encoding){		
		String uniqueWordFile = 
			DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"UniqueWord.txt";
		//
		UniqueWordMap = IOText.loadUniqueElements_LineFormat_IntTabStr(uniqueWordFile, encoding);
		//
		if(null == UniqueWordMap){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	//
	private static int getUniqueNumberOfWord(LogVersion version){
		if(null == UniqueWordMap){
			loadUniqueWord(version, "UTF-8");
			return UniqueWordMap.size();
		}else{
			return UniqueWordMap.size();
		}	
	}
	private static void ini_Q_W_GraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int i=0; i<queryNodeNumber; i++){
			Q_W_Graph.addVertex(new LogNode(Integer.toString(i), LogNode.NodeType.Query));
		}
		//
		int wordNodeNumber = getUniqueNumberOfWord(version);
		for(int i=0; i<wordNodeNumber; i++){
			Q_W_Graph.addVertex(new LogNode(Integer.toString(i), LogNode.NodeType.Word));
		}
	}
	private static void ini_Q_Q_CoClickGraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int i=0; i<queryNodeNumber; i++){
			CoClick_Q_Q_Graph.addVertex(new LogNode(Integer.toString(i), LogNode.NodeType.Query));
		}
	}
	private static void loadQ_W_Graph(LogVersion version){
		ini_Q_W_GraphNodes(version);
		//
		String queryToMemberWordsFile = 
    		DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_MemberWords.txt";
		//
		try{
			BufferedReader reader = IOText.getBufferedReader_UTF8(queryToMemberWordsFile);
			//
			String line;
			String[] array;
			while(null != (line=reader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					if(array.length>1){
						LogNode qNode = new LogNode(array[0], LogNode.NodeType.Query);
						for(int i=1; i<array.length; i++){
							LogNode wNode = new LogNode(array[i], LogNode.NodeType.Word);
							//
							LogEdge qwEdge = new LogEdge(LogEdge.EdgeType.WQuery);
							Q_W_Graph.addEdge(qwEdge, qNode, wNode);
						}
					}
				}
			}
			reader.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		//test
		/*
		LogNode qNode = new LogNode("236", LogNode.NodeType.Query);
		for(LogNode wNode: this.Q_W_Graph.getNeighbors(qNode)){
			System.out.print(TextDataBase.getWordStr(Integer.parseInt(wNode.getID()))+"\t");
		}
		*/
	}
	//
	public static void loadQ_Q_CoSessionGraph(LogVersion version){
		ini_Q_Q_CoSessionGraphNodes(version);
		//
		String q_q_CoSessionFile = 
			DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_CoSession.txt";
		try{
			BufferedReader reader = IOText.getBufferedReader_UTF8(q_q_CoSessionFile);
			//
			String line;
			String[] array;
			while(null != (line=reader.readLine())){
				if(line.length() > 0){
					array = line.split(TabSeparator);
					if(array.length > 1){
						LogNode sNode = new LogNode(array[0], LogNode.NodeType.Query);
						for(int i=1; i<array.length; i++){
							String [] strFre = array[i].split(":");
							String tStr = strFre[0];
							int fre = Integer.parseInt(strFre[1]);
							//
							LogNode tNode = new LogNode(tStr, LogNode.NodeType.Query);
							LogEdge edge = Q_Q_CoSession_Graph.findEdge(sNode, tNode);
							if(edge == null){
								edge = new LogEdge(LogEdge.EdgeType.QQ);
								edge.setCount(fre);
								Q_Q_CoSession_Graph.addEdge(edge, sNode, tNode);
							}
						}
					}
				}
			}
			reader.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//
	private static void loadCoClick_Q_Q_Graph(LogVersion version){
		ini_Q_Q_CoClickGraphNodes(version);
		try{
			String q_q_CoClickFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_CoClick.txt";
			BufferedReader reader = IOText.getBufferedReader_UTF8(q_q_CoClickFile);
			//
			String line;
			String[] array;
			while(null != (line=reader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Query);
					LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Query);
					LogEdge edge = new LogEdge(LogEdge.EdgeType.QQ);
					edge.setCount(Integer.parseInt(array[2]));
					CoClick_Q_Q_Graph.addEdge(edge, firstNode, secondNode);
				}
			}
			reader.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private static void generateWordCoParentFile(LogVersion version){
		//
		loadQ_W_Graph(version);
		//co-parent
		try{
			String wwCoParentFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoParent.txt";
			BufferedWriter wwCoParentWriter = IOText.getBufferedWriter_UTF8(wwCoParentFile);
			//
			int queryNodeNumber = getUniqueNumberOfQuery(version);
			//for using UniqueQTextMap
			loadUniqueQText(version, "UTF-8");
			for(Entry<String, IntStrInt> entry: UniqueQTextMap.entrySet()){
				IntStrInt uniqueQ = entry.getValue();
				//
				LogNode qNode = new LogNode(Integer.toString(uniqueQ.getFirst()), LogNode.NodeType.Query);
				//
				LogNode [] wNodeArray = Q_W_Graph.getNeighbors(qNode).toArray(new LogNode[1]);
				if(null == wNodeArray){
					System.out.println("Null consisting words");
					continue;
				}				
				//
				for(int j=0; j<wNodeArray.length-1; j++){
					for(int k=j+1; k<wNodeArray.length; k++){
						//
						wwCoParentWriter.write(wNodeArray[j].getID()+TabSeparator
								+wNodeArray[k].getID()+TabSeparator
								+Integer.toString(uniqueQ.getThird()));
						//
						wwCoParentWriter.newLine();												
					}
				}
			}			
			//
			wwCoParentWriter.flush();
			wwCoParentWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	private static void generateWordCoSessionFile(LogVersion version){
		//query co-session
		loadQ_Q_CoSessionGraph(version);
		//query with its consisting words
		loadQ_W_Graph(version);		
		//
		try{
			String w_w_CoSessionFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoSession.txt";
			//			
			BufferedWriter wwCoSessionWriter = IOText.getBufferedWriter_UTF8(w_w_CoSessionFile);
			//
			for(LogEdge qqEdge: Q_Q_CoSession_Graph.getEdges()){
				Pair<LogNode> pair = Q_Q_CoSession_Graph.getEndpoints(qqEdge);
				LogNode firstQNode = pair.getFirst();
				LogNode secondQNode = pair.getSecond();
				//
				for(LogNode firstWordNode: Q_W_Graph.getNeighbors(firstQNode)){
					for(LogNode secondWordNode: Q_W_Graph.getNeighbors(secondQNode)){
						if(!firstWordNode.equals(secondWordNode)){
							//
							wwCoSessionWriter.write(firstWordNode.getID()+TabSeparator
									+secondWordNode.getID()+TabSeparator
									+Integer.toString(qqEdge.getCount()));
							wwCoSessionWriter.newLine();
						}
					}
				}			
			}
			//
			wwCoSessionWriter.flush();
			wwCoSessionWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}				
	}
	private static void generateWordCoClickFile(LogVersion version){		
		//
		loadQ_W_Graph(version);		
		loadCoClick_Q_Q_Graph(version);
		//
		String w_w_CoClickFile = 
			DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoClick.txt";
		try{
			BufferedWriter w_w_Writer = IOText.getBufferedWriter_UTF8(w_w_CoClickFile);
			//
			for(LogEdge edge: CoClick_Q_Q_Graph.getEdges()){
				Pair<LogNode> pair = CoClick_Q_Q_Graph.getEndpoints(edge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				//
				for(LogNode fWNode: Q_W_Graph.getNeighbors(firstNode)){
					for(LogNode sWNode: Q_W_Graph.getNeighbors(secondNode)){
						if(!fWNode.equals(sWNode)){
							w_w_Writer.write(fWNode.getID()+TabSeparator
									+sWNode.getID()+TabSeparator+Integer.toString(edge.getCount()));
							//
							w_w_Writer.newLine();
						}
					}
				}
			}
			//
			w_w_Writer.flush();
			w_w_Writer.close();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	//
	private static void ini_W_W_GraphNodes(LogVersion version){
		int wordNodeNumber = getUniqueNumberOfWord(version);
		for(int i=0; i<wordNodeNumber; i++){
			W_W_Graph.addVertex(new LogNode(Integer.toString(i), LogNode.NodeType.Word));
		}
	}
	public void generateGraphFile_W_W(LogVersion version){
		ini_W_W_GraphNodes(version);
		//
		String line;
		String[] array;		
		try{
			String wwCoParentFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoParent.txt";
			BufferedReader coParentReader = IOText.getBufferedReader_UTF8(wwCoParentFile);
			//			
			while(null != (line=coParentReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					if(array.length > 1){
						LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Word);
						LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Word);
						int fre = Integer.parseInt(array[2]);
						//
						WordEdge edge = this.W_W_Graph.findEdge(firstNode, secondNode);
						if(null != edge){
							edge.upAttributeCount(WordEdge.WCoType.CoParent, fre);
						}else{
							edge = new WordEdge();
							edge.upAttributeCount(WordEdge.WCoType.CoParent, fre);
							this.W_W_Graph.addEdge(edge, firstNode, secondNode);
						}						
					}
				}
			}
			coParentReader.close();
			//co-session
			String w_w_CoSessionFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoSession.txt";
			BufferedReader coSessionReader = IOText.getBufferedReader_UTF8(w_w_CoSessionFile);
			//			
			while(null != (line=coSessionReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Word);
					LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Word);
					int fre = Integer.parseInt(array[2]);
					//
					WordEdge edge = this.W_W_Graph.findEdge(firstNode, secondNode);
					if(null != edge){
						edge.upAttributeCount(WordEdge.WCoType.CoSession, fre);
					}else{
						edge = new WordEdge();
						edge.upAttributeCount(WordEdge.WCoType.CoSession, fre);
						this.W_W_Graph.addEdge(edge, firstNode, secondNode);
					}
				}
			}
			coSessionReader.close();
			//co-click
			String w_w_CoClickFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoClick.txt";
			BufferedReader coClickReader = IOText.getBufferedReader_UTF8(w_w_CoClickFile);
			//
			while(null != (line=coClickReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Word);
					LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Word);
					int fre = Integer.parseInt(array[2]);
					WordEdge edge = this.W_W_Graph.findEdge(firstNode, secondNode);
					if(null!=edge){
						edge.upAttributeCount(WordEdge.WCoType.CoClick, fre);
					}else{
						edge = new WordEdge();
						edge.upAttributeCount(WordEdge.WCoType.CoClick, fre);
						this.W_W_Graph.addEdge(edge, firstNode, secondNode);
					}
				}
			}
			coClickReader.close();
			//
			//
			String w_w_AttributeFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_Attribute.txt";
			BufferedWriter wwWriter = IOText.getBufferedWriter_UTF8(w_w_AttributeFile);
			//
			for(WordEdge wEdge: this.W_W_Graph.getEdges()){
				Pair<LogNode> pair = this.W_W_Graph.getEndpoints(wEdge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				int [] attributes = wEdge.getCoArray();
				//
				wwWriter.write(firstNode.getID()+TabSeparator+secondNode.getID());
				//
				for(int i=0; i<attributes.length; i++){
					wwWriter.write(":"+Integer.toString(attributes[i]));
				}
				wwWriter.newLine();
			}
			wwWriter.flush();
			wwWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	//
	private static void loadW_W_AttributeGraph(LogVersion version){
		ini_W_W_GraphNodes(version);
		//
		try{
			String w_w_AttributeFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_Attribute.txt";
			BufferedReader wwReader = IOText.getBufferedReader_UTF8(w_w_AttributeFile);
			//
			String line;
			String[] array;
			//
			while(null != (line=wwReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Word);
					//
					String [] parts = array[1].split(":");
					LogNode secondNode = new LogNode(parts[0], LogNode.NodeType.Word);
					//
					WordEdge edge = new WordEdge();
					edge.upAttributeCount(WordEdge.WCoType.CoParent, Integer.parseInt(parts[1]));
					edge.upAttributeCount(WordEdge.WCoType.CoSession, Integer.parseInt(parts[2]));
					edge.upAttributeCount(WordEdge.WCoType.CoClick, Integer.parseInt(parts[3]));
					//
					W_W_Graph.addEdge(edge, firstNode, secondNode);
				}
			}
			wwReader.close();			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Interface for w-w attributes
	 * **/
	public static int [] getCoInfoOfWToW(LogVersion version, String word_1, String word_2){
		Integer wID_1 = getWordID(version, word_1);
		Integer wID_2 = getWordID(version, word_2);
		if(null!=wID_1 && null!=wID_2 && wID_1!=wID_2){
			if(null == W_W_Graph){
				loadW_W_AttributeGraph(version);
			}
			//
			LogNode firstNode = new LogNode(wID_1.toString(), LogNode.NodeType.Word);
			LogNode secondNode = new LogNode(wID_2.toString(), LogNode.NodeType.Word);
			WordEdge edge = W_W_Graph.findEdge(firstNode, secondNode);
			if(null != edge){
				return edge.getCoArray();
			}
		}
		return null;
	}
	
	
	public static void main(String []args){
		//ClickThroughAnalyzer clickThroughAnalyzer = new ClickThroughAnalyzer();
		
		//1 get the distinct element per unit file from the whole query log
		///*
		//ClickThroughAnalyzer.getUniqueElementsPerUnit(LogVersion.AOL);
		//ClickThroughAnalyzer.getUniqueElementsPerUnit(LogVersion.SogouQ2008);
		//*/
		
		//2 get the distinct elements at the level of the whole query log
		///*
		//ClickThroughAnalyzer.getUniqueElementsForAll(LogVersion.AOL);
		//ClickThroughAnalyzer.getUniqueElementsForAll(LogVersion.SogouQ2008);
		//*/
		
		//3 convert to digital format
		//ClickThroughAnalyzer.convertToDigitalUnitClickThrough(LogVersion.AOL);
		//ClickThroughAnalyzer.convertToDigitalUnitClickThrough(LogVersion.SogouQ2008);
		
		//4 generate query-level co-session, co-click files
		//ClickThroughAnalyzer.generateFiles_QQCoSessioin_QQCoClick_QDGraph(LogVersion.AOL);
		//ClickThroughAnalyzer.generateFiles_QQCoSessioin_QQCoClick_QDGraph(LogVersion.SogouQ2008);
		
		//4.5 only for aol
		//ClickThroughAnalyzer.mergeUnitGraph_QQCoSession_QQCoClick_DQ(LogVersion.AOL);
		
		//5 generate query-level attribute file
		//ClickThroughAnalyzer.generateFiles_QQAttributeGraph(LogVersion.SogouQ2008);
		
		//6 parsing queries into fine-grained granularity: words
		//ClickThroughAnalyzer.parsingQueriesToWords(LogVersion.SogouQ2008);
		
		System.out.println("test!");
		
	}
}
