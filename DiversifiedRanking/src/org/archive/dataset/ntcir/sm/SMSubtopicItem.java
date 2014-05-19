package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
import java.util.HashSet;

public class SMSubtopicItem {
	public SubtopicInstance itemDelegater = null;
	//gourp members of SubtopicInstance
	public ArrayList<SubtopicInstance> subtopicInstanceGroup = null;
	//for computing similarity among different SMSubtopicItem
	//union of the first <SMTopic.ShrinkThreshold> modifiers of member's IRAnnotation corresponding to each topic irAnnotation
	public ArrayList<ArrayList<String>> termModifierGroupList = null;
	public ArrayList<ArrayList<String>> phraseModifierGroupList = null;
	
	SMSubtopicItem(SubtopicInstance subtopicInstance){
		this.subtopicInstanceGroup = new ArrayList<SubtopicInstance>();
		this.subtopicInstanceGroup.add(subtopicInstance);
		//
		this.termModifierGroupList = new ArrayList<ArrayList<String>>();
		this.phraseModifierGroupList = new ArrayList<ArrayList<String>>();
		//initial delegater
		itemDelegater = subtopicInstance;
	}
	
	public void addSubtopicInstance(SubtopicInstance member){
		if(member._text.length() > itemDelegater._text.length()){
			itemDelegater = member;
		}
		this.subtopicInstanceGroup.add(member);
	}
	
	public void getModifierGroupList(){
		SubtopicInstance pilotSubtopicInstance = this.subtopicInstanceGroup.get(0);
		for(int termIRAIndex=0; termIRAIndex<pilotSubtopicInstance.termIRAnnotationList.size(); termIRAIndex++){			
			//term based
			ArrayList<String> termModifierGroup = new ArrayList<String>();
			HashSet<String> termModifierSet = new HashSet<String>();			
			for(SubtopicInstance subtopicInstance: subtopicInstanceGroup){
				IRAnnotation irAnnotation = subtopicInstance.termIRAnnotationList.get(termIRAIndex);
				if(null != irAnnotation){
					int k = Math.min(SMTopic.ShrinkThreshold, irAnnotation.moSet.size());
					for(int i=0; i<k; i++){
						if(!termModifierSet.contains(irAnnotation.moSet.get(i).moStr)){
							termModifierSet.add(irAnnotation.moSet.get(i).moStr);
							termModifierGroup.add(irAnnotation.moSet.get(i).moStr);
						}
					}
				}				
			}
			//
			this.termModifierGroupList.add(termModifierGroup);			
		}
		//
		for(int phraseIRAIndex=0; phraseIRAIndex<pilotSubtopicInstance.phraseIRAnnotationList.size(); phraseIRAIndex++){
			//phrase based
			ArrayList<String> phraseModifierGroup = new ArrayList<String>();
			HashSet<String> phraseModifierSet = new HashSet<String>();
			for(SubtopicInstance subtopicInstance: subtopicInstanceGroup){
				IRAnnotation irAnnotation = subtopicInstance.phraseIRAnnotationList.get(phraseIRAIndex);
				if(null != irAnnotation){
					int k = Math.min(SMTopic.ShrinkThreshold, irAnnotation.moSet.size());
					for(int i=0; i<k; i++){
						if(!phraseModifierSet.contains(irAnnotation.moSet.get(i).moStr)){
							phraseModifierSet.add(irAnnotation.moSet.get(i).moStr);
							phraseModifierGroup.add(irAnnotation.moSet.get(i).moStr);
						}
					}
				}				
			}
			//
			this.phraseModifierGroupList.add(phraseModifierGroup);
		}
	}
}
