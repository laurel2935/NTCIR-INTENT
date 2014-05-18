package org.archive.ntcir.sm.similarity.editdistance.definition;

/**
 * term text & term pos tag
 * **/

public class TermEditUnit extends EditUnit {
	protected Term term = null;
	
	public TermEditUnit(Term term){
		this.term = term;
	}
	
	public TermEditUnit(String tTxt){
		this.term = new Term(tTxt);
	}
	
	public TermEditUnit(String tTxt, String pos){
		this.term = new Term(tTxt, pos);
	}
		
	public String getUnitString() {
		return term.getTerm();
	}
	
	//
	///*
	@Override
	public double getSubstitutionCost(EditUnit otherUnit){
		if(!(otherUnit instanceof TermEditUnit)) return 1.0;
		if(equals(otherUnit)) return 0.0;
		
		TermEditUnit other = (TermEditUnit)otherUnit;
		//
		if(this.term.getTerm().equals(other.term.getTerm()) 
				&& !this.term.getPos().equals(other.term.getPos())){
			return 0.5;
		}else{
			return 1.0;
		}		
	}
	//*/
	///*
	@Override	
	public boolean equals(Object other){
    	if(!(other instanceof TermEditUnit)) return false;
    	TermEditUnit otherUnit = (TermEditUnit)other;
    	Term otherTerm = otherUnit.term;
    	//
    	return null==this.term.getTerm()? null==otherTerm.getTerm() : this.term.getTerm().equals(otherTerm.getTerm()) 
				&& null==this.term.getPos()? null==otherTerm.getPos() : this.term.getPos().equals(otherTerm.getPos());		
	}
	//*/
}
