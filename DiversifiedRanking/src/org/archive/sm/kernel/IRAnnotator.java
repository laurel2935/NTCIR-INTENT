package org.archive.sm.kernel;

import java.util.ArrayList;
import java.util.Vector;

import org.archive.sm.data.IRAnnotation;
import org.archive.sm.data.KernelObject;
import org.archive.sm.data.Modifier;
import org.archive.sm.data.TaggedTerm;


public abstract class IRAnnotator {
	//
	public ArrayList<IRAnnotation> irAnnotate(ArrayList<TaggedTerm> topicTaggedTerms){
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
	public IRAnnotation getIRAnnotation(int koIndex, ArrayList<TaggedTerm> topicTaggedTerms){
		Vector<Modifier> moSet = new Vector<Modifier>();
		KernelObject ko = topicTaggedTerms.get(koIndex).toKernelObject();
		for(int i=0; i<topicTaggedTerms.size(); i++){
			if(i != koIndex){
				moSet.add(topicTaggedTerms.get(i).toModifier());
			}
		}
		return new IRAnnotation(ko, moSet);
	}
	//
	public abstract boolean accept(String posTag);
}
