package org.archive.dataset.ntcir;

import java.util.ArrayList;
import java.util.HashSet;

import org.archive.sm.data.IRAnnotation;

public class SMTopic {
	
	private String _id;
	private String _text;
	private ArrayList<IRAnnotation> _nounIRAnnotationList;
	private ArrayList<IRAnnotation> _npIRAnnotationList;
	
	public ArrayList<String> suggestionBaidu;
	public ArrayList<String> suggestionBing;
	public ArrayList<String> suggestionGoogle;
	public ArrayList<String> suggestionSougou;
	public ArrayList<String> relatedQList;
	
	public ArrayList<String> uniqueRelatedQueries;
	
	public SMTopic(String id, String text){
		this._id = id;
		this._text = text;
		//
		suggestionBaidu = new ArrayList<String>();
		suggestionBing = new ArrayList<String>();
		suggestionGoogle = new ArrayList<String>();
		suggestionSougou = new ArrayList<String>();
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
	public ArrayList<IRAnnotation> getNounIRAnnotations(){
		return this._nounIRAnnotationList;
	}
	public void setNounIRAnnotations(ArrayList<IRAnnotation> nounIRAnnotationList){
		this._nounIRAnnotationList = nounIRAnnotationList;
	}
	public ArrayList<IRAnnotation> getNpIRAnnotations(){
		return this._npIRAnnotationList;
	}
	public void setNpIRAnnotations(ArrayList<IRAnnotation> npIRAnnotationList){
		this._npIRAnnotationList = npIRAnnotationList;
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
		for(String q: this.relatedQList){
			if(!qSet.contains(q)){
				qSet.add(q);
				this.uniqueRelatedQueries.add(q);
			}
		}
	}
}
