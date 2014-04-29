package org.archive.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import org.archive.nlp.tokenizer.Tokenizer;
import org.archive.structure.ClickTime;
import org.archive.structure.Record;
import org.archive.structure.AOLRecord;
import org.archive.structure.SogouQRecord2008;
import org.archive.structure.SogouQRecord2012;
import org.archive.util.Language.Lang;
import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;
import org.archive.util.tuple.IntStrInt;
import org.archive.util.tuple.StrInt;
import org.archive.util.tuple.StrStrEdge;
import org.archive.util.tuple.StrStrInt;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;


public class ClickThroughAnalyzer {
	private static boolean DEBUG = true;
	//
	public static final int STARTID = 1;
	public static final String TabSeparator = "\t";
	//for session segmentation, i.e., 30 minutes
	private static final int SessionSegmentationThreshold = 30;
	//unique files: AOL, SogouQ2008, SogouQ2012
	//for id access, and id starts from "1"
	private static Hashtable<String, Integer> UniqueQTextTable = null;
	private static Hashtable<String, Integer> UniqueUserIDTable = null;
	private static Hashtable<String, Integer> UniqueClickUrlTable = null;
	private static Hashtable<String, Integer> UniqueWordTable = null;
	//for accessing multiple fields	
	private static ArrayList<IntStrInt> UniqueQTextList = null;
	private static ArrayList<StrInt> UniqueUserIDList = null;
	private static ArrayList<IntStrInt> UniqueClickUrlList = null;
	private static ArrayList<IntStrInt> UniqueWordList = null;
	
	
	
	
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
		//
		//target file
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		String dir = null;
		String unitFile = null;
		if(LogVersion.AOL == version){			
			dir = DataDirectory.SessionSegmentationRoot+version.toString()+"/";			
			unitFile = dir + version.toString()+"_Sessioned_"+SessionSegmentationThreshold+"_"+unit+".txt";	
		}else if(LogVersion.SogouQ2008 == version){
			dir = DataDirectory.RawDataRoot+DataDirectory.RawData[version.ordinal()];
			unitFile = dir + "access_log.200608"+unit+".decode.filter";
		}else if(LogVersion.SogouQ2012 == version){
			dir = DataDirectory.SessionSegmentationRoot+version.toString()+"/";	
			unitFile = dir + version.toString()+"_Sessioned_"+SessionSegmentationThreshold+".txt";
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
							record = new AOLRecord(recordLine, true);	
						}else if(LogVersion.SogouQ2008 == version){
							record = new SogouQRecord2008(unit, recordLine);
						}else if(LogVersion.SogouQ2012 == version){
							record = new SogouQRecord2012(recordLine, true);
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
		String qFile = null, userIDFile = null, urlFile = null;
		if(LogVersion.SogouQ2012 == version){
			qFile = qDir+version.toString()+"_UniqueQuery_All.txt";		
			userIDFile = userIDDir+version.toString()+"_UniqueUserID_All.txt";		
			urlFile = urlDir+version.toString()+"_UniqueClickUrl_All.txt";
			//
		}else{
			qFile = qDir+version.toString()+"_UniqueQuery_"+StandardFormat.serialFormat(unitSerial, "00")+".txt";		
			userIDFile = userIDDir+version.toString()+"_UniqueUserID_"+StandardFormat.serialFormat(unitSerial, "00")+".txt";		
			urlFile = urlDir+version.toString()+"_UniqueUrl_"+StandardFormat.serialFormat(unitSerial, "00")+".txt";
			//
		}
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
				queryWriter.write(queryInstance.second+TabSeparator
						+queryInstance.first);
				queryWriter.newLine();
			}			
			queryWriter.flush();
			queryWriter.close();
			//for clicked url
			for(StrInt urlInstance: urlTable.values()){
				urlWriter.write(urlInstance.second+TabSeparator
						+urlInstance.first);
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
		}else if(LogVersion.SogouQ2012 == version){
			getUniqueElementsPerUnit(1, LogVersion.SogouQ2012);
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
		int maxStrID = STARTID;
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
				queryWriter.write(element.getSecond()+TabSeparator
						+element.getFirst());
				queryWriter.newLine();
			}
			queryWriter.flush();
			queryWriter.close();
			//for clickUrl
			urlWriter = IOText.getBufferedWriter_UTF8(outputDir+uniqueClickUrl_AllFile);
			for(StrInt element: uniqueClickUrl_All){
				urlWriter.write(element.getSecond()+TabSeparator
						+element.getFirst());
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
		String uniqueQuery_AllFile = null;
		if(LogVersion.SogouQ2012 == version){
			uniqueQuery_AllFile = "Query/"+version.toString()+"_UniqueQuery_All.txt";
		}else{
			uniqueQuery_AllFile = version.toString()+"_UniqueQuery_All.txt";
		}
		//
		UniqueQTextList = IOText.loadUniqueElements_LineFormat_IntTabStr(dir+uniqueQuery_AllFile, encoding);
		//
		if(null == UniqueQTextList){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	private static void loadUniqueUserID(LogVersion version, String encoding){
		String dir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_All[version.ordinal()];		
		String uniqueUserID_AllFile = null;
		if(LogVersion.SogouQ2012 == version){
			uniqueUserID_AllFile = "UserID/"+version.toString()+"_UniqueUserID_All.txt";
		}else{
			uniqueUserID_AllFile = version.toString()+"_UniqueUserID_All.txt";
		}
		//
		UniqueUserIDList = IOText.loadStrInts_LineFormat_Str(dir+uniqueUserID_AllFile, encoding);
		//
		if(null == UniqueUserIDList){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	private static void loadUniqueClickUrl(LogVersion version, String encoding){
		String dir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_All[version.ordinal()];		
		String uniqueClickUrl_AllFile = version.toString()+"_UniqueClickUrl_All.txt"; 
		if(LogVersion.SogouQ2012 == version){
			uniqueClickUrl_AllFile = "Url/"+version.toString()+"_UniqueClickUrl_All.txt"; 
		}else{
			uniqueClickUrl_AllFile = version.toString()+"_UniqueClickUrl_All.txt"; 
		}
		//
		UniqueClickUrlList = IOText.loadUniqueElements_LineFormat_IntTabStr(dir+uniqueClickUrl_AllFile, encoding);
		//
		if(null == UniqueClickUrlList){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	//
	private static Integer getSessionID(LogVersion version, String userIDStr){		
		if(null == UniqueUserIDTable){
			if(null == UniqueUserIDList){
				loadUniqueUserID(version, "UTF-8");
			}
			//
			UniqueUserIDTable = new Hashtable<String, Integer>();
			for(int id=STARTID; id<=UniqueUserIDList.size(); id++){
				UniqueUserIDTable.put(UniqueUserIDList.get(id-1).getFirst(), id);
			}			
		}
		return UniqueUserIDTable.get(userIDStr);						
	}
	private static Integer getQTextID(LogVersion version, String qText){	
		if(null == UniqueQTextTable){
			if(null==UniqueQTextList){
				loadUniqueQText(version, "UTF-8");
			}
			//
			UniqueQTextTable = new Hashtable<String, Integer>();
			for(int id=STARTID; id<=UniqueQTextList.size(); id++){
				UniqueQTextTable.put(UniqueQTextList.get(id-1).getSecond(), id);
			}
		}		
		//
		return UniqueQTextTable.get(qText);		
	}
	private static Integer getClickUrlID(LogVersion version, String urlStr){
		if(null == UniqueClickUrlTable){
			if(null == UniqueClickUrlList){
				loadUniqueClickUrl(version, "UTF-8");;
			}			
			UniqueClickUrlTable = new Hashtable<String, Integer>();
			for(int id=STARTID; id<=UniqueClickUrlList.size(); id++){
				UniqueClickUrlTable.put(UniqueClickUrlList.get(id-1).getSecond(), id);
			}
		}
		//
		return UniqueClickUrlTable.get(urlStr);							
	}
	private static Integer getWordID(LogVersion version, String wordStr){
		if(null == UniqueWordTable){
			if(null == UniqueWordList){
				loadUniqueWord(version, "UTF-8");
			}
			//
			UniqueWordTable = new Hashtable<String, Integer>();
			for(int id=STARTID; id<=UniqueWordList.size(); id++){
				UniqueWordTable.put(UniqueWordList.get(id-1).getSecond(), id);
			}
		}
		return UniqueWordTable.get(wordStr);
	}
	//
	private static int getUniqueNumberOfQuery(LogVersion version){
		if(null==UniqueQTextList){
			loadUniqueQText(version, "UTF-8");
		}
		return UniqueQTextList.size();		
	}
	private static int getUniqueNumberOfUserID(LogVersion version){
		if(null == UniqueUserIDList){
			loadUniqueUserID(version, "UTF-8");			
		}
		return UniqueUserIDList.size();
	}
	private static int getUniqueNumberOfClickUrl(LogVersion version){
		if(null == UniqueClickUrlList){
			loadUniqueClickUrl(version, "UTF-8");
		}
		return UniqueClickUrlList.size();
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
			convertToDigitalUnitClickThrough(1, version);
		}		
	}
	//
	private static void convertToDigitalUnitClickThrough(int unitSerial, LogVersion version){
		//
		//target file
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		String dir = null;
		String unitFile = null;
		if(LogVersion.AOL == version){
			dir = DataDirectory.SessionSegmentationRoot+version.toString()+"/";			
			unitFile = dir + version.toString()+"_Sessioned_"+SessionSegmentationThreshold+"_"+unit+".txt";				
		}else if(LogVersion.SogouQ2008 == version){
			dir = DataDirectory.RawDataRoot + DataDirectory.RawData[version.ordinal()];
			unitFile = dir + "access_log.200608"+unit+".decode.filter";
		}else if(LogVersion.SogouQ2012 == version){
			dir = DataDirectory.SessionSegmentationRoot+version.toString()+"/";	
			unitFile = dir + version.toString()+"_Sessioned_"+SessionSegmentationThreshold+".txt";
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
			//
			String digitalUnitFileName = null;
			if(LogVersion.SogouQ2012 == version){				
				digitalUnitFileName = outputDir+version.toString()+"_DigitalLog_All.txt";
			}else{
				digitalUnitFileName = outputDir+version.toString()+"_DigitalLog_"+unit+".txt";
			}
			//
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
							aolRecord = new AOLRecord(recordLine, false);
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
						SogouQRecord2012 record2012 = null;
						try {
							record2012 = new SogouQRecord2012(recordLine, false);
						}catch(Exception ee){
							System.out.println("invalid record-line exist in "+unit);
							System.out.println(recordLine);								
							continue;
						}
						//				
						if(null!=record2012 && record2012.validRecord()){
							Integer sessionID = getSessionID(version, record2012.getUserID());						
							Integer qID = getQTextID(version, record2012.getQueryText());
							Integer docID = getClickUrlID(version, record2012.getClickUrl());
							//
							if(null!=sessionID && null!=qID && null!=docID){
								dWriter.write(record2012.getQueryTime()+TabSeparator
										+sessionID+TabSeparator
										+qID+TabSeparator
										+record2012.getItemRank()+TabSeparator
										+record2012.getClickOrder()+TabSeparator
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
	private static Vector<Vector<Record>> loadDigitalUnitClickThroughSessions(int unitSerial, LogVersion version){
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		String inputDir = DataDirectory.DigitalFormatRoot+DataDirectory.DigitalFormat[version.ordinal()];
		//
		String digitalUnitFileName = null;
		if(LogVersion.SogouQ2012 == version){
			digitalUnitFileName = inputDir+version.toString()+"_DigitalLog_All.txt";
		}else{
			digitalUnitFileName = inputDir+version.toString()+"_DigitalLog_"+unit+".txt";
		}		
		//buffering distinct sessions
		Vector<Vector<Record>> digitalUnitClickThroughSessions = new Vector<Vector<Record>>();
		Hashtable<String, Integer> sessionTable = new Hashtable<String, Integer>();		
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
						record = new AOLRecord(line, true);	
					}else if(LogVersion.SogouQ2008 == version){
						//digital construct
						record = new SogouQRecord2008(line);
					}else if(LogVersion.SogouQ2012 == version){
						//using digital construct
						record = new SogouQRecord2012(line, true);
					}else{
						new Exception("Version Error!").printStackTrace();
					}				
					//
					if(null != record){
						String userID = record.getUserID();
						if(sessionTable.containsKey(userID)){
							digitalUnitClickThroughSessions.get(sessionTable.get(userID)).add(record);
						}else{
							//new id
							sessionTable.put(userID, digitalUnitClickThroughSessions.size());
							//
							Vector<Record> drVec = new Vector<Record>();
							drVec.add(record);
							digitalUnitClickThroughSessions.add(drVec);
						}						
					}else{
						System.out.println("Null DigitalRecord!");
					}
				}				
				reader.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		//
		return digitalUnitClickThroughSessions;				
	}
	///////////////////////////
	//query-level 
	///////////////////////////
	private static void ini_Q_D_GraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int id=STARTID; id<=queryNodeNumber; id++){			
			Q_D_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Query));
		}
		//
		int docNodeNumber = getUniqueNumberOfClickUrl(version);
		for(int id=STARTID; id<=docNodeNumber; id++){
			Q_D_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Doc));
		}
	}
	//for aol due to day by day operation
	private static void refresh_D_Q_GraphNodes(LogVersion version){
		Q_D_Graph = null;
		Q_D_Graph = new UndirectedSparseGraph<LogNode, LogEdge>(); 
		//
		ini_Q_D_GraphNodes(version);
	}
	private static void ini_Q_Q_CoSessionGraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int id=STARTID; id<=queryNodeNumber; id++){
			Q_Q_CoSession_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Query));
		}
	}
	//for aol due to day by day operation
	private static void refresh_Q_Q_CoSessionGraphNodes(LogVersion version){
		Q_Q_CoSession_Graph = null;
		Q_Q_CoSession_Graph = new UndirectedSparseGraph<LogNode, LogEdge>(); 
		//
		ini_Q_Q_CoSessionGraphNodes(version);
	}
	//
	private static void ini_QQAttributeGraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int id=STARTID; id<=queryNodeNumber; id++){
			Q_Q_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Query));
		}
	}
		
	
	//
	private static void generateUnmergedFiles_QQCoSession_QD(LogVersion version, int fromDay, int toDay){		
		String outputDir = DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()];				
		//
		try{
			File outputDirFile = new File(outputDir);
			if(!outputDirFile.exists()){
				outputDirFile.mkdirs();
			}
			String unmergedQQCoSessionFile = outputDir+"Query_Query_CoSession_Unmerged.txt";	
			//order specified
			String unmergedDQGraphFile = outputDir+"Query_Doc_1_OrderGraph_Unmerged.txt";
			//
			BufferedWriter qqCoSessionWriter = IOText.getBufferedWriter_UTF8(unmergedQQCoSessionFile);
			BufferedWriter qdWriter = IOText.getBufferedWriter_UTF8(unmergedDQGraphFile);
			//
			Vector<Vector<Record>> digitalUnitClickThroughSessions = null;
			//
			for(int k=fromDay; k<=toDay; k++){
				digitalUnitClickThroughSessions = loadDigitalUnitClickThroughSessions(k, version);
				//
				int count = 1;
				for(Vector<Record> sessionRecords: digitalUnitClickThroughSessions){
					count++;
					if(count%100000 == 0){
						System.out.println((count));
					}				
					//session-range queries
					HashSet<String> sessionQSet = new HashSet<String>();					
					for(Record record: sessionRecords){
						//distinct queries in a session
						if(!sessionQSet.contains(record.getQueryText())){
							sessionQSet.add(record.getQueryText());
						}
						//1 query - clicked document
						if(null != record.getClickUrl()){
							//query-document order, the frequency is default 1
							qdWriter.write(record.getQueryText()+TabSeparator+record.getClickUrl());
							qdWriter.newLine();
						}
					}
					//
					String [] coSessionQArray = sessionQSet.toArray(new String[1]);
					for(int i=0; i<coSessionQArray.length-1; i++){
						String q_i = coSessionQArray[i];						
						for(int j=i+1; j<coSessionQArray.length; j++){
							String q_j = coSessionQArray[j];
							qqCoSessionWriter.write(q_i+TabSeparator+q_j);	
							qqCoSessionWriter.newLine();
						}
					}
				}
				//
				qdWriter.flush();
				qqCoSessionWriter.flush();
			}
			//
			qdWriter.flush();
			qdWriter.close();
			//
			qqCoSessionWriter.flush();
			qqCoSessionWriter.close();			
		}catch(Exception e){
			e.printStackTrace();
		}				
	}
	//0
	public static void generateUnmergedFiles_QQCoSession_QDGraph(LogVersion version){
		
		if(LogVersion.AOL == version){			
			generateUnmergedFiles_QQCoSession_QD(version, 1, 10);		
		}else if(LogVersion.SogouQ2008 == version){			
			generateUnmergedFiles_QQCoSession_QD(version, 1, 31);			
		}else if(LogVersion.SogouQ2012 == version){			
			generateUnmergedFiles_QQCoSession_QD(version, 1, 1);			
		}else{
			new Exception("Version Error!").printStackTrace();
		}
	}
	//1
	public static void generateFilesByMerging_QQCoSessioin(LogVersion version){
		ini_Q_Q_CoSessionGraphNodes(version);		
		//
		String intputDir = DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()];				
		//
		try{			
			//co-session
			String unmergedQQCoSessionFile = intputDir+"Query_Query_CoSession_Unmerged.txt";
			System.out.println("loading ...\t"+unmergedQQCoSessionFile);
			BufferedReader unmergedQQCoSessionReader = IOText.getBufferedReader_UTF8(unmergedQQCoSessionFile);
			//
			String line;
			String[] array;
			int count = 1;
			while(null != (line=unmergedQQCoSessionReader.readLine())){
				if(count%100000 == 0){
					System.out.println(count);
				}
				count++;
				if(line.length() > 0){
					array = line.split(TabSeparator);
					//
					LogNode headNode = new LogNode(array[0], LogNode.NodeType.Query);
					LogNode tailNode = new LogNode(array[1], LogNode.NodeType.Query);
					LogEdge qqEdge = Q_Q_CoSession_Graph.findEdge(headNode, tailNode);
					if(null == qqEdge){
						qqEdge = new LogEdge(LogEdge.EdgeType.QQ);
						Q_Q_CoSession_Graph.addEdge(qqEdge, headNode, tailNode);
					}else{
						qqEdge.upCount();
					}					
				}
			}
			unmergedQQCoSessionReader.close();
			//output			
			String qqCoSessionFile = intputDir+"Query_Query_CoSession.txt";			
			BufferedWriter qqCoSessionWriter = IOText.getBufferedWriter_UTF8(qqCoSessionFile);			
			//
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
			//++			
			qqCoSessionWriter.flush();
			qqCoSessionWriter.close();			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//2
	public static void generateFilesByMerging_QDGraph_QQCoClick(LogVersion version){
		ini_Q_D_GraphNodes(version);
		String intputDir = DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()];				
		//
		try{
			//order specified
			String unmergedQDGraphFile = intputDir+"Query_Doc_1_OrderGraph_Unmerged.txt";	
			System.out.println("loading ... "+unmergedQDGraphFile);
			BufferedReader unmergedQDGraphReader = IOText.getBufferedReader_UTF8(unmergedQDGraphFile);
			//
			String line;
			String[] array;
			while(null != (line=unmergedQDGraphReader.readLine())){
				if(line.length() > 0){
					array = line.split(TabSeparator);
					//
					LogNode qNode = new LogNode(array[0], LogNode.NodeType.Query);
					LogNode docNode = new LogNode(array[1], LogNode.NodeType.Doc);
					//
					LogEdge dqEdge = Q_D_Graph.findEdge(qNode, docNode);
					if(null==dqEdge){
						dqEdge = new LogEdge(LogEdge.EdgeType.QDoc);
						Q_D_Graph.addEdge(dqEdge, qNode, docNode);
					}else{
						dqEdge.upCount();
					}
				}
			}
			unmergedQDGraphReader.close();
			//
			String qqCoClickFile = intputDir+"Query_Query_CoClick.txt";	
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
			//
			qqCoClickWriter.flush();
			qqCoClickWriter.close();				
			//d-q file
			String qdGraphFile = intputDir+"Query_Doc_Graph.txt";
			BufferedWriter qdWriter = IOText.getBufferedWriter_UTF8(qdGraphFile);
			for(LogEdge edge: Q_D_Graph.getEdges()){
				Pair<LogNode> pair = Q_D_Graph.getEndpoints(edge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				//put query at the first position
				if(firstNode.getType() == LogNode.NodeType.Query){
					qdWriter.write(firstNode.getID()+TabSeparator
							+secondNode.getID()+TabSeparator
							+edge.getCount());
					qdWriter.newLine();
				}else{
					qdWriter.write(secondNode.getID()+TabSeparator
							+firstNode.getID()+TabSeparator
							+edge.getCount());
					qdWriter.newLine();
				}
			}
			//			
			qdWriter.flush();
			qdWriter.close();	
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//3
	private static void generateFile_QQAttributeGraph(LogVersion version){
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
	public static void load_QQAttributeGraph(LogVersion version){
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
	private static void loadUniqueWord(LogVersion version, String encoding){		
		String uniqueWordFile = 
			DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"UniqueWord.txt";
		//
		UniqueWordList = IOText.loadUniqueElements_LineFormat_IntTabStr(uniqueWordFile, encoding);
		//
		if(null == UniqueWordList){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	private static int getUniqueNumberOfWord(LogVersion version){
		if(null == UniqueWordList){
			loadUniqueWord(version, "UTF-8");			
		}
		//
		return UniqueWordList.size();
	}
	private static void ini_W_W_GraphNodes(LogVersion version){
		int wordNodeNumber = getUniqueNumberOfWord(version);
		for(int id=STARTID; id<=wordNodeNumber; id++){
			W_W_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Word));
		}
	}
	private static void ini_Q_W_GraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int id=STARTID; id<=queryNodeNumber; id++){
			Q_W_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Query));
		}
		//
		int wordNodeNumber = getUniqueNumberOfWord(version);
		for(int id=STARTID; id<=wordNodeNumber; id++){
			Q_W_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Word));
		}
	}
	private static void ini_Q_Q_CoClickGraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int id=STARTID; id<=queryNodeNumber; id++){
			CoClick_Q_Q_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Query));
		}
	}
	//not quantified edge
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
						LogNode tNode = new LogNode(array[1], LogNode.NodeType.Query);
						int fre = Integer.parseInt(array[2]);
						//						
						LogEdge edge = Q_Q_CoSession_Graph.findEdge(sNode, tNode);
						if(edge == null){
							edge = new LogEdge(LogEdge.EdgeType.QQ);
							edge.setCount(fre);
							Q_Q_CoSession_Graph.addEdge(edge, sNode, tNode);
						}else{
							edge.upCount(fre);
						}						
					}
				}
			}
			reader.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
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
	
	//0
	public static void parsingQueriesToWords(LogVersion version){		
		loadUniqueQText(version, "UTF-8");		
		//
		String uniqueWordFile = 
			DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"UniqueWord.txt";
    	String wordToSourceQFile = 
    		DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_SourceQueries.txt";
    	String queryToMemberWordsFile = 
    		DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_MemberWords.txt";
		//
    	Hashtable<String, Integer> wordTable = new Hashtable<String, Integer>();
    	Vector<UserWord> wordNodeVec = new Vector<UserWord>();
    	//    	
    	ArrayList<String> words;
		//
    	try {
    		//query to member words
    		BufferedWriter queryToMemberWordsWriter = IOText.getBufferedWriter_UTF8(queryToMemberWordsFile);
    		//
    		for(IntStrInt ununiqueQuery: UniqueQTextList){    			
    			if(LogVersion.AOL == version){
    				words = Tokenizer.qSegment(ununiqueQuery.getSecond(), Lang.English);
    			}else{
    				words = Tokenizer.qSegment(ununiqueQuery.getSecond(), Lang.Chinese);
    			}
    			//
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
    						UserWord wNode = wordNodeVec.get(id);    						
    						wNode.addFre(ununiqueQuery.getThird());
    						wNode.addSourceQ(ununiqueQuery.getFirst());
    					}else{    						
    						wordTable.put(w, wordNodeVec.size());
    						//
    						UserWord wNode = new UserWord(w, ununiqueQuery.getThird(), ununiqueQuery.getFirst());
    						wordNodeVec.add(wNode);
    					}
    				}
    				//record q to words
    				queryToMemberWordsWriter.write(Integer.toString(ununiqueQuery.getFirst()));
    				for(Iterator<String> itr=wordSet.iterator(); itr.hasNext();){
    					String w = itr.next();
    					int wid = wordTable.get(w);
    					queryToMemberWordsWriter.write(TabSeparator+Integer.toString(wid+1));        					
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
    		for(int id=1; id<=wordNodeVec.size(); id++){
    			wNode = wordNodeVec.get(id-1);
    			sourceQList = wNode.sourceQList;
    			//to word small text
    			uniqueWordsWriter.write(wNode.logFre+TabSeparator+wNode.word);
    			uniqueWordsWriter.newLine();
    			//to word-query small text
    			//wordToQuerySmallTextWriter.write(Integer.toString(i));
    			for(Integer sQID: sourceQList){
    				wordToSourceQWriter.write(Integer.toString(id)+TabSeparator+sQID.toString());
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
	//1	
	private static void generateUnmergedFile_WordCoParent(LogVersion version){
		if(null == UniqueQTextList){
			loadUniqueQText(version, "UTF-8");
		}
		//
		loadQ_W_Graph(version);
		//co-parent
		try{
			String wwCoParentFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoParent_Unmerged.txt";
			BufferedWriter wwCoParentWriter = IOText.getBufferedWriter_UTF8(wwCoParentFile);
			//			
			for(IntStrInt uniqueQ: UniqueQTextList){				
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
	//2
	private static void generateUnmergedFile_WordCoSession(LogVersion version){
		//query co-session
		loadQ_Q_CoSessionGraph(version);
		//query with its consisting words
		loadQ_W_Graph(version);		
		//
		try{
			String w_w_CoSessionFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoSession_Unmerged.txt";
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
	//3
	private static void generateUnmergedFile_WordCoClick(LogVersion version){		
		//
		loadQ_W_Graph(version);		
		loadCoClick_Q_Q_Graph(version);
		//
		String w_w_CoClickFile = 
			DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoClick_Unmerged.txt";
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
									+sWNode.getID()+TabSeparator
									+Integer.toString(edge.getCount()));
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
	//4	
	public static void generateFile_WWAttributeGraph(LogVersion version){
		ini_W_W_GraphNodes(version);
		//
		String line;
		String[] array;		
		try{
			String wwCoParentFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoParent_Unmerged.txt";
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
						WordEdge edge = W_W_Graph.findEdge(firstNode, secondNode);
						if(null != edge){
							edge.upAttributeCount(WordEdge.WCoType.CoParent, fre);
						}else{
							edge = new WordEdge();
							edge.upAttributeCount(WordEdge.WCoType.CoParent, fre);
							W_W_Graph.addEdge(edge, firstNode, secondNode);
						}						
					}
				}
			}
			coParentReader.close();
			//co-session
			String w_w_CoSessionFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoSession_Unmerged.txt";
			BufferedReader coSessionReader = IOText.getBufferedReader_UTF8(w_w_CoSessionFile);
			//			
			while(null != (line=coSessionReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Word);
					LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Word);
					int fre = Integer.parseInt(array[2]);
					//
					WordEdge edge = W_W_Graph.findEdge(firstNode, secondNode);
					if(null != edge){
						edge.upAttributeCount(WordEdge.WCoType.CoSession, fre);
					}else{
						edge = new WordEdge();
						edge.upAttributeCount(WordEdge.WCoType.CoSession, fre);
						W_W_Graph.addEdge(edge, firstNode, secondNode);
					}
				}
			}
			coSessionReader.close();
			//co-click
			String w_w_CoClickFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoClick_Unmerged.txt";
			BufferedReader coClickReader = IOText.getBufferedReader_UTF8(w_w_CoClickFile);
			//
			while(null != (line=coClickReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Word);
					LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Word);
					int fre = Integer.parseInt(array[2]);
					WordEdge edge = W_W_Graph.findEdge(firstNode, secondNode);
					if(null!=edge){
						edge.upAttributeCount(WordEdge.WCoType.CoClick, fre);
					}else{
						edge = new WordEdge();
						edge.upAttributeCount(WordEdge.WCoType.CoClick, fre);
						W_W_Graph.addEdge(edge, firstNode, secondNode);
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
			for(WordEdge wEdge: W_W_Graph.getEdges()){
				Pair<LogNode> pair = W_W_Graph.getEndpoints(wEdge);
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
	private static void load_WWAttributeGraph(LogVersion version){
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
				load_WWAttributeGraph(version);
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
	
	
	
	//////////////////////////////////
	//PreProcess: session segmentation
	//(1): AOL clickthrough: time threshold 30 min;
	//(2): SogouQ2008: the same cookie id in a day;
	//(3): SogouQ2012: the same cookie id in a day;
	//////////////////////////////////
	/** analysis for SogouQ2012 session identification
	//用户ID是根据用户使用浏览器访问搜索引擎时的Cookie信息自动赋值，即同一次使用浏览器输入的不同查询对应同一个用户ID
	20111230000009	96994a0480e7e1edcaef67b20d8816b7	伟大导演	1	1	http://movie.douban.com/review/1128960/
	20111230000135	96994a0480e7e1edcaef67b20d8816b7	伟大导演	2	2	http://www.mtime.com/news/2009/02/20/1404845.html
	20111230000149	96994a0480e7e1edcaef67b20d8816b7	伟大导演	5	3	http://i.mtime.com/1449171/blog/4297703/
	20111230000439	96994a0480e7e1edcaef67b20d8816b7	伟大导演	9	4	http://news.xinhuanet.com/newmedia/2007-08/14/content_6527307.htm
	//
	 * **/
	
	//get ordered clickthrough SogouQ
	//sogouQ2008 in descending order by click order
	//sogouQ2012 in descending order by query time
	private static void getOrderedSogouQ2012(){
		//input file		
		String inputDir = DataDirectory.RawDataRoot+DataDirectory.RawData[LogVersion.SogouQ2012.ordinal()];
		String unitFile = inputDir + "querylog";			
		//recordMap of one unit file
		HashMap<String, Vector<SogouQRecord2012>> recordMap = new HashMap<String, Vector<SogouQRecord2012>>();
		//
		try{		
			//input
    		File file = new File(unitFile);
			if(file.exists()){	
				System.out.println("loading...\t"+unitFile);
				BufferedReader reader = IOText.getBufferedReader(unitFile, "GBK");
				//
				String recordLine = null;				
				SogouQRecord2012 record = null;				
				while(null!=(recordLine=reader.readLine())){
					//System.out.println(count++);					
					try{							
						record = new SogouQRecord2012(recordLine, false);
					}catch(Exception ee){
						System.out.println("invalid record-line exist!");
						System.out.println(recordLine);
						System.out.println();
						recordLine=null;
						record=null;
						continue;
					}
					//
					if(null!=record && record.validRecord()){
						if(recordMap.containsKey(record.getUserID())){
							recordMap.get(record.getUserID()).add(record);
						}else{
							Vector<SogouQRecord2012> recordVec = new Vector<SogouQRecord2012>();
							recordVec.add(record);
							recordMap.put(record.getUserID(), recordVec);
						}
					}																
				}
				reader.close();
				reader=null;				
			}
			//sort and output
			String outputDir = DataDirectory.OrderedSogouQRoot+"SogouQ2012/";
			File dirFile = new File(outputDir);
			if(!dirFile.exists()){
				dirFile.mkdirs();
			}
			String targetFile = outputDir + "SogouQ2012_Ordered_UTF8.txt";
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(targetFile);
			//
			for(Entry<String, Vector<SogouQRecord2012>> entry: recordMap.entrySet()){				
				//
				Vector<SogouQRecord2012> recordVec = entry.getValue();
				java.util.Collections.sort(recordVec);
				//no specific session segmentation, just for the same user
				for(SogouQRecord2012 r: recordVec){
					//
					writer.write(r.getQueryTime()+TabSeparator
							+r.getUserID()+TabSeparator
							+r.getQueryText()+TabSeparator
							+r.getItemRank()+TabSeparator
							+r.getClickOrder()+TabSeparator
							+r.getClickUrl());
					//
					writer.newLine();
				}			
			}
			//
			writer.flush();
			writer.close();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	private static void getOrderedSogouQ2008(int unitSerial){
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		//sort and output
		String outputDir = DataDirectory.OrderedSogouQRoot+LogVersion.SogouQ2008.toString()+"/";
		File dirFile = new File(outputDir);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}
		//
		String targetFile = outputDir + LogVersion.SogouQ2008.toString() +"_Ordered_UTF8_"+unit+".txt";
			
		//input				
		String inputDir = DataDirectory.RawDataRoot+DataDirectory.RawData[LogVersion.SogouQ2008.ordinal()];
		String unitFile = inputDir + "access_log.200608"+unit+".decode.filter";									
		//recordMap of one unit file
		HashMap<String, Vector<SogouQRecord2008>> recordMap = new HashMap<String, Vector<SogouQRecord2008>>();
		//
		try{		
			//input
    		File file = new File(unitFile);
			if(file.exists()){	
				System.out.println("loading...\t"+unitFile);
				BufferedReader reader = IOText.getBufferedReader(unitFile, "GBK");
				//
				String recordLine = null;				
				SogouQRecord2008 record = null;				
				while(null!=(recordLine=reader.readLine())){
					//System.out.println(count++);					
					try{							
						record = new SogouQRecord2008(unit, recordLine);
					}catch(Exception ee){
						System.out.println("invalid record-line exist!");
						System.out.println(recordLine);
						System.out.println();
						recordLine=null;
						record=null;
						continue;
					}
					//
					if(null!=record && record.validRecord()){
						if(recordMap.containsKey(record.getUserID())){
							recordMap.get(record.getUserID()).add(record);
						}else{
							Vector<SogouQRecord2008> recordVec = new Vector<SogouQRecord2008>();
							recordVec.add(record);
							recordMap.put(record.getUserID(), recordVec);
						}
					}																
				}
				reader.close();
				reader=null;				
			}
			//
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(targetFile);
			//
			for(Entry<String, Vector<SogouQRecord2008>> entry: recordMap.entrySet()){				
				//
				Vector<SogouQRecord2008> recordVec = entry.getValue();
				java.util.Collections.sort(recordVec);
				//no specific session segmentation, just for the same user
				for(SogouQRecord2008 r: recordVec){
					//
					writer.write(r.getUserID()+TabSeparator
							+r.getQueryText()+TabSeparator
							+r.getItemRank()+TabSeparator
							+r.getClickOrder()+TabSeparator
							+r.getClickUrl());
					//
					writer.newLine();
				}			
			}
			//
			writer.flush();
			writer.close();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	//
	public static void getOrderedSogouQ(LogVersion version){
		if(LogVersion.SogouQ2008 == version){
			for(int i=1; i<=31; i++){
				getOrderedSogouQ2008(i);
			}
		}else if(LogVersion.SogouQ2012 == version){
			getOrderedSogouQ2012();
		}		
	}
	
	//////////////////////////////////////
	//perform simple session segmentation
	//////////////////////////////////////
	public static void performSessionSegmentation(LogVersion version){
		if(LogVersion.AOL == version){
			for(int i=1; i<=10; i++){
				segmentSessions(i, version);				
			}
		}else if(LogVersion.SogouQ2012 == version){
			segmentSessions(1, version);
		}else{
			new Exception("Version error!").printStackTrace();
		}
	}
	//
	private static void segmentSessions(int unitSerial, LogVersion version){		
		//input file
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		String inputFile = null;
		if(LogVersion.AOL == version){
			String dir = DataDirectory.RawDataRoot+DataDirectory.RawData[version.ordinal()];
			inputFile = dir + "user-ct-test-collection-"+unit+".txt";
		}else if (LogVersion.SogouQ2012 == version) {
			String dir = DataDirectory.OrderedSogouQRoot+"SogouQ2012/";			
			inputFile = dir + "SogouQ2012_Ordered_UTF8.txt";
		}
		//
		try{	
			//output
			String outputDir = DataDirectory.SessionSegmentationRoot+version.toString()+"/";
			File outputDirFile = new File(outputDir);
			if(!outputDirFile.exists()){
				outputDirFile.mkdirs();
			}
			String outputFile = null;
			if(LogVersion.AOL == version){
				outputFile = outputDir + version.toString()+"_Sessioned_"+SessionSegmentationThreshold+"_"+unit+".txt";
			}else if(LogVersion.SogouQ2012 == version){
				outputFile = outputDir + version.toString()+"_Sessioned_"+SessionSegmentationThreshold+".txt";
			}
			//
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(outputFile);
			//
    		File file = new File(inputFile);
			if(file.exists()){	
				System.out.println("loading...\t"+inputFile);
				BufferedReader reader = IOText.getBufferedReader(inputFile, "GBK");				
				String recordLine = null;
				
				//
				if(LogVersion.AOL == version){
					//
					AOLRecord formerRecord = null, newRecord = null;
					Date referenceDate = null;
					//overlook the first line, which is attribute names
					reader.readLine();
					//first record
					int sessionID = STARTID;
					recordLine = reader.readLine();
					formerRecord = new AOLRecord(recordLine, false);
					referenceDate = formerRecord.getDateQueryTime();
					if(!formerRecord.hasClickEvent()){
						//
						writer.write(unit+"-"+formerRecord.getUserID()+"-"+sessionID+TabSeparator
								+formerRecord.getQueryText()+TabSeparator
								+formerRecord.getQueryTime());
						//
						writer.newLine();
					}else{
						//
						writer.write(unit+"-"+formerRecord.getUserID()+"-"+sessionID+TabSeparator
								+formerRecord.getQueryText()+TabSeparator
								+formerRecord.getQueryTime()+TabSeparator
								+formerRecord.getItemRank()+TabSeparator
								+formerRecord.getClickUrl());
						//
						writer.newLine();
					}					
					//
					while(null!=(recordLine=reader.readLine())){
						newRecord = new AOLRecord(recordLine, false);
						try {
							if(!newRecord.getUserID().equals(formerRecord.getUserID())){
								sessionID = STARTID;
								if(newRecord.hasClickEvent()){
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime()+TabSeparator
											+newRecord.getItemRank()+TabSeparator
											+newRecord.getClickUrl());
									//
									writer.newLine();
								}else{								
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime());
									//
									writer.newLine();
								}
								//
								formerRecord = newRecord;
								referenceDate = newRecord.getDateQueryTime();
							}else if(ClickTime.getTimeSpan_MM(referenceDate, newRecord.getDateQueryTime()) 
									<= SessionSegmentationThreshold){
								//same session
								if(newRecord.hasClickEvent()){
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime()+TabSeparator
											+newRecord.getItemRank()+TabSeparator
											+newRecord.getClickUrl());								
									writer.newLine();
								}else{								
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime());								
									writer.newLine();
								}							
							}else{
								//same user id, but another session
								sessionID++;
								//
								if(newRecord.hasClickEvent()){
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime()+TabSeparator
											+newRecord.getItemRank()+TabSeparator
											+newRecord.getClickUrl());								
									writer.newLine();
								}else{								
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime());								
									writer.newLine();
								}
								//
								referenceDate = newRecord.getDateQueryTime();							
							}
						} catch (Exception e) {
							// TODO: handle exception
							System.out.println(recordLine);
						}																									
					}
					//over
				}else if(LogVersion.SogouQ2012 == version){
					//
					SogouQRecord2012 formerRecord = null, newRecord = null;
					Date referenceDate = null;					
					//first record
					int sessionID = STARTID;
					recordLine = reader.readLine();
					formerRecord = new SogouQRecord2012(recordLine, false);
					referenceDate = formerRecord.getDateQueryTime();
					if(formerRecord.validRecord()){						
						writer.write(formerRecord.getQueryTime()+TabSeparator
								+formerRecord.getUserID()+"-"+sessionID+TabSeparator
								+formerRecord.getQueryText()+TabSeparator
								+formerRecord.getItemRank()+TabSeparator
								+formerRecord.getClickOrder()+TabSeparator
								+formerRecord.getClickUrl());						
						writer.newLine();
					}				
					//
					while(null!=(recordLine=reader.readLine())){
						newRecord = new SogouQRecord2012(recordLine, false);
						//						
						if(!newRecord.getUserID().equals(formerRecord.getUserID())){
							sessionID = STARTID;
							//
							if(newRecord.validRecord()){						
								writer.write(newRecord.getQueryTime()+TabSeparator
										+newRecord.getUserID()+"-"+sessionID+TabSeparator
										+newRecord.getQueryText()+TabSeparator
										+newRecord.getItemRank()+TabSeparator
										+newRecord.getClickOrder()+TabSeparator
										+newRecord.getClickUrl());						
								writer.newLine();
							}
							//
							formerRecord = newRecord;
							referenceDate = newRecord.getDateQueryTime();
						}else if(ClickTime.getTimeSpan_MM(referenceDate, newRecord.getDateQueryTime()) 
								<= SessionSegmentationThreshold){
							//same session
							if(newRecord.validRecord()){						
								writer.write(newRecord.getQueryTime()+TabSeparator
										+newRecord.getUserID()+"-"+sessionID+TabSeparator
										+newRecord.getQueryText()+TabSeparator
										+newRecord.getItemRank()+TabSeparator
										+newRecord.getClickOrder()+TabSeparator
										+newRecord.getClickUrl());						
								writer.newLine();
							}						
						}else{
							//same user id, but another session
							sessionID++;
							//
							if(newRecord.validRecord()){						
								writer.write(newRecord.getQueryTime()+TabSeparator
										+newRecord.getUserID()+"-"+sessionID+TabSeparator
										+newRecord.getQueryText()+TabSeparator
										+newRecord.getItemRank()+TabSeparator
										+newRecord.getClickOrder()+TabSeparator
										+newRecord.getClickUrl());						
								writer.newLine();
							}
							//
							referenceDate = newRecord.getDateQueryTime();							
						}																											
					}
				}
				//
				reader.close();
				reader=null;	
				//
				writer.flush();
				writer.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	
	public static void main(String []args){
		/** get ordered files **/
		//ClickThroughAnalyzer.getOrderedSogouQ(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.getOrderedSogouQ(LogVersion.SogouQ2012);
		
		/** session segmentation **/
		//ClickThroughAnalyzer.performSessionSegmentation(LogVersion.AOL);
		//ClickThroughAnalyzer.performSessionSegmentation(LogVersion.SogouQ2012);
		
		//ClickThroughAnalyzer clickThroughAnalyzer = new ClickThroughAnalyzer();
		
		//1 get the distinct element per unit file from the whole query log
		///*
		//ClickThroughAnalyzer.getUniqueElementsPerUnit(LogVersion.AOL);
		//ClickThroughAnalyzer.getUniqueElementsPerUnit(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.getUniqueElementsPerUnit(LogVersion.SogouQ2012);
		//*/
		
		//2 get the distinct elements at the level of the whole query log
		///*
		//ClickThroughAnalyzer.getUniqueElementsForAll(LogVersion.AOL);
		//ClickThroughAnalyzer.getUniqueElementsForAll(LogVersion.SogouQ2008);
		//no need for sogouQ2012
		//*/
		
		//3 convert to digital format
		//ClickThroughAnalyzer.convertToDigitalUnitClickThrough(LogVersion.AOL);
		//ClickThroughAnalyzer.convertToDigitalUnitClickThrough(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.convertToDigitalUnitClickThrough(LogVersion.SogouQ2012);
		
		//4 generate un-merged files
		//ClickThroughAnalyzer.generateUnmergedFiles_QQCoSession_QDGraph(LogVersion.AOL);
		//ClickThroughAnalyzer.generateUnmergedFiles_QQCoSession_QDGraph(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.generateUnmergedFiles_QQCoSession_QDGraph(LogVersion.SogouQ2012);
		
		//5 QQCoSessioin
		//ClickThroughAnalyzer.generateFilesByMerging_QQCoSessioin(LogVersion.AOL);
		//ClickThroughAnalyzer.generateFilesByMerging_QQCoSessioin(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.generateFilesByMerging_QQCoSessioin(LogVersion.SogouQ2012);
		
		//6 QDGraph_QQCoClick
		ClickThroughAnalyzer.generateFilesByMerging_QDGraph_QQCoClick(LogVersion.AOL);
		//ClickThroughAnalyzer.generateFilesByMerging_QDGraph_QQCoClick(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.generateFilesByMerging_QDGraph_QQCoClick(LogVersion.SogouQ2012);
		
		//7 
		//ClickThroughAnalyzer.generateFiles_QQAttributeGraph(LogVersion.AOL);
		//ClickThroughAnalyzer.generateFiles_QQAttributeGraph(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.generateFiles_QQAttributeGraph(LogVersion.SogouQ2012);
		
		//4 generate query-level co-session, co-click files
		//ClickThroughAnalyzer.generateFiles_QQCoSessioin_QQCoClick_QDGraph(LogVersion.AOL);
		//ClickThroughAnalyzer.generateFiles_QQCoSessioin_QQCoClick_QDGraph(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.generateFiles_QQCoSessioin_QQCoClick_QDGraph(LogVersion.SogouQ2012);
		
		//4.5 only for aol
		//ClickThroughAnalyzer.mergeUnitGraph_QQCoSession_QQCoClick_DQ(LogVersion.AOL);
		
		//5 generate query-level attribute file
		//ClickThroughAnalyzer.generateFiles_QQAttributeGraph(LogVersion.SogouQ2008);
		
		//6 parsing queries into fine-grained granularity: words
		//ClickThroughAnalyzer.parsingQueriesToWords(LogVersion.SogouQ2008);
		
		//System.out.println("test!");
		
	}
}
