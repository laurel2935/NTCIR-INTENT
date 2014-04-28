package org.archive.structure;

public class AOLRecord extends Record{
	//specific fields of AOL	
	protected String queryTime;	
	
	//construct using digital records, ignore the time field due to unnecessary space consumption
	public AOLRecord(String digitalRecord, boolean ignoreQueryTime){
		String [] array = digitalRecord.split("\t");
		if(3 == array.length){
			this.userID = array[0];
			this.queryText = array[1];
			this.queryTime = ignoreQueryTime? null:array[2];
			//
			this.valid = true;
		}else{
			this.userID = array[0];
			this.queryText = array[1];
			this.queryTime = ignoreQueryTime? null:array[2];
			this.itemRank = array[3];
			this.clickUrl = array[4];
			//
			this.valid = true;
		}
	}
	//construct using original records
	public AOLRecord(String unitSerial, String recordText){
		String [] fieldArray = recordText.split("\t");
		//
		if(3 == fieldArray.length && null!=fieldArray[0] && null!=fieldArray[1] && null!=fieldArray[2]){			
			this.userID = unitSerial+"-"+fieldArray[0];
			this.queryText = fieldArray[1].toLowerCase();
			this.queryTime = fieldArray[2];
			//
			this.valid = true;
		}else if(5 == fieldArray.length && null!=fieldArray[0] && null!=fieldArray[1] && null!=fieldArray[2]){			
			this.userID = unitSerial+"-"+fieldArray[0];
			this.queryText = fieldArray[1].toLowerCase();
			this.queryTime = fieldArray[2];
			this.itemRank = fieldArray[3];
			this.clickUrl = fieldArray[4].toLowerCase();
			//
			this.valid = true;
		}else{
			this.valid = false;
		}		
	}
	//
	public String getQueryTime(){
		return this.queryTime;
	}
}
