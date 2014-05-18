package org.archive.ntcir.sm.similarity.editdistance.definition;

import org.archive.ntcir.sm.similarity.hownet.concept.XiaConceptParser;

/**
 * no pos tag
 * **/

public class SemanticTermEditUnit  extends EditUnit {
	protected Term term = null;
	
	public SemanticTermEditUnit(String tTxt){
		this.term = new Term(tTxt);
	}
	
	//semantic similarity of two terms are considered
	@Override
	public double getSubstitutionCost(EditUnit otherUnit){
		if(!(otherUnit instanceof SemanticTermEditUnit)) return 1.0;
		if(equals(otherUnit)) return 0.0;
		
		SemanticTermEditUnit other = (SemanticTermEditUnit)otherUnit;
		
		return 1 - XiaConceptParser.getInstance().getSimilarity(getUnitString(), other.getUnitString());
	}
	
	@Override
	public boolean equals(Object other){
    	if(!(other instanceof SemanticTermEditUnit)) return false;
    	SemanticTermEditUnit otherUnit = (SemanticTermEditUnit)other;
    	//
		double sim = XiaConceptParser.getInstance().getSimilarity(getUnitString(), otherUnit.getUnitString());
		return sim>0.85;
	}
	
	public String getUnitString() {
		return term.getTerm();
	}

}
