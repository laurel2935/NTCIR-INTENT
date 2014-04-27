package org.archive.nlp.tokenizer;

import java.util.ArrayList;
import java.util.Vector;

import org.archive.nlp.chunk.ShallowParser;
import org.archive.nlp.qpunctuation.QPunctuationParser;
import org.archive.nlp.qpunctuation.QSegment;
import org.archive.nlp.tokenizer.ictclas.ICTCLAS2014;
import org.archive.util.Language.Lang;
import org.archive.util.pattern.RQConstants;

public class Tokenizer {
	//for english text segmentation
	private static ShallowParser stanfordParser = null;	
	//
	private static final int chDirectWordLength_Threshold = 3;
		
	////////////////////////
	//common segmentation
	////////////////////////
	public static ArrayList<String> segment(String text, Lang lang){
		if(Lang.English == lang){
			if(null == stanfordParser){
				stanfordParser = new ShallowParser(Lang.English);
			}
			return stanfordParser.segment(text);
		}else if(Lang.Chinese == lang){
			return ICTCLAS2014.segment(text);
		}else{
			new Exception("Language type error!").printStackTrace();
			return null;
		}		
	}
	
	////////////////////
	//query segmentation
	////////////////////
	/**
	 * taking the possible symbols into consideration 
	 * **/
	public static ArrayList<String> qSegment(String rawQuery, Lang lang){
		ArrayList<String> words=null;
		//
		if(RQConstants.containSeparatorSymbol(rawQuery)){
			Vector<QSegment> symbolSegmentSet;
			if(null!=(symbolSegmentSet=QPunctuationParser.parse(rawQuery)) && symbolSegmentSet.size()>0){			
				//
				words = segment(symbolSegmentSet, lang);
			}
		}else{			
			//
			words = segment(rawQuery, lang);
		}
		//
		if(null!=words && words.size()>0){
			return words;
		}else{
			return null;
		}
	}
	//
	private static ArrayList<String> segment(Vector<QSegment> segmentSet, Lang lang){
		ArrayList<String> wordSet = new ArrayList<String>();
		//		
		for(QSegment seg: segmentSet){
			if(seg.unit){
				wordSet.add(seg.getStr());
			}else if(Lang.Chinese==lang && seg.getStr().length()<=chDirectWordLength_Threshold){
				wordSet.add(seg.getStr());
			}else if(Lang.Chinese==lang && seg.getStr().indexOf("+") >= 0 && RQConstants.containChCharacter(seg.getStr())){
				String s2 = seg.getStr().replaceAll("\\+", " ").trim();
				wordSet.add(s2);
			}else{
				ArrayList<String> wSet = segment(seg.getStr(), lang);
				if(null != wSet){
					wordSet.addAll(wSet);
				}					
			}
		}
		//
		for(int i=0; i<wordSet.size(); i++){
			if(wordSet.get(i).indexOf("+") >= 0){
				wordSet.set(i, wordSet.get(i).replaceAll("\\+", " ").trim());
			}
		}		
		return wordSet;
	}
}
