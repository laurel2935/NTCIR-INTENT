package org.archive.ntcir.sm.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.archive.OutputDirectory;
import org.archive.dataset.DataSetDiretory;
import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.nlp.chunk.lpt.ltpService.LTML;
import org.archive.nlp.chunk.lpt.ltpService.LTPOption;
import org.archive.nlp.chunk.lpt.ltpService.LTPService;
import org.archive.util.tuple.StrStr;

public class TopicParser {
	//
	public static final boolean DEBUG = true;
	//
	public static ArrayList<StrStr> loadTopicList_NTCIR11_SM_CH(){
		ArrayList<StrStr> topicList_NTCIR11_SM_CH = new ArrayList<StrStr>();
		try {
			String file = DataSetDiretory.ROOT+DataSetDiretory.NTCIR11_SM + "IMine.Query.txt";
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), NTCIRLoader.CODE_UTF8));
			//
			String line = null;
			String [] array;
			int lineCount = 1;
			while(null != (line=reader.readLine())){
				array = line.split("\t");
				topicList_NTCIR11_SM_CH.add(new StrStr(array[0], array[1]));
				lineCount++;
				if(lineCount > 50){
					break;
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(DEBUG){
			System.out.println("-----");
			for(StrStr topic: topicList_NTCIR11_SM_CH){
				System.out.println(topic.first+"\t"+topic.second);
			}
			System.out.println("-----");
		}
		return topicList_NTCIR11_SM_CH;
	}
	//
	private static void parseNTCIR11SMChTopics(ArrayList<StrStr> topicList, String ltpOption){
		//
		File dirFile = new File(OutputDirectory.NTCIR11_Buffer);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}
		//
		String outputFile = null;
		LTPService ls = new LTPService("yu-haitao@iss.tokushima-u.ac.jp:IF7ynN42"); 
		for(StrStr topic: topicList){
			 outputFile = OutputDirectory.NTCIR11_Buffer+topic.first+".xml";
			 //
			 try {
	            ls.setEncoding(LTPOption.UTF8);
	            //perform all operations!!!
	            LTML ltml = ls.analyze(ltpOption, topic.getSecond());
	            ltml.saveDom(outputFile);
			 }catch(Exception e1){
				 e1.printStackTrace();				 
			 }
			 //
			 try{
				 System.out.println("snooze...");
				 Thread.sleep(100);
			 }catch(InterruptedException e){
				 e.printStackTrace();
			 }
		}
		ls.close();
	}
	//
	private static void parseNTCIR11SMChTopics(String ltpOption){
		parseNTCIR11SMChTopics(loadTopicList_NTCIR11_SM_CH(), ltpOption);
	}
	
	//
	private static void parseTopicTest(){
		StrStr smTopic = new StrStr("0017", "̩���ز�");
		File dirFile = new File(OutputDirectory.NTCIR11_Buffer);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}
		String outputFile = OutputDirectory.NTCIR11_Buffer+smTopic.first+".xml";
		//
		LTPService ls = new LTPService("yu-haitao@iss.tokushima-u.ac.jp:IF7ynN42"); 
		//
        try {
            ls.setEncoding(LTPOption.UTF8);
            LTML ltml = ls.analyze(LTPOption.PARSER, smTopic.getSecond());
            ltml.saveDom(outputFile);
            /*
            int sentNum = ltml.countSentence();
            for(int i = 0; i< sentNum; ++i){
                ArrayList<Word> wordList = ltml.getWords(i);
                System.out.println(ltml.getSentenceContent(i));
                for(int j = 0; j < wordList.size(); ++j){
                    System.out.print("\t" + wordList.get(j).getWS());
                    System.out.print("\t" + wordList.get(j).getPOS());
                    System.out.print("\t" + wordList.get(j).getNE());
                    System.out.print("\t" + wordList.get(j).getParserParent() + 
                            "\t" + wordList.get(j).getParserRelation());
                    if(ltml.hasSRL() && wordList.get(j).isPredicate()){
                        ArrayList<SRL> srls = wordList.get(j).getSRLs();
                        System.out.println();
                        for(int k = 0; k <srls.size(); ++k){
                            System.out.println("\t\t" + srls.get(k).type + 
                                    "\t" + srls.get(k).beg +
                                    "\t" + srls.get(k).end);
                        }
                    }
                    System.out.println();
                }
            }
            */
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            ls.close();
        }
	}
	
	//
	public static void main(String []args){
		// test
		//TopicParser.parseTopicTest();
		//TopicParser.loadTopicList_NTCIR11_SM_CH();
		
		//1 parseNTCIR11SMChTopics
		TopicParser.parseNTCIR11SMChTopics(LTPOption.ALL);
	}

}
