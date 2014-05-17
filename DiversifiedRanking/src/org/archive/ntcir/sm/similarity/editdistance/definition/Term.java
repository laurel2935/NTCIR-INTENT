package org.archive.ntcir.sm.similarity.editdistance.definition;

public class Term {
	/** ¥ ”Ôƒ⁄»› */
	private String str;
	/** ¥ ”Ô¥ –‘ */
	private String pos = null;
	
	public Term(String tTxt, String pos){
		this.str = tTxt;
		this.pos = pos;
	}	
	public Term(String tTxt){
		this.str = tTxt;
	}
	public String getTerm() {
		return str;
	}
	public void setTerm(String tTxt) {
		this.str = tTxt;
	}
	public String getPos() {
		return pos;
	}
	public void setPos(String pos) {
		this.pos = pos;
	}			
}