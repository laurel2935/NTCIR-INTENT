package org.archive.util.pattern;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.nlp.qpunctuation.PSegment;
import org.archive.util.tuple.StrInt;


public class PatternFactory {
	//--- Pattern ---//
	/**
	 * ^[\u2E80-\u9FFF]+$   匹配所有东亚区的语言    
	 * ^[\u4E00-\u9FFF]+$   匹配简体和繁体  
	 * ^[\u4E00-\u9FA5]+$   匹配简体  	
	 */
	//match Chinese character
	private static final Pattern chCharacterPattern = Pattern.compile("[\u4E00-\u9FFF]+");
	//match non-Chinese character
	private static final Pattern nonChCharacterPattern = Pattern.compile("[^\u4E00-\u9FFF]+");
	//match characters of lower case, number, upper case
	private static final Pattern numberAlphabetPattern = Pattern.compile("[a-z0-9A-Z_]+");
	//match url	address
	private static final String url_1 = "(http|www|ftp|site){1,}(://)?" +
	"(\\.(\\w+(-\\w+)*))*((:\\d+)?)(/(\\w+(-\\w+)*))*(\\.?(\\w)*)(\\?)?(((\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*(\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*)*(\\w*)*)(/)*";
	private static final String url_2 = "("+url_1+"|\\w+(.com)+)";
	private static final Pattern urlPattern = Pattern.compile(url_2);	
	//match email address
	private static final Pattern emailPattern = Pattern.compile("([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}");
	//match date : [12]{1}[0-9]{3}
	private static final Pattern datePattern = Pattern.compile("\\d+[年月日]+|[12]{1}[0-9]{3}");	
	//match drama, TV
	private static final Pattern dramaPattern = Pattern.compile("[第]*[0-9一二三四五六七八九十]+[部集期号季]");
	//match place word
	private static final Pattern placePattern = Pattern.compile("[\u4E00-\u9FFF]+[村乡郡寨县省市区国]");
	//modifier edition pattern
	public static final Pattern moEditionPattern = Pattern.compile("[a-z0-9A-Z_]+[版号]");	
	
	
	
	//--- Prefix / Infix / Suffix ---//
	//For query parsing
	//
	public static final int DirectRQ_L = 4;
	public static final int LeastKO_L = 2;
	public static final int DirectWord_L = 4;
	//
	
	
	
	
	
	
	//--- Interface ---//
	
	//include Chinese character
	public static boolean includeChC(String str){		
		Matcher mat = chCharacterPattern.matcher(str);
		if(mat.find()){
			return true;
		}else{
			return false;
		}
	}
	//a string consists of number and alphabet
	public static boolean strOfNumOrAlphabet(String str){		
		Matcher matcher = numberAlphabetPattern.matcher(str);
		if(matcher.find()){					
			if(matcher.group().equals(str)){
				return true;
			}
		}		
		return false;
	}		
	
