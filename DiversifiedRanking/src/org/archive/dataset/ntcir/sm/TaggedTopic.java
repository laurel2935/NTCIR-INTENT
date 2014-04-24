package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;

import edu.stanford.nlp.ling.TaggedWord;

public class TaggedTopic {
	public String _pennString;	
	public ArrayList<TaggedTerm> _taggedTerms;
	public ArrayList<TaggedTerm> _taggedPhrases;
	
	public TaggedTopic(){}
	
	//
	public void setPennString(String pennString){
		this._pennString = pennString;
	}	
	public void setTaggedTerms(ArrayList<TaggedTerm> taggedTerms){
		this._taggedTerms = taggedTerms;
	}
	public void setTaggedPhrases(ArrayList<TaggedTerm> taggedPhrases){
		this._taggedPhrases = taggedPhrases;
	}
	//
	public String toString(){
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(_pennString+"\n");
		for(TaggedTerm taggedTerm: _taggedTerms){
			strBuffer.append(taggedTerm.toString()+"\t");			
		}
		strBuffer.append("\n");
		if(null != _taggedPhrases){
			for(TaggedTerm taggedTerm: _taggedPhrases){
				strBuffer.append(taggedTerm.toString()+"\t");			
			}
			strBuffer.append("\n");
		}		
		return strBuffer.toString();		
	}

}
