package org.archive.util.pattern;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.nlp.qpunctuation.PSegment;
import org.archive.nlp.qpunctuation.QSegment;


/**
 * 
 * 1. Inner character type : half shaped character
 * 
 * **/

public class RQConstants {	
	//
	public static final int DirectWord_L = 3;	
	//place pattern
	private static final Pattern placePattern = Pattern.compile("[\u4E00-\u9FFF]+[村乡郡寨县省市区国]");
	//time pattern : [12]{1}[0-9]{3}
	public static final Pattern timePattern = Pattern.compile("\\d+[年月日]+|[12]{1}[0-9]{3}");	
	//TV pattern
	public static final Pattern tvPattern = Pattern.compile("[第]*[0-9一二三四五六七八九十]+[部集期号季]");
	//modifier edition pattern
	public static final Pattern moEditionPattern = Pattern.compile("[a-z0-9A-Z_]+[版号]");
	//full-shaped character
	public static final Pattern fullShapedCPattern = Pattern.compile("[\uff00-\uffff]");
	//separator-like symbol
	public static final Pattern separatorSymbolPattern = Pattern.compile("[^a-z0-9A-Z\u4E00-\u9FFF]+");
	/**
	 * ^[\u2E80-\u9FFF]+$   匹配所有东亚区的语言    
	 * ^[\u4E00-\u9FFF]+$   匹配简体和繁体  
	 * ^[\u4E00-\u9FA5]+$   匹配简体  	
	 */
	public static final Pattern nonChPattern = Pattern.compile("[^\u4E00-\u9FFF]+");
	//
	public static final Pattern chPattern = Pattern.compile("[\u4E00-\u9FFF]+");
	//
	//public static QuerySymbolParser qsymbolParser = new QuerySymbolParser();
	//net pattern
	public static String net_1 = "(http|www|ftp|site){1,}(://)?" +
	"(\\.(\\w+(-\\w+)*))*((:\\d+)?)(/(\\w+(-\\w+)*))*(\\.?(\\w)*)(\\?)?(((\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*(\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*)*(\\w*)*)(/)*";
	public static String netSum = "("+net_1+"|\\w+(.com)+)";
	public static Pattern netPattern = Pattern.compile(netSum);
	//mail pattern
	public static Pattern mailPattern = Pattern.compile("([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}");
	//
	private static boolean matchPlacePattern(String wStr){		
		//
		Matcher matcher = placePattern.matcher(wStr);
		if(matcher.find()){
			String mStr = matcher.group();			
			//
			if(mStr.equals(wStr)){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	//		
	//
	private static boolean isTimeWord(String wStr){
		Matcher matcher = timePattern.matcher(wStr);
		if(matcher.find()){
			String mStr = matcher.group();			
			//
			if(mStr.equals(wStr)){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	//
	//
	private static boolean isTvWord(String wStr){
		Matcher matcher = tvPattern.matcher(wStr);
		if(matcher.find()){
			String mStr = matcher.group();			
			//
			if(mStr.equals(wStr)){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	//
	private static boolean isMoEditionWord(String wStr){
		Matcher matcher = moEditionPattern.matcher(wStr);
		if(matcher.find()){
			String mStr = matcher.group();			
			//
			if(mStr.equals(wStr)){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	//
	public static boolean containFullShapedC(String str){
		Matcher matcher = fullShapedCPattern.matcher(str);
		if(matcher.find()){
			return true;
		}else{
			return false;
		}
	}
	//
	/**
	 * full shaped c -> half shaped c
	 * @param input
	 * @return char
	 */
	private static char cFullToHalf(char input){
        if (input == 12288) {
            input = (char) 32;
            
        }else if (input > 65280 && input < 65375) {
            input = (char) (input - 65248);
            
        }else if (input >= 'A' && input <= 'Z') {
        	input += 32;
		}
        
        return input;
	}
	//
	public static String getHalfShapedStr(String str){
		StringBuffer buffer = new StringBuffer();
		char [] cArray = str.toCharArray();
		for(int i=0; i<cArray.length; i++){
			buffer.append(cFullToHalf(cArray[i]));
		}
		return buffer.toString();
	}
	/**
	 * blank type '+'
	 * **/
	public static String convertBlankPlus(String str){
		if(str.indexOf("+") >= 0){
			return str.replaceAll("\\+", " ").trim();
		}else{
			return str;
		}
	}
	//
	public static boolean containSeparatorSymbol(String query){
		Matcher matcher = separatorSymbolPattern.matcher(query);
		if(matcher.find()){
			return true;
		}else{
			return false;
		}
	}
	//including non-Chinese character
	public static boolean containNonChC(String query){
		Matcher matcher = nonChPattern.matcher(query);
		if(matcher.find()){
			return true;
		}else{
			return false;
		}
	}
	//including Chinese character
	public static boolean containChCharacter(String str){		
		Matcher mat = chPattern.matcher(str);
		if(mat.find()){
			return true;
		}else{
			return false;
		}
	}
	//
	/*
	public static Vector<String> directSegment(String rawQuery){
		try{
			Vector<String> wordSet = IKDictSegmenter.segmentChText_IK(rawQuery);
			if(null != wordSet){
				for(int i=0; i<wordSet.size(); i++){
					if(wordSet.get(i).indexOf("+") >= 0){
						wordSet.set(i, wordSet.get(i).replaceAll("\\+", " ").trim());
					}
				}
				return wordSet;
			}else{
				return null;
			}			
		}catch(Exception e){
			System.out.println(rawQuery);
			e.printStackTrace();
		}				
		return null;
	}
	*/
	//
	/*
	public static Vector<String> segment(Vector<QSegment> segmentSet){
		Vector<String> wordSet = new Vector<String>();
		//		
		for(QSegment seg: segmentSet){
			if(seg.unit){
				wordSet.add(seg.getStr());
			}else if(seg.getStr().length() <= RQConstants.DirectWord_L){
				wordSet.add(seg.getStr());
			}else if(seg.getStr().indexOf("+") >= 0 && containChCharacter(seg.getStr())){
				String s2 = seg.getStr().replaceAll("\\+", " ").trim();
				wordSet.add(s2);
			}else{
				Vector<String> wSet = IKDictSegmenter.segmentChText_IK(seg.getStr());
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
	*/
	//
	public static String removeHeadDeC(String wStr){
		if(wStr.length()>1 && wStr.startsWith("的")){
			return wStr.substring(1);
		}else{
			return wStr;
		}
	}
	//
	/**
	 * Function KORank()
	 * 
	 * **/
	//For length factor	
	private static double lengthFactor(String koW, Vector<String> wSet, double maxWLength, double lenDiscount){
		double product = 1.0;
		for(String w: wSet){
			//denominator to differentiate different segmentations
			//numerator to differentiate word-length with the same segmentation
			product *= (koW.length()/(maxWLength+lenDiscount-w.length()));
		}
		//
		return product;
	}
	//For position factor
	private static double positonFactor(double size, double position){
		//size to differentiate different segmentations
		//position to differentiate word of different position
		return Math.exp(1/(size*position));
	}
	//For session-level frequency factor 
	private static double frequencyFactor(double maxWLength, double wLength, double frequency,
			double assCount, double sessionSize){
		//session-level frequency
		double value = frequency/sessionSize;		
		//associative emergence
		value *= ((assCount+0.5)/sessionSize);	
		//discount frequent modifier
		value *= (1/Math.pow(maxWLength+1-wLength, 2));
		//
		return value;
	}	
	//
	public static boolean singleChW(Vector<String> wordSet){
		if(wordSet.size()>1){
			for(int i=0; i<wordSet.size(); i++){
				if(wordSet.get(i).length() == 1){
					return true;
				}
			}
			return false;
		}else{
			return false;
		}
	}			
	//
	private static boolean singleChC(String word){
		if(word.length()==1 && !containNonChC(word)){
			return true;
		}else{
			return false;
		}
	}		
	//
	public static boolean validQuery(String query){
		if(query.length()>30){
			return false;
		}else{
			if(containNonChC(query)){
				Vector<PSegment> mat;
				if(null!=(mat=patternMatch(query, netPattern))){
					return false;
				}else if(null!=(mat=patternMatch(query, mailPattern))){
					return false;
				}else{
					return true;
				}				
			}else{
				return true;
			}
		}		
	}
	//	
	//match the given pattern
	private static Vector<PSegment> patternMatch(String query, Pattern pattern){		
		//
		String mStr;
		Vector<String> uniqueStrSet = new Vector<String>();
		Matcher matcher = pattern.matcher(query);
		while(matcher.find()){
			mStr = matcher.group();			
			if(!uniqueStrSet.contains(mStr)){
				uniqueStrSet.add(mStr);
			}			
			//System.out.println(mStr);
		}
		//
		if(uniqueStrSet.size()>0){
			Vector<PSegment> matSubSet = new Vector<PSegment>();
			PSegment subUnit;
			//
			for(String uS: uniqueStrSet){
				int firstIndex = 0;
				while(0<=(firstIndex=query.indexOf(uS, firstIndex))){
					subUnit = new PSegment(uS, firstIndex, firstIndex+uS.length()-1);
					matSubSet.add(subUnit);
					//
					firstIndex++;
				}
			}		
			//
			if(matSubSet.size()>0){
				return matSubSet;
			}else{
				return null;
			}
		}else{
			return null;
		}		
	}
	/**
	 * 
	 * **/
	public static void main(String []args){
		/**1**/
		/*
		String str_1 = "小鬼当家１０";
		String str_2 = "小鬼当家角10";
		if(RQConstants.containFullShapedC(str_1)){
			System.out.println("true - "+str_1);
			System.out.println(RQConstants.getHalfShapedStr(str_1));
		}
		if(RQConstants.containFullShapedC(str_2)){
			System.out.println("true - "+str_2);
			System.out.println(RQConstants.getHalfShapedStr(str_2));
		}
		*/
		/**2**/
		/*
		Vector<String> qSet = new Vector<String>();
		qSet.add("疯狂的石头+四月");
		qSet.add("小鬼当家+四月");
		*/
		/**3**/
		//台湾	百分百	贴图
		//温州	长途汽车	时刻表
		//北京	对外经济贸易大学	网址		
	}

}
