package org.archive.structure;

public class SogouQRecord2012 extends SogouQRecord2008{
	//specific field of SogouQ2012 version
	protected String queryTime;
	
	public SogouQRecord2012(String daySerial, String recordText){
		super(daySerial, recordText);
	}

}
