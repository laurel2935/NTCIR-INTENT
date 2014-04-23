package org.archive.sm.data;

import java.util.Vector;

/**
 * intent role annotation
 * **/
public class IRAnnotation {
	
	public boolean _bad = false;
	
	public KernelObject ko = null;
	
	public Vector<Modifier> moSet = null;
	//
	public IRAnnotation(KernelObject ko, Vector<Modifier> moSet){
		this.ko = ko;
		this.moSet = moSet;	
	}		
	//
	public IRAnnotation(boolean bad){
		this._bad = bad;
	}
	
	public int hashCode()
    {
		int hash = 0;
		//
		for(Modifier mo: this.moSet){
			hash += mo.hashCode();
		}		
		hash += this.ko.hashCode();
		//
		return hash;
    }
	//
	public boolean equals(Object other){
		if(this == other){
			return true;
		}
		if(other instanceof IRAnnotation){
			final IRAnnotation cmpIRA = (IRAnnotation)other;
			//
			if(this.ko.equals(cmpIRA.ko)){
				if(this.moSet.size() == cmpIRA.moSet.size()){
					if(this.moSet.size()>0){
						return compareVector(this.moSet, cmpIRA.moSet);
					}
					return true;					
				}else{
					return false;
				}
			}else{
				return false;
			}						
		}else{
			return false;
		}
	}
	/**
	 * Compare modifier vector corresponding to the MergeThreshold:
	 * When two irAnnotation share the same kernel-object,
	 * we determine whether they indicate the same intent based on the most left two modifiers
	 * **/
	private boolean compareVector(Vector<Modifier> vectorA, Vector<Modifier> vectorB){
		if(vectorA.containsAll(vectorB) && vectorB.containsAll(vectorA)){
			return true;
		}else{
			return false;
		}	
	}	
	//
	public static void main(String []args){
		
	}
}
