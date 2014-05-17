package org.archive.ntcir.sm;

import java.util.Vector;

public class RankedList {
	public Vector<RankedRecord> recordList;
	//
	public void addRecord(RankedRecord record){
		if(null == this.recordList){
			this.recordList = new Vector<RankedRecord>();
		}
		//
		this.recordList.add(record);
	}
	//
	public int getSize(){
		return this.recordList.size();
	}
}
