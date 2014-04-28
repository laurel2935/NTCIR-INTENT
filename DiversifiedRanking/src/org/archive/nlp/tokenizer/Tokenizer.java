package org.archive.nlp.tokenizer;

import java.util.ArrayList;
import java.util.Vector;

import org.archive.nlp.chunk.ShallowParser;
import org.archive.nlp.qpunctuation.QPunctuationParser;
import org.archive.nlp.qpunctuation.QSegment;
import org.archive.nlp.qpunctuation.QueryPreParser;
import org.archive.nlp.tokenizer.ictclas.ICTCLAS2014;
import org.archive.util.Language.Lang;

public class Tokenizer {
	private final static boolean DEBUG = true;
	//for English text segmentation
	private static ShallowParser stanfordParser = null;
	private static final String BLANK = " ";
	
	
	//for Chinese, a string including fewer than 3 or just 3 characters is directly regarded as a word	 
	private static final int chDirectWordLength_Threshold = 3;
	//for English, a string including no blank, is directly regarded as one word 
	private static final int enDirectWordLength_Threshold = 1;
		
	////////////////////////
	//common segmentation
	////////////////////////
	public static ArrayList<String> segment(String text, Lang lang){
		return 	segment(text, lang, false);
	}
	//
	private static ArrayList<String> segment(String text, Lang lang, boolean checkLength){
		//
		if(checkLength && isDirectWord(text, lang)){
			ArrayList<String> wordList = new ArrayList<String>();
			wordList.add(text);
			return wordList;
		}
		//
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
	//
	private static boolean isDirectWord(String text, Lang lang){
		if(Lang.English == lang){
			return text.split(BLANK).length<=enDirectWordLength_Threshold? true:false;
		}else if(Lang.Chinese == lang){
			return text.length()<=chDirectWordLength_Threshold? true:false;
		}else{
			new Exception("Language type error!").printStackTrace();
			return false;
		}	
	}
	//
	
	////////////////////
	//query-oriented segmentation
	////////////////////
	/**
	 * taking the possible symbols into consideration 
	 * **/
	public static ArrayList<String> adaptiveQuerySegment(Lang lang, String rawQuery, boolean checkSymbol, boolean checkLength){
		ArrayList<String> words=null;
		//
		if(!checkSymbol && !checkLength){
			return segment(rawQuery, lang, false);
		}else if(!checkSymbol && checkLength){
			return segment(rawQuery, lang, true);
		}else if(checkSymbol && Lang.Chinese==lang){
			if(QPunctuationParser.includePunctuation(rawQuery)){
				Vector<QSegment> symbolSegmentSet;
				if(null!=(symbolSegmentSet=QueryPreParser.symbolAnalysis(rawQuery)) && symbolSegmentSet.size()>0){			
					//
					words = segment(symbolSegmentSet, lang);
				}
			}else{			
				//
				words = segment(rawQuery, lang, true);
			}
			//
			if(null!=words && words.size()>0){
				return words;
			}else{
				return null;
			}
		}else{
			new Exception("Symbol check is not available for English").printStackTrace();
			return null;
		}		
	}
	///
	private static ArrayList<String> segment(Vector<QSegment> segmentSet, Lang lang){
		ArrayList<String> wordSet = new ArrayList<String>();
		//		
		for(QSegment seg: segmentSet){
			if(seg.unit){
				wordSet.add(seg.getStr());
			}else if(isDirectWord(seg.getStr(), lang)){
				wordSet.add(seg.getStr());
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
	//
	
	
	////////////////////////
	//additional utility
	////////////////////////
	
	public static String replaceSymboleAsBlank(String rawText, Lang lang){
		if(Lang.Chinese == lang){
			if((QPunctuationParser.includePunctuation(rawText))){
				Vector<QSegment> symbolSegmentSet;
				if(null!=(symbolSegmentSet=QueryPreParser.symbolAnalysis(rawText)) && symbolSegmentSet.size()>0){			
					//
					String result = "";
					result += symbolSegmentSet.get(0).getStr();
					for(int i=1; i<symbolSegmentSet.size(); i++){
						result += BLANK;
						result += symbolSegmentSet.get(i).getStr();
					}
					if(DEBUG){
						System.out.println(result);
					}
					return result;				
				}else{
					System.out.println("No meaning input!");
					return null;
				}
			}else{
				if(DEBUG){
					System.out.println(rawText);
				}
				return rawText;
			}
		}else{
			new Exception("Lang type error!").printStackTrace();
			return null;
		}		
	}
	
	
	//
	public static void main(String []args){
		//1 query pre-parsing test
		//http+www.10010.com
		//android2.3游戏下载
		//植物大战僵尸+安卓2.3
		String rawText = "最新ie浏览器2011官方下载";
		Tokenizer.replaceSymboleAsBlank(rawText, Lang.Chinese);
		
	}
}
