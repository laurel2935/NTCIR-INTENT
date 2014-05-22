package org.archive.ntcir.dr.preprocess;

import java.util.ArrayList;

import org.archive.OutputDirectory;
import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.util.Language.Lang;
import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;
import org.archive.util.io.XmlWriter;
import org.archive.util.tuple.StrStr;

public class Preprocessor {
	
	
	//
	private static void generateQueryFile(){
		ArrayList<StrStr> topicList = NTCIRLoader.loadNTCIR11TopicList(Lang.English);		
		for(StrStr topic: topicList){
			System.out.println(topic.toString());
		}
		
		String dir = OutputDirectory.ROOT+"/ntcir-11/buffer/";
		
		try{
			int id = 1;
			String fileName;
			XmlWriter writer;
			
			fileName = "NTCIR11_SQ_"+StandardFormat.serialFormat(id, "00")+".txt";	
			writer = IOText.getXmlWriter_UTF8(dir+fileName);
			writer.startDocument("parameters");			
			writer.writeElement("printDocuments", Boolean.TRUE.toString());
			System.out.println(topicList.size());
			for(int i=1; i<=topicList.size(); i++){
				StrStr smTopic = topicList.get(i-1);
				System.out.println(smTopic.toString());
				
				if(i%2 == 0){
					writer.startElement("query");					
					writer.writeElement("type", "indri");
					writer.writeElement("number", smTopic.first);					
					writer.writeElement("text", "#combine("+smTopic.second.trim()+")");					
					writer.endElement("query");
					
					writer.endDocument("parameters");
					writer.flush();
					writer.close();
					writer = null;
					//
					id++;
					if((i+1) > topicList.size()){
						break;
					}else{
						fileName = "NTCIR11_SQ_"+StandardFormat.serialFormat(id, "00")+".txt";	
						writer = IOText.getXmlWriter_UTF8(dir+fileName);
						writer.startDocument("parameters");			
						writer.writeElement("printDocuments", Boolean.TRUE.toString());
					}					
				}else{
					writer.startElement("query");					
					writer.writeElement("type", "indri");
					writer.writeElement("number", smTopic.first); 					
					writer.writeElement("text", "#combine("+smTopic.second.trim()+")");					
					writer.endElement("query");
				}
			}	
		}catch(Exception e){
			e.printStackTrace();
		}	
	}
	
	public static void main(String []args){
		//
		Preprocessor.generateQueryFile();
	}

}
