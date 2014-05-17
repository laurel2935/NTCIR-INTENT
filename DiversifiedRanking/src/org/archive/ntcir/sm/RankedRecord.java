package org.archive.ntcir.sm;

import java.text.DecimalFormat;


/**
 * 
 * **/
public class RankedRecord {
	private static DecimalFormat dataFormat =new DecimalFormat("0.###");
	//
	public String topicID;
	private static String zeroStr = "0";
	public String stStr;
	public int rank;
	public double rankValue;
	public String runTitle;
	//
	public RankedRecord(String topicID, String stStr, int rank, double rankV, String runTitle){
		this.topicID = topicID;
		this.stStr = stStr;
		this.rank = rank;
		this.rankValue = rankV;
		this.runTitle = runTitle;		
	}
	//
	public String toString(){
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(this.topicID+";"+zeroStr+";"+this.stStr+";"+Integer.toString(this.rank)+";");
		strBuffer.append(dataFormat.format(this.rankValue)+";"+this.runTitle);
		return strBuffer.toString();
	}

}