	/**
	 * Direct reject cases:
	 * 1. length
	 * 2. interrogative word
	 * **/
	public static boolean RejectQ_Length(String query){
		if(query.length() < LeastKO_L){
			return true;
		}
		return false;
	}
	public static boolean RejectQ_InterrogativeW(String query){
		if(containInterrogativeW(query)){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * check interrogative word
	 * **/
	private static Vector<String> ruleWordSet = new Vector<String>();
	private static boolean containInterrogativeW(String query){
		if(ruleWordSet.size() == 0 ){
			ruleWordSet.add("吗");
			ruleWordSet.add("哪");
			ruleWordSet.add("谁");
			ruleWordSet.add("怎么");
			ruleWordSet.add("怎样");			
			ruleWordSet.add("为什么");
			ruleWordSet.add("什么是");		
			ruleWordSet.add("什么");
			ruleWordSet.add("是否");
			ruleWordSet.add("能否");
			ruleWordSet.add("如何");
			ruleWordSet.add("哪个");
			ruleWordSet.add("何时");
			ruleWordSet.add("多少");
		}
		//
		for(String iw: ruleWordSet){
			if(query.indexOf(iw) >= 0){
				return true;
			}
		}
		//
		return false;
	}
	/**
	 * Check whether the given query consists of a single kernel-object
	 * Based on a group of rules:
	 * Length restriction: LeastKO_L<= |q| <= DirectRQ_L
	 * Suffix matching: 
	 * **/	
	public static boolean KoQuery_Length(String query){
		//Length
		if(query.length()>=LeastKO_L && query.length()<=DirectRQ_L){
			return true;
		}		
		//
		return false;
	}
	//
	/*
	public static boolean KoQuery_Suffix(String query){
		//Pre-defined suffix-array
		//骗局残局
		for(int i=0; i<SuffixArrayForKoQ.length; i++){
			if(query.endsWith(SuffixArrayForKoQ[i]) && !containSeparatorSymbol(query)
					&& !containContradictSuffix(query)){
				return true;
			}
		}
		//
		return false;
	}
	*/
	
	
	
	
	
	/**
	 * Combination for joint segments
	 * 1. 
	 * 2. head joint-place
	 * **/
	
	
	
	
	
		
	/**
	 * A word being a modifier with a certain degree
	 * **/
	/*
	public static boolean mustBeModifier(String wStr){
		if(isTimeWord(wStr) || isMoPlace(wStr)||
				isTvWord(wStr) || isMoEditionWord(wStr)){
			//
			return true;
		}else{
			return false;
		}
	}
	*/
	//
	/*
	private static boolean isMoPlace(String wStr){
		if(inPlaceDict(wStr) && matchPlacePattern(wStr)){
			return true;
		}else{
			return false;
		}
	}
	*/
	//
	private static boolean isTimeWord(String wStr){
		Matcher matcher = datePattern.matcher(wStr);
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
		Matcher matcher = dramaPattern.matcher(wStr);
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
	//including non-Chinese character
	public static boolean includeNonChC(String query){
		Matcher matcher = nonChCharacterPattern.matcher(query);
		if(matcher.find()){
			return true;
		}else{
			return false;
		}
	}	
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
	//quantify the weight being the kernel-object without contextual information
	/*
	public static float koWeight(String word, Vector<String> wordVec){			
		double maxWLength = 0;		
		for(String w: wordVec){			
			if(w.length() > maxWLength){
				maxWLength = w.length();
			}
		}		
		//
		double position = -1;
		for(int i=0; i<wordVec.size(); i++){
			if(wordVec.get(i).equals(word)){
				position = i+1;
				break;
			}
		}		
		//
		double value;
		//
		double lenV = lengthFactor(word, wordVec, maxWLength, lenDiscount);
		double posV = positonFactor(wordVec.size()*1.0, position);				
		//
		value = lenV*posV;		
		//		
		return (float)value;
	}
	*/
	//
	//lcs as an existed kernel-object
	public static boolean f_ko_lcs(String lcs, Hashtable<String, StrInt> currentKOTable){
		return currentKOTable.containsKey(lcs);
	}
	//lcs as a whole query 
	public static boolean f_q_lcs(String lcs, Vector<String> compareQSet){
		for(String q: compareQSet){
			if(lcs.indexOf(q) >= 0){
				return true;
			}
		}
		//
		return false;
	}	
	//lcs as a possible mwe4
	/*
	public static boolean f_mwe_lcs(String lcs, Hashtable<String, StrInt> lcsTable, int queryNum){
		if(lcsTable.containsKey(lcs)){
			if(lcs.length() >= LeastMWE_L){
				if(queryNum>2){
					return lcsTable.get(lcs).second >= leastFre_Mul_3Q;
				}else if(queryNum == 2){
					return lcsTable.get(lcs).second >= LeastFre_Mul_2Q;
				}
				return false;
			}else{
				return false;
			}
		}else{
			return false;
		}		
	}
	*/
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
	public static void combineJointWord(Vector<String> wordSet, boolean check_run){
		combineConsecutiveSingleChCs(wordSet);
		//joint cases
		//JointSegment_PlacePreFix(wordSet, check_run);
		//JointSegment_EditionInFix(wordSet);		
		//JointSegment_OrgInFix(wordSet);
	}
	//
	private static boolean singleChC(String word){
		if(word.length()==1 && !includeNonChC(word)){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * Stack s = new Stack(); 创建一个栈;
	 * s.empty(); 测试堆栈是否为空;
	 * s.peek(); 查看栈顶对象而不移除它;
	 * s.pop(); 移除栈顶对象并作为此函数的值返回该对象;
	 * s.push(); 把项压入栈顶
	 * **/
	public static boolean combineConsecutiveSingleChCs(Vector<String> wordSet){
		boolean combined = false;
		if(wordSet.size() > 1){
			if(wordSet.get(0).length()==1 && wordSet.get(1).length()>1){
				wordSet.set(0, wordSet.get(0)+wordSet.get(1));
				wordSet.remove(1);
				combined = true;
			}
		}
		//
		if(wordSet.size() > 1){
			Stack<Integer> stack = new Stack<Integer>();
			//
			for(int i=0; i<wordSet.size(); i++){
				if(singleChC(wordSet.get(i))){
					if(stack.size() > 0){
						if(stack.peek() == (i-1)){
							stack.push(i);
						}else{
							stack.clear();
							stack.push(i);
						}
					}else{
						stack.push(i);
					}
				}else if(stack.size() == 1){
					stack.clear();
				}else if(stack.size()>1){
					break;
				}
			}
			//
			//
			if(stack.size() > 1){
				StringBuffer buffer = new StringBuffer();
				int tail = stack.pop();
				buffer.append(wordSet.get(tail));
				//
				int head = -1;
				while(stack.size() > 0){
					head = stack.pop();
					buffer.insert(0, wordSet.get(head));					
				}
				//
				wordSet.set(head, buffer.toString());
				for(int k=head+1; k<=tail; k++){
					wordSet.remove(k);
					tail--;
					k--;
				}
				combined = true;
			}
		}
		//
		return combined;		
	}	
	
	//
	public static boolean validQuery(String query){
		if(query.length()>30){
			return false;
		}else{
			if(includeNonChC(query)){
				Vector<PSegment> mat;
				if(null!=(mat=patternMatch(query, urlPattern))){
					return false;
				}else if(null!=(mat=patternMatch(query, emailPattern))){
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
}
