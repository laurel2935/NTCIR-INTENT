package org.archive.ntcir.sm.similarity.editdistance;

import org.archive.ntcir.sm.similarity.Similaritable;
import org.archive.ntcir.sm.similarity.editdistance.definition.EditUnit;
import org.archive.ntcir.sm.similarity.editdistance.definition.SuperString;


public abstract class EditDistance implements Similaritable {
        
    public abstract double getEditDistance(SuperString<? extends EditUnit> S, SuperString<? extends EditUnit> T);    
 
    /*
    public double getSimilarity(String s1, String s2){
    	SuperString<TermEditUnit> S = SuperString.createWordSuperString(s1);
    	SuperString<TermEditUnit> T = SuperString.createWordSuperString(s2);
    	
    	return 1-(getEditDistance(S, T))/(Math.max(S.length(), T.length()));
    }
    */
}
