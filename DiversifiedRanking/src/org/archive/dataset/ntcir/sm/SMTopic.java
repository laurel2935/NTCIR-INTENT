package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
import java.util.HashSet;

public class SMTopic {
	//
	public static final int ShrinkThreshold = 2;
	
	private String _id;
	private String _text;
	private ArrayList<IRAnnotation> _termIRAnnotationList;
	private ArrayList<IRAnnotation> _phraseIRAnnotationList;
	//for Chinese topic
	public ArrayList<String> suggestionSougou;
	public ArrayList<String> suggestionBaidu;
	public ArrayList<String> relatedQList;	
	//for English topic
	public ArrayList<String> suggestionYahoo;
	//both
	public ArrayList<String> suggestionBing;	
	public ArrayList<String> suggestionGoogle;	
	
	//
	public ArrayList<String> uniqueRelatedQueries;
	
	public ArrayList<SMSubtopicItem> subtopicItemList;
	
	public SMTopic(String id, String text){
		this._id = id;
		this._text = text;
		//
		suggestionBaidu = new ArrayList<String>();
		suggestionBing = new ArrayList<String>();
		suggestionGoogle = new ArrayList<String>();
		suggestionSougou = new ArrayList<String>();
		suggestionYahoo = new ArrayList<String>();
		relatedQList = new ArrayList<String>();
	}
	//
	public void addBaidu(String baSuggestion){
		suggestionBaidu.add(baSuggestion);
	}
	public void addBing(String biSuggestion){
		suggestionBing.add(biSuggestion);
	}
	public void addGoogle(String gSuggestion){
		suggestionGoogle.add(gSuggestion);
	}
	public void addSougou(String sSuggestion){
		suggestionSougou.add(sSuggestion);
	}
	public void addYahoo(String ySuggestion){
		suggestionYahoo.add(ySuggestion);
	}
	public void addRelatedQ(String rSuggestion){
		relatedQList.add(rSuggestion);
	}
	//
	public String getID(){
		return this._id;
	}
	public String getTopicText(){
		return this._text;
	}
	//
	public ArrayList<IRAnnotation> getTermIRAnnotations(){
		return this._termIRAnnotationList;
	}
	public void setTermIRAnnotations(ArrayList<IRAnnotation> termIRAnnotationList){
		this._termIRAnnotationList = termIRAnnotationList;
	}
	public ArrayList<IRAnnotation> getPhraseIRAnnotations(){
		return this._phraseIRAnnotationList;
	}
	public void setPhraseIRAnnotations(ArrayList<IRAnnotation> phraseIRAnnotationList){
		this._phraseIRAnnotationList = phraseIRAnnotationList;
	}
	//
	public void getUniqueRelatedQueries(){
		this.uniqueRelatedQueries = new ArrayList<String>();
		HashSet<String> qSet = new HashSet<String>();
		for(String q: this.suggestionBaidu){
			if(!qSet.contains(q)){
				qSet.add(q);
				this.uniqueRelatedQueries.add(q);
			}
		}
		for(String q: this.suggestionBing){
			if(!qSet.contains(q)){
				qSet.add(q);
				this.uniqueRelatedQueries.add(q);
			}
		}
		for(String q: this.suggestionGoogle){
			if(!qSet.contains(q)){
				qSet.add(q);
				this.uniqueRelatedQueries.add(q);
			}
		}
		for(String q: this.suggestionSougou){
			if(!qSet.contains(q)){
				qSet.add(q);
				this.uniqueRelatedQueries.add(q);
			}
		}
		for(String q: this.suggestionYahoo){
			if(!qSet.contains(q)){
				qSet.add(q);
				this.uniqueRelatedQueries.add(q);
			}
		}
		for(String q: this.relatedQList){
			if(!qSet.contains(q)){
				qSet.add(q);
				this.uniqueRelatedQueries.add(q);
			}
		}
	}
	//first meet first match, which may leads to non-consistent results
	public void getSubtopicItemList(ArrayList<SubtopicInstance> subtopicInstances){
		this.subtopicItemList = new ArrayList<SMSubtopicItem>();
		SMSubtopicItem smSubtopicItem = new SMSubtopicItem(subtopicInstances.get(subtopicInstances.size()-1));
		subtopicInstances.remove(subtopicInstances.size()-1);
		this.subtopicItemList.add(smSubtopicItem);
		//
		while(subtopicInstances.size() > 0){
			SubtopicInstance instance = subtopicInstances.get(subtopicInstances.size()-1);
			boolean matched = false;
			for(SMSubtopicItem item: this.subtopicItemList){
				for(SubtopicInstance taggedInstance: item.subtopicInstanceGroup){
					if(instance.shrinkMatch(taggedInstance)){
						item.addSubtopicInstance(instance);
						matched = true;
						break;
					}
				}
				if(matched){
					break;
				}
			}
			//
			if(matched){
				subtopicInstances.remove(subtopicInstances.size()-1);
			}else{
				SMSubtopicItem newSmItem = new SMSubtopicItem(instance);
				this.subtopicItemList.add(newSmItem);
				subtopicInstances.remove(subtopicInstances.size()-1);
			}
		}
	}
}
