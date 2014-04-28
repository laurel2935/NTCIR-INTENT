package org.archive.structure;

import java.io.BufferedReader;
import java.util.ArrayList;

import org.archive.analysis.ClickThroughAnalyzer;
import org.archive.util.io.IOText;


public class SogouQRecord2012 extends SogouQRecord2008{
	//specific field of SogouQ2012 version
	protected String queryTime;
	
	/*
	//as the query time is added, thus daySerial is not needed to differentiate sessions from different days
	public SogouQRecord2012(String qTime, String subRecordText){
		super();
		this.queryTime = qTime;	
		//int firstTab = recordText.indexOf(ClickThroughAnalyzer.TabSeparator);
		//String qTime = recordText.substring(0, firstTab);
		//String subRecordText = recordText.substring(firstTab+1);
	}
	*/
	//
	public SogouQRecord2012(String recordText, boolean ignoreQueryTime){
		String [] fields = recordText.split(ClickThroughAnalyzer.TabSeparator);
		if(6 == fields.length){
			this.queryTime = ignoreQueryTime? null:fields[0];
			this.userID = fields[1];
			this.queryText = fields[2];
			this.itemRank = fields[3];
			this.clickOrder = fields[4];
			this.clickUrl = fields[5];
			//
			this.valid = true;
		}else{
			this.valid = false;
			System.out.println("unexpected record!"+recordText);
		}
	}
	//
	public String getQueryTime(){
		return this.queryTime;
	}
	//
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append(this.queryTime+ClickThroughAnalyzer.TabSeparator+this.userID+ClickThroughAnalyzer.TabSeparator
				+this.queryText+ClickThroughAnalyzer.TabSeparator+this.itemRank+ClickThroughAnalyzer.TabSeparator
				+this.clickOrder+ClickThroughAnalyzer.TabSeparator+this.clickUrl);
		return buffer.toString();
	}
	
	//
	public static void main(String []args){
		try {
			ArrayList<SogouQRecord2012> recordList = new ArrayList<SogouQRecord2012>();
			//
			String fileString = "E:/Data_Log/DataSource_Raw/SogouQ2012.mini/SogouQ.mini";
			String lineString;
			BufferedReader reader = IOText.getBufferedReader(fileString, "GBK");
			System.out.println(fileString);
			while(null != (lineString=reader.readLine())){
				//String [] array = lineString.split("\t");
				//System.out.println(array.length);
				///*
				//int firstTab = lineString.indexOf(ClickThroughAnalyzer.TabSeparator);
				//String qTime = lineString.substring(0, firstTab);
				//String subRecordText = lineString.substring(firstTab+1);
				SogouQRecord2012 sogouQRecord2012 = new SogouQRecord2012(lineString, false);
				recordList.add(sogouQRecord2012);
				//*/
			}
			reader.close();
			//
			for(SogouQRecord2012 record: recordList){
				System.out.println(record.toString());
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
