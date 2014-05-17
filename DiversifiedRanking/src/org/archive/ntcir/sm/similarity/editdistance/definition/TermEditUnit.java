package org.archive.ntcir.sm.similarity.editdistance.definition;

public class TermEditUnit extends EditUnit {
	private Term term = null;
	
	public TermEditUnit(Term term){
		this.term = term;
	}
	
	public TermEditUnit(String tTxt){
		this.term = new Term(tTxt);
	}
		
	public String getUnitString() {
		return term.getTerm();
	}
	
	//
	/*
	@Override
	public double getSubstitutionCost(EditUnit otherUnit){
		if(!(otherUnit instanceof WordEditUnit)) return 1.0;
		if(equals(otherUnit)) return 0.0;
		
		WordEditUnit other = (WordEditUnit)otherUnit;
		//
		if(word.getPos()!=other.word.getPos()){
			return 1.0;
		}
		return 1 - XiaConceptParser.getInstance().getSimilarity(getUnitString(), other.getUnitString());
	}
	*/
	/*
	@Override	
	public boolean equals(Object other){
    	if(!(other instanceof WordEditUnit)) return false;
    	WordEditUnit otherUnit = (WordEditUnit)other;
    	Word otherWord = otherUnit.word;
    	//
		if(word.getPos()!=otherWord.getPos()){
			return false;
		}
		double sim = XiaConceptParser.getInstance().getSimilarity(getUnitString(), otherUnit.getUnitString());
		return sim>0.85;
	}
	*/
}
