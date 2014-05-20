package org.archive.dataset.ntcir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.archive.OutputDirectory;
import org.archive.dataset.DataSetDiretory;
import org.archive.dataset.ntcir.sm.IRAnnotation;
import org.archive.dataset.ntcir.sm.LTPIRAnnotator;
import org.archive.dataset.ntcir.sm.SMSubtopicItem;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.dataset.ntcir.sm.TermIRAnnotator;
import org.archive.dataset.ntcir.sm.PhraseIRAnnotator;
import org.archive.dataset.ntcir.sm.SubtopicInstance;
import org.archive.dataset.ntcir.sm.TaggedTerm;
import org.archive.nlp.chunk.ShallowParser;
import org.archive.nlp.htmlparser.pk.HtmlExtractor;
import org.archive.nlp.lcs.LCSScaner;
import org.archive.nlp.qpunctuation.QueryPreParser;
import org.archive.nlp.tokenizer.Tokenizer;
import org.archive.nlp.tokenizer.ictclas.ICTCLAS2014;
import org.archive.util.DocUtils;
import org.archive.util.FileFinder;
import org.archive.util.Language.Lang;
import org.archive.util.io.IOText;
import org.archive.util.tuple.StrInt;

public class NTCIRLoader {
	
	private final static boolean DEBUG = false;
	
	private static boolean ICTCLAS2014_INI = false;
	public static final String CODE_UTF8 = "UTF-8";
	private static final String CODE_GB2312 = "GB2312";
	private static final String DR_SPLIT = " ";
	private static final String SM_SPLIT = ";";
	
	//
	public static enum NTCIR_EVAL_TASK{NTCIR9_SM, NTCIR9_DR, 
		NTCIR10_SM_EN, NTCIR10_SM_CH, NTCIR10_DR_CH,
		NTCIR11_SM_EN, NTCIR11_SM_CH, NTCIR11_DR_CH, NTCIR11_DR_EN};
	
	//location
	//////////////////
	//top-k-specific
	//////////////////
	
	private final static String TOP_K = "top-100";	
	/////////////////
	//topic
	////////////////
	private final static String NTCIR10_TOPIC = "./dataset/ntcir/ntcir-10/DR/INTENT1and2CtopicsQS.xlsx";	
	//
	private final static String NTCIR11_TOPIC = DataSetDiretory.ROOT+"ntcir/ntcir-11/SM/IMine.Query.txt";
	
	/////////////////
	//NTCIR10 document ranking
	////////////////
	private final static String NTCIR10_DR_BASELINE = "./dataset/ntcir/ntcir-10/DR/BASELINE-D-C-1.txt";	
	private final static String NTCIR10_DR_CH_Iprob = "./dataset/ntcir/ntcir-10/DR/INTENT-2DRC.Iprob";
	private final static String NTCIR10_DR_CH_Dqrels = "./dataset/ntcir/ntcir-10/DR/INTENT-2DRC.Dqrels";
	/////////////////
	//subtopic mining
	////////////////
	private final static String NTCIR10_SM_CH_Iprob = "./dataset/ntcir/ntcir-10/SM/INTENT-2SMC.Iprob";
	private final static String NTCIR10_SM_CH_Dqrels = "./dataset/ntcir/ntcir-10/SM/INTENT-2SMC.rev.Dqrels";
	private final static String NTCIR10_SM_EN_Iprob = "./dataset/ntcir/ntcir-10/SM/INTENT-2SME.Iprob";
	private final static String NTCIR10_SM_EN_Dqrels = "/dataset/ntcir/ntcir-10/SM/INTENT-2SME.rev.Dqrels";
	
	private final static String NTCIR11_SM_RelatedQueries = DataSetDiretory.ROOT+"ntcir/ntcir-11/SM/IMine.RelatedQueries/IMine.RelatedQueries.txt";
	private final static String NTCIR11_SM_QuerySuggestions = DataSetDiretory.ROOT+"ntcir/ntcir-11/SM/IMine.QuerySuggestion/IMine.QuerySuggestion.txt";
	
