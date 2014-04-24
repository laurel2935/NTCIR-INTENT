package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
/**
 * corresponding to a subtopic instance
 * **/
public class SubtopicInstance {
	//corresponding to each kernel-object of the topic
	//null will be added if it doesn't include the kernel-object
	public String _text;
	
	public ArrayList<IRAnnotation> termIRAnnotationList;
	public ArrayList<IRAnnotation> phraseIRAnnotationList;
	
	public SubtopicInstance(String text){
		this._text = text;
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
	
	//
	public boolean shrinkMatch(SubtopicInstance cmpSubtopicInstance){
		//term-level
		for(int i=0; i<this.termIRAnnotationList.size(); i++){
			if(this.termIRAnnotationList.get(i).shrinkEquals(cmpSubtopicInstance.termIRAnnotationList.get(i))){
				return true;
			}
		}
		//phrase-level
		for(int j=0; j<this.phraseIRAnnotationList.size(); j++){
			if(this.phraseIRAnnotationList.get(j).shrinkEquals(cmpSubtopicInstance.phraseIRAnnotationList.get(j))){
				return true;
			}
		}
		return false;
	}

}
