package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
import java.util.Vector;

public abstract class IRAnnotator {
	//
	public IRAnnotation irAnnotate(ArrayList<TaggedTerm> canTaggedTerms, IRAnnotation topicIRAnnotation){
		if(null == canTaggedTerms){
			return null;
		}
		//
		boolean include = false;
		ArrayList<Modifier> moSet = new ArrayList<Modifier>();
    	KernelObject canKO = null;
    	for(TaggedTerm taggedTerm: canTaggedTerms){    		
    		if(taggedTerm.koMatch(topicIRAnnotation.ko)){
    			include = true;
    			canKO = taggedTerm.toKernelObject();
    		}else{
    			moSet.add(taggedTerm.toModifier());
    		}    		  		
    	}
    	if(include){
    		return new IRAnnotation(canKO, moSet);
    	}else {
			return null;
		}
	}
	//
	public ArrayList<IRAnnotation> irAnnotate(ArrayList<TaggedTerm> topicTaggedTerms){
		if(null == topicTaggedTerms){
			return null;
		}
		//
		Vector<Integer> acceptPosIndex = new Vector<Integer>();
		for(int i=0; i<topicTaggedTerms.size(); i++){
			if(accept(topicTaggedTerms.get(i).posTag)){
				acceptPosIndex.add(i);
			}
		}
		//
		if(acceptPosIndex.size()>0){
			ArrayList<IRAnnotation> irAnnotations = new ArrayList<IRAnnotation>();
			//
			for(int k=0; k<acceptPosIndex.size(); k++){
				irAnnotations.add(getIRAnnotation(acceptPosIndex.get(k), topicTaggedTerms));
			}
			//
			return irAnnotations;
		}else{
			return null;
		}
	}	
	//
	protected IRAnnotation getIRAnnotation(int koIndex, ArrayList<TaggedTerm> topicTaggedTerms){
		ArrayList<Modifier> moSet = new ArrayList<Modifier>();
		KernelObject ko = topicTaggedTerms.get(koIndex).toKernelObject();
		for(int i=0; i<topicTaggedTerms.size(); i++){
			if(i != koIndex){
				moSet.add(topicTaggedTerms.get(i).toModifier());
			}
		}
		return new IRAnnotation(ko, moSet);
	}
	//
	protected abstract boolean accept(String posTag);
}