	/////////////////
	//document
	////////////////
	private final static String NTCIR10_DOC_HTML_DIR = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/doc/"+TOP_K+"/";
	private final static String NTCIR10_DOC_TEXT_BUFFER_DIR = "./buffer/doc/ntcir-10/"+TOP_K+"/c_extracted/";
	private final static String NTCIR10_DOC_SEGMENT_BUFFER_DIR = "./buffer/doc/ntcir-10/"+TOP_K+"/c_segmented/";
	/*
	private final static String TREC_QRELS   = "";
	private final static String QUERY_FILE   = "";
	private final static String ASPECT_FILE  = "";
	*/
	
	//candidate html_parser
	//private static HITHtmlExtractor htmlExtractor = new HITHtmlExtractor();
	private static HtmlExtractor htmlExtractor = new HtmlExtractor();
	
	public static HashMap<String,String> loadNTCIR10Docs(){
		if(!ICTCLAS2014_INI){
			ICTCLAS2014.iniConfig();
			ICTCLAS2014_INI = true;
		}
		
		int bPOSTagged = 0;
		//
		HashMap<String,String> ntcir10Docs = new HashMap<String,String>();
		ArrayList<File> files = FileFinder.GetAllFiles(NTCIR10_DOC_HTML_DIR, "", true);
		//
		String query_id = null;
		String doc_name = null;
		String doc_segmented = null;
		String doc_extracted = null;
		File f_segmented = null;
		File f_extracted = null;
		//
		int nullFileCount = -1;
		ArrayList<String> idList = new ArrayList<String>();		
		try {
			for(File f: files){					
				int index = f.getAbsolutePath().lastIndexOf("\\");
				query_id = f.getAbsolutePath().substring(index-4, index);
				doc_name = f.getAbsolutePath().substring(index+1, f.getAbsolutePath().indexOf("."));
				//System.out.println(f.getAbsolutePath());
				//System.out.println(query_id+"\t"+doc_name);
				if(Integer.parseInt(query_id) <= 100){
					continue;
				}
				if(!idList.contains(query_id)){
					if(nullFileCount > 0){
						if (DEBUG) {
							System.out.println("Null File Count for\t"+idList.get(idList.size()-1)+"\t"+nullFileCount);
							System.out.println();
						}						
					}
					//
					idList.add(query_id);
					if (DEBUG) {
						System.out.println("Running for \t"+query_id);
					}					
					nullFileCount = 0;
				}
								
				///*
				f_segmented = new File(NTCIR10_DOC_SEGMENT_BUFFER_DIR + query_id + "/" +doc_name);
				if(f_segmented.exists()){
					doc_segmented = DocUtils.ReadFile(f_segmented, true);
					if (DEBUG) {
						System.out.println("Load buffered doc_segmented for\t"+doc_name);
					}					
					
					if(doc_segmented.length()>10){
						ntcir10Docs.put(doc_name, doc_segmented);
					}else{
						ntcir10Docs.put(doc_name, null);
						nullFileCount++;
					}
				}else {
					f_extracted = new File(NTCIR10_DOC_TEXT_BUFFER_DIR + query_id + "/" +doc_name);
					if(f_extracted.exists()){
						doc_extracted = DocUtils.ReadFile(f_extracted, true);
						if (DEBUG) {
							System.out.println("Load buffered doc_extracted for\t"+doc_name);
						}						
						//generated segmented file
					}else{
						/*
						if(null == (doc_extracted = htmlExtractor.extractFromHtml(f.getAbsolutePath()))){
							doc_extracted = "0";
						}
						*/
						StringBuffer sBuffer = htmlExtractor.htmlToText(new File(f.getAbsolutePath()), "GB2312");
						if(null == sBuffer){
							doc_extracted = "0";
						}else{
							doc_extracted = sBuffer.toString();
						}
						//new
						File _dir1 = new File(NTCIR10_DOC_TEXT_BUFFER_DIR + query_id + "/");
						if(!_dir1.exists()){
							_dir1.mkdir();
						}
						DocUtils.createNewFile(f_extracted, doc_extracted);
						if (DEBUG) {
							System.out.println("Generate doc_extracted for\t"+doc_name);
						}						
					}
					//
					if(doc_extracted.length() > 10){
						if (DEBUG) {
							System.out.println("Begin segmenting!");
						}						
						if(doc_extracted.length() > 5000){
							doc_extracted = doc_extracted.substring(0, 5000);
						}
						doc_segmented = ICTCLAS2014.CLibrary.Instance.NLPIR_ParagraphProcess(doc_extracted, bPOSTagged);
						if (DEBUG) {
							System.out.println("Finish segmenting!");
						}						
						if(null == doc_segmented){
							doc_segmented = "0";
						}
					}else{
						doc_segmented = "0";
					}					
					//new
					File _dir2 = new File(NTCIR10_DOC_SEGMENT_BUFFER_DIR + query_id + "/");
					if(!_dir2.exists()){
						_dir2.mkdir();
					}
					DocUtils.createNewFile(f_segmented, doc_segmented);
					if (DEBUG) {
						System.out.println("Generate doc_segmented for\t"+doc_name);
					}					
					//
					if(doc_segmented.length()>10){
						ntcir10Docs.put(doc_name, doc_segmented);
					}else{
						ntcir10Docs.put(doc_name, null);
						nullFileCount++;
					}					
				}
				//*/				
			}				
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		//
		ICTCLAS2014.exitConfig();
		//
		return ntcir10Docs;
	}
	
	public static HashMap<String, ArrayList<String>> loadPoolOfNTCIR10DRBaseline() {
		
		int top_k = Integer.parseInt(TOP_K.substring(TOP_K.indexOf("-")+1));
		
		HashMap<String, ArrayList<String>> poolOfBaseline = new HashMap<String, ArrayList<String>>();
		
		try {
			String line;
			String [] parts;
			
			BufferedReader br = new BufferedReader(new FileReader(NTCIR10_DR_BASELINE));
			//without first line
			br.readLine();
			while(null != (line=br.readLine())){
				parts = line.split(" ");
				//System.out.println(parts[0]+"\t"+parts[1]+"\t"+parts[2]);
				if(Integer.parseInt(parts[3]) < top_k){
					if(poolOfBaseline.containsKey(parts[0])){
						poolOfBaseline.get(parts[0]).add(parts[2]);
					}else {
						ArrayList<String> docList = new ArrayList<String>();
						docList.add(parts[2]);
						
						poolOfBaseline.put(parts[0], docList);
					}					
				}else{
					continue;
				}				
			}
			br.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return poolOfBaseline;
	}
	/**
	 * special topic: en-0060
	 * **/
	public static List<NTCIRTopic> loadNTCIR10TopicList(NTCIR_EVAL_TASK eval){
		
		if(!ICTCLAS2014_INI){
			ICTCLAS2014.iniConfig();
			ICTCLAS2014_INI = true;
		}
		
		ArrayList<NTCIRTopic> topicList = new ArrayList<NTCIRTopic>();
		HashMap<String, NTCIRTopic> topicMap = new HashMap<String, NTCIRTopic>();
		
		//1	load topic id and raw text
		try {
			InputStream infs= new FileInputStream(NTCIR10_TOPIC);
			XSSFWorkbook inbook = new XSSFWorkbook(infs);
			XSSFSheet topicSheet = inbook.getSheetAt(0); 	
			int topicNum = topicSheet.getPhysicalNumberOfRows();
			
			XSSFRow topicRow;
			String id, tText;
			NTCIRTopic topic;
			
			for(int i=0; i<topicNum; i++){
				topicRow = topicSheet.getRow(i);
				id = topicRow.getCell(0).getStringCellValue();
				tText = topicRow.getCell(1).getStringCellValue();
				//
				String simpleSegment = ICTCLAS2014.CLibrary.Instance.NLPIR_ParagraphProcess(tText, 0);
				//
				topic = new NTCIRTopic(id, tText, simpleSegment);
				topicList.add(topic);
				topicMap.put(id, topic);
				//
				//System.out.println(topic.getTopicID()+"\t"+topic.getTopicText()+"\t"+topic.getTopicRepresentation());
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
		
		String line = null;
		String [] strArray = null;
		String split_regrex = null;
		//2	load standard IprobFile
		String iprobFile = null, dqrelsFile = null;
		if(eval == NTCIR_EVAL_TASK.NTCIR10_SM_EN){
			iprobFile = NTCIR10_SM_EN_Iprob;
			dqrelsFile = NTCIR10_SM_EN_Dqrels;
			split_regrex = SM_SPLIT;
		}else if (eval == NTCIR_EVAL_TASK.NTCIR10_SM_CH) {
			iprobFile = NTCIR10_SM_CH_Iprob;
			dqrelsFile = NTCIR10_SM_CH_Dqrels;
			split_regrex = SM_SPLIT;
		}else if (eval == NTCIR_EVAL_TASK.NTCIR10_DR_CH) {			
			iprobFile = NTCIR10_DR_CH_Iprob;
			dqrelsFile = NTCIR10_DR_CH_Dqrels;
			split_regrex = DR_SPLIT;
		}
		
		try {
			BufferedReader iprobReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(iprobFile), CODE_UTF8));
							
			while(null != (line=iprobReader.readLine()))
			{
				strArray = line.split(split_regrex);
				if (DEBUG) {
					System.out.println(line);
					System.out.println(strArray);
				}
				//0202;4;0.115646
				topicMap.get(strArray[0]).addIprob(Integer.parseInt(strArray[1]), Double.parseDouble(strArray[2]));			
			}			
			iprobReader.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		//3	load DqrelsFile
		try {
			BufferedReader dqrelReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(dqrelsFile), CODE_UTF8));
			
			while(null != (line=dqrelReader.readLine()))
			{
				strArray = line.split(split_regrex);
				//0201;1;ͶӰ�����ѡ��;L1
				String levelStr = strArray[3].replaceAll("L", "");
				if (Integer.parseInt(levelStr) > 0) {
					topicMap.get(strArray[0]).addLabeledItem(
							Integer.parseInt(strArray[1]), strArray[2], Integer.parseInt(levelStr));			
				}
			}
			dqrelReader.close();
			//
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!"+line);
		}		
		return topicList;
	}
	
	/*
	private static HashMap<String, String> loadNTCIR11TopicList(){
		HashMap<String, String> topicList = new HashMap<String, String>();
		//
		String line = null;
		String []strArray = null;
		//
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(NTCIR11_TOPIC)));
			
			while(null != (line=reader.readLine()))
			{
				strArray = line.split("\t");
				if (Integer.parseInt(strArray[0]) > 100) {
					break;		
				}else{
					topicList.put(strArray[0], strArray[1]);
				}
				
			}
			reader.close();
			//
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();			
		}		
		return topicList;				
	}
	*/
	
	/**
	 * ch: ? for related query
	 * en: 0060
	 * **/
	
	public static List<SMTopic> loadNTCIR11TopicList(NTCIR_EVAL_TASK eval, boolean PerformIRAnnotation){
		ArrayList<SMTopic> smTopicList = new ArrayList<SMTopic>();
		//suggestions
		try {
			String line = null;			
			//
			BufferedReader suggReader = new BufferedReader(new InputStreamReader(new FileInputStream(NTCIR11_SM_QuerySuggestions), CODE_UTF8));
			//
			String id = null, text = null, suggestion = null, from = null;
			int intID = -1;
			SMTopic smTopic = null;
			while(null != (line=suggReader.readLine()))
			{
				if(line.indexOf("ID") > 0 ){					
					id = line.substring(line.indexOf(">")+1, line.indexOf("</"));
					intID = Integer.parseInt(id);					
				}
				if(NTCIR_EVAL_TASK.NTCIR11_SM_CH == eval){
					if(intID>50){
						smTopicList.add(smTopic);
						break;
					}
				}
				if(NTCIR_EVAL_TASK.NTCIR11_SM_EN == eval){
					if(intID<=50){
						continue;
					}else if(intID>100){
						smTopicList.add(smTopic);
						break;
					}
				}
				if(line.indexOf("Topic") > 0){
					text = line.substring(line.indexOf(">")+1, line.indexOf("</")).trim();					
					//
					if(NTCIR_EVAL_TASK.NTCIR11_SM_CH==eval && Integer.parseInt(id) > 1){						
						smTopicList.add(smTopic);
					}else if(NTCIR_EVAL_TASK.NTCIR11_SM_EN==eval && Integer.parseInt(id) > 51){
						smTopicList.add(smTopic);
					}					
					//
					smTopic = new SMTopic(id, text);
				}else if(line.indexOf("CandidateFrom") > 0){
					from = line.substring(line.indexOf(">")+1, line.indexOf("</"));					
				}else if(line.indexOf("Candidate>") > 0){
					suggestion = line.substring(line.indexOf(">")+1, line.indexOf("</"));					
					//
					if(from.equals("Baidu")){
						smTopic.addBaidu(suggestion);
					}else if(from.equals("Bing")){
						smTopic.addBing(suggestion);
					}else if(from.equals("Sougou")){
						smTopic.addSougou(suggestion);
					}else if(from.equals("Google")){
						smTopic.addGoogle(suggestion);
					}else if(from.equals("Yahoo")){
						smTopic.addYahoo(suggestion);
					}else{
						System.out.println("error from:\t"+from);
						new Exception("error").printStackTrace();
					}					
				}
			}
			suggReader.close();
			//
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();			
		}
		//related queries only for Chinese topic
		if(NTCIR_EVAL_TASK.NTCIR11_SM_CH == eval){
			try {
				BufferedReader rqReader = new BufferedReader(new InputStreamReader(new FileInputStream(NTCIR11_SM_RelatedQueries), CODE_UTF8));
				String line=null, relatedQ=null;
				int id = -1;
				while(null != (line=rqReader.readLine()))
				{
					if(line.indexOf("ID") > 0 ){					
						id = Integer.parseInt(line.substring(line.indexOf(">")+1, line.indexOf("</")));						
					}else if(line.indexOf("Candidate>") > 0){	
						line = line.replaceFirst("<Candidate>", "");
						line = line.replaceFirst("</Candidate>", "");
						//relatedQ = line.substring(11, line.indexOf("</"));
						relatedQ = line;
						//
						smTopicList.get(id-1).addRelatedQ(relatedQ);
					}				
				}
				rqReader.close();
				//
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();			
			}
		}			
		//
		/*
		if(DEBUG){
			int k =51;
			System.out.println("size:\t"+smTopicList.size());
			for(SMTopic smTopic: smTopicList){
				System.out.println(smTopic.getID()+"\t"+(k++));
				System.out.println(smTopic.getTopicText());
				System.out.println(smTopic.suggestionBaidu);
				System.out.println(smTopic.suggestionBing.toString());
				System.out.println(smTopic.suggestionGoogle);
				System.out.println(smTopic.suggestionSougou);
				System.out.println(smTopic.suggestionYahoo);
				System.out.println(smTopic.relatedQList);
			}
		}
		*/		
		//
		for(SMTopic smTopic: smTopicList){
			smTopic.getUniqueRelatedQueries();
			if(NTCIR_EVAL_TASK.NTCIR11_SM_CH == eval){
				for(int k=smTopic.uniqueRelatedQueries.size()-1; k>=0; k--){
					String str = smTopic.uniqueRelatedQueries.get(k);
					if(!QueryPreParser.isOddQuery(str, Lang.Chinese)){
						smTopic.uniqueRelatedQueries.remove(k);
					}					
				}
			}			
		}
		
		if(NTCIR_EVAL_TASK.NTCIR11_SM_CH == eval){
			for(SMTopic smTopic: smTopicList){
				loadChPolysemyList(smTopic);
				if(DEBUG && null!=smTopic.polysemyList){
					System.out.println(smTopic.toString());
					for(String polye : smTopic.polysemyList){
						System.out.println(polye);
					}					
					System.out.println();
				}
			}
		}
				
		//
		if(!PerformIRAnnotation){
			return smTopicList;
		}
		
		//
		ShallowParser enShallowParser = null;
		if(NTCIR_EVAL_TASK.NTCIR11_SM_EN == eval){
			enShallowParser = new ShallowParser(Lang.English);
		}				
		//perform intent role annotation for each topic
		TermIRAnnotator termIRAnnotator = new TermIRAnnotator();
		PhraseIRAnnotator phraseIRAnnotator = new PhraseIRAnnotator();
		for(SMTopic smTopic: smTopicList){
			if(NTCIR_EVAL_TASK.NTCIR11_SM_CH == eval){
				LTPIRAnnotator.getTaggedTopic(smTopic);				
			}else{
				enShallowParser.getTaggedTopic(smTopic);
			}
			
			if(DEBUG){				
				System.out.println(smTopic.toString());
			}
			//
			if(NTCIR_EVAL_TASK.NTCIR11_SM_CH == eval){
				smTopic.setTermIRAnnotations(termIRAnnotator.irAnnotate(smTopic.getTaggedTopic()._taggedTerms, Lang.Chinese));
				smTopic.setPhraseIRAnnotations(phraseIRAnnotator.irAnnotate(smTopic.getTaggedTopic()._taggedPhraseList, Lang.Chinese));
			}else{
				smTopic.setTermIRAnnotations(termIRAnnotator.irAnnotate(smTopic.getTaggedTopic()._taggedTerms, Lang.English));
				smTopic.setPhraseIRAnnotations(phraseIRAnnotator.irAnnotate(smTopic.getTaggedTopic()._taggedPhraseList, Lang.English));
			}			
			//
			smTopic.checkIRAnnotation();
		}
		
		if(DEBUG){
			for(SMTopic smTopic: smTopicList){
				if(smTopic.CompleteSentence){
					System.out.println("Sentence topic:\t"+smTopic.getTopicText());
				}
			}
		}
		
		//perform shallow parsing for subtopic string 	
		HashMap<String, ArrayList<TaggedTerm>> stInstance_all_term = new HashMap<String, ArrayList<TaggedTerm>>();
		HashMap<String, ArrayList<ArrayList<TaggedTerm>>> stInstance_all_phrase = new HashMap<String, ArrayList<ArrayList<TaggedTerm>>>();
		for(SMTopic smTopic: smTopicList){
			if(smTopic.belongToBadCase()){
				System.out.println(smTopic.toString());
				smTopic.printBadCase();
			}else{
				for(int i=0; i<smTopic.uniqueRelatedQueries.size(); i++){
					String rq = smTopic.uniqueRelatedQueries.get(i);
								
					if(NTCIR_EVAL_TASK.NTCIR11_SM_EN == eval){
						stInstance_all_term.put(rq, enShallowParser.getTaggedTerms(rq));
						stInstance_all_phrase.put(rq, enShallowParser.getTaggedPhraseList(rq));
					}else{
						ArrayList<TaggedTerm> taggedTerms = LTPIRAnnotator.getTaggedTerm(smTopic, i);						
						if(null == taggedTerms){
							continue;
						}
						//adobe acrobat 9.0 professional简体中文版 下载
						stInstance_all_term.put(rq, taggedTerms);
						ArrayList<ArrayList<TaggedTerm>> taggedPhraseList = LTPIRAnnotator.getTaggedPhraseList(taggedTerms);
						stInstance_all_phrase.put(rq, taggedPhraseList);
						if(DEBUG){
							System.out.println("tagged terms for:\t"+rq);
							System.out.println("\t"+taggedTerms);
							System.out.println("\t"+taggedPhraseList);
						}
					}					
				}
			}			
		}		
		//perform intent role annotation for subtopic string		
		for(SMTopic smTopic: smTopicList){			
			ArrayList<SubtopicInstance> subtopicInstanceList = new ArrayList<SubtopicInstance>();
			//
			if(!smTopic.belongToBadCase()){
				for(String rq: smTopic.uniqueRelatedQueries){
					SubtopicInstance subtopicInstance = new SubtopicInstance(rq);
					//if has term irannotations
					if(null != smTopic.getTermIRAnnotations()){
						for(IRAnnotation topicTermIRAnnotation: smTopic.getTermIRAnnotations()){
							subtopicInstance.addTermIRAnnotation(
									termIRAnnotator.irAnnotate(stInstance_all_term.get(rq), topicTermIRAnnotation));					
						}
					}
					//if has phrase irannotations
					if(null != smTopic.getPhraseIRAnnotations()){
						for(IRAnnotation topicPhraseIRAnnotation: smTopic.getPhraseIRAnnotations()){
							subtopicInstance.addPhraseIRAnnotation(
									phraseIRAnnotator.irAnnotate(stInstance_all_phrase.get(rq), topicPhraseIRAnnotation));
						}
					}				
					//
					subtopicInstanceList.add(subtopicInstance);
				}		
				//
				smTopic.getSubtopicItemList(subtopicInstanceList);
			}			
		}		
		//
		if(DEBUG){
			System.out.println("---------------!");
			for(SMTopic smTopic: smTopicList){
				if (smTopic.belongToBadCase()) {
					System.out.println("!!!!!!!!!!!-\t"+smTopic);
				}else{
					System.out.println(smTopic.getID()+"\t"+smTopic.getTopicText());
					System.out.println("number of subtopicitem:\t"+smTopic.smSubtopicItemList.size());
					int itemCount = 1;
					for(SMSubtopicItem smSubtopicItem: smTopic.smSubtopicItemList){
						System.out.println("item - "+(itemCount++)+"\tinstance number:\t"+smSubtopicItem.subtopicInstanceGroup.size());
						for(SubtopicInstance instance: smSubtopicItem.subtopicInstanceGroup){
							System.out.println("\t"+instance._text);
						}
						System.out.println("---------");
						if(smSubtopicItem.termModifierGroupList.size() > 0){
							System.out.println("\tTerm-Annotation:");
							int iraCount = 1;
							for(ArrayList<String> moGroup: smSubtopicItem.termModifierGroupList){
								System.out.println("\tPossible Term-IRA-"+(iraCount++));
								System.out.println("\t"+moGroup);
							}
						}
						//
						if(smSubtopicItem.phraseModifierGroupList.size() > 0){
							System.out.println("\tPhrase-Annotation:");
							int iraCount = 1;
							for(ArrayList<String> moGroup: smSubtopicItem.phraseModifierGroupList){
								System.out.println("\tPossible Phrase-IRA-"+(iraCount++));
								System.out.println(moGroup);
							}
						}
					}
				}
			}			
		}
		//
		return smTopicList;
	}	
	
	private static void loadChPolysemyList(SMTopic smTopic){
		////baikepoly_0010.txt
		String dir = OutputDirectory.ROOT+"/ntcir-11/SM/baike/";
		String id = smTopic.getID();
		File polyFile = new File(dir+"baikepoly_"+id+".txt");
		if(polyFile.exists()){
			ArrayList<ArrayList<String>> polyList = new ArrayList<ArrayList<String>>();
			
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(polyFile.getAbsolutePath());
			
			for(String line: lineList){
				
				if(line.equals("汉语词语")){
					continue;
				}
				
				boolean match = false;
				for(int i=0; i<polyList.size(); i++){
					ArrayList<String> poly = polyList.get(i);
					if(segmentMatch(smTopic.getTopicText(), poly, line)){
						poly.add(line);
						match = true;
						break;
					}
				}
				if(!match){
					ArrayList<String> poly = new ArrayList<String>();
					poly.add(line);
					polyList.add(poly);
				}
			}
			
			ArrayList<String> polysemyList = new ArrayList<String>();
			for(ArrayList<String> poly: polyList){
				String p = "";
				for(String x: poly){
					p += x;
					p += "\t";
				}
				polysemyList.add(p.trim());
			}
			
			smTopic.setPolysemyList(polysemyList);
		}		
	}
	
	private static boolean segmentMatch(String exceptText, ArrayList<String> strList, String candidate){
		for(String str: strList){
			if(chMatch(exceptText, str, candidate)){
				return true;
			}
		}
		return false;
		
	}
	private static boolean chMatch(String exceptText, String a, String b){
		Vector<String> strSet = new Vector<String>();
		strSet.add(a);
		strSet.add(b);
		
		LCSScaner lcsScaner = new LCSScaner(strSet, Lang.Chinese);
		ArrayList<StrInt> lcsList = lcsScaner.enumerateLCS_AtLeastK(2);
		for(StrInt lcs: lcsList){
			if(lcs.getFirst().length() >= 2 && !lcs.getFirst().equals(exceptText)){
				return true;
			}
		}
		return false;
	}
	
	public static HashMap<String, ArrayList<String>> loadSystemRun(String sysRunFile, NTCIR_EVAL_TASK eval){
		return loadSystemRun(sysRunFile, CODE_UTF8, eval);
	}
	
	public static HashMap<String, ArrayList<String>> loadSystemRun(String sysRunFile, String encoding, NTCIR_EVAL_TASK eval){
		//system run to be evaluated
		HashMap<String, ArrayList<String>> systemRun = new HashMap<String, ArrayList<String>>();
		
		String split_regrex = null;		
		if(eval == NTCIR_EVAL_TASK.NTCIR10_SM_EN){			
			split_regrex = SM_SPLIT;
		}else if (eval == NTCIR_EVAL_TASK.NTCIR10_SM_CH) {			
			split_regrex = SM_SPLIT;
		}else if (eval == NTCIR_EVAL_TASK.NTCIR10_DR_CH) {		
			split_regrex = DR_SPLIT;
		}
		
		try {
			BufferedReader sysRunReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(sysRunFile), encoding));
			//
			String line;
			String []array;
			while(null != (line=sysRunReader.readLine())){
				//
				if(line.startsWith("<SYSDESC>"))
				{
					continue;
				}
				//
				array = line.split(split_regrex);
				if(systemRun.containsKey(array[0])){
					ArrayList<String> itemList = systemRun.get(array[0]);
					itemList.add(array[2]);
				}else{
					ArrayList<String> itemList = new ArrayList<String>();
					itemList.add(array[2]);
					systemRun.put(array[0], itemList);
				}
			}
			//
			sysRunReader.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return systemRun;
	}
	
	private static PrintStream printer = null; 
	public static void openPrinter(){
		try{
			printer = new PrintStream(new FileOutputStream(new File(OutputDirectory.ROOT+"log.txt")));
			System.setOut(printer);			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void closePrinter(){
		printer.flush();
		printer.close();
	}
	public static void main(String []args){
		//1
		//NTCIRLoader.loadNTCIR10Docs();
		
		//2
		//NTCIRLoader.loadNTCIR10TopicList();
		
		//3
		//NTCIRLoader.openPrinter();
		NTCIRLoader.loadNTCIR11TopicList(NTCIR_EVAL_TASK.NTCIR11_SM_CH, false);
		//NTCIRLoader.closePrinter();
		//NTCIRLoader.loadNTCIR11TopicList(NTCIR_EVAL_TASK.NTCIR11_SM_EN, true);
	}
	
}
