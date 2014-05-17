package org.archive.ntcir.sm.similarity.editdistance.definition;

public class CharEditUnit extends EditUnit {
	private String content = "";
	
	public CharEditUnit(Character ch){
		content = ch.toString();
	}
	
	@Override
	public String getUnitString() {
		return content;
	}

}
