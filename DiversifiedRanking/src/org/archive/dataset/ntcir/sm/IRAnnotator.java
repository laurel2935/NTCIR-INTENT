package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
import java.util.Vector;

import org.archive.util.Language.Lang;

public abstract class IRAnnotator {
	private final static boolean DEBUG = true;
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
    	if(DEBUG){
    		System.out.println("Topic Annotation:\t"+topicIRAnnotation.toString());
    	}
    	if(include){
    		IRAnnotation canIrAnnotation = new IRAnnotation(canKO, moSet);
    		if(DEBUG){
    			System.out.println("Subtopic String IRA:\t"+canIrAnnotation.toString());
    		}
    		return canIrAnnotation;
    	}else {
    		if(DEBUG){
    			System.out.println("No Subtopic String IRA!");
    		}
			return null;
		}
	}
	//
	public ArrayList<IRAnnotation> irAnnotate(ArrayList<TaggedTerm> topicTaggedTerms, Lang lang){
		if(null == topicTaggedTerms){
			return null;
		}
		//
		Vector<Integer> acceptPosIndex = new Vector<Integer>();
		for(int i=0; i<topicTaggedTerms.size(); i++){
			if(accept(topicTaggedTerms.get(i).posTag, lang)){
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
			
			if(DEBUG){
				for(IRAnnotation irAnnotation: irAnnotations){
					System.out.println(irAnnotation.toString());
				}
				System.out.println();
			}
			
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
	protected abstract boolean accept(String posTag, Lang lang);
}
