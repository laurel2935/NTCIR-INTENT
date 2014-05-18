package org.archive.ntcir.sm.similarity.editdistance.definition;

//
public abstract class EditUnit {
	//
	public abstract String getUnitString();
	
	//
	public double getSubstitutionCost(EditUnit other){
		return this.equals(other)?0:1;
	}
	
	//
    public double getDeletionCost(){
        return 1.0;
    }    
    
    //
    public double getInsertionCost(){
        return 1.0;
    }
    	
    @Override
	public boolean equals(Object other){
    	if(!(other instanceof EditUnit)) return false;
		return getUnitString().equals(((EditUnit)other).getUnitString());
	}
	
	@Override
	public String toString(){
		return getUnitString();
	}
}
