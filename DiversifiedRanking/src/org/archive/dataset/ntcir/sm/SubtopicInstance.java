package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
/**
 * corresponding to a subtopic instance
 * **/
public class SubtopicInstance {
	//corresponding to each kernel-object of the topic
	//null will be added if it doesn't include the kernel-object
	private ArrayList<IRAnnotation> termIRAnnotationList;
	private ArrayList<IRAnnotation> phraseIRAnnotationList;
	
	public SubtopicInstance(){
		this.termIRAnnotationList = new ArrayList<IRAnnotation>();
		this.phraseIRAnnotationList = new ArrayList<IRAnnotation>();		
	}
	
	//
	public void addTermIRAnnotation(IRAnnotation irAnnotation){
		this.termIRAnnotationList.add(irAnnotation);
	}
	//
	public void addPhraseIRAnnotation(IRAnnotation irAnnotation){
		this.phraseIRAnnotationList.add(irAnnotation);
	}

}
