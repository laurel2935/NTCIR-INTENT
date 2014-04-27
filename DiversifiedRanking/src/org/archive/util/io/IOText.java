package org.archive.util.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Vector;

import org.archive.util.tuple.IntStrInt;
import org.archive.util.tuple.StrInt;

public class IOText {
	
	/**Ä¬ÈÏ±àÂë*/
	private static final String DEFAULT_ENCODING = "UTF-8";
	
	/**
	 * 
	 * @param targetFile
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getBufferedReader(String targetFile, String encoding) throws IOException{
		File file = new File(targetFile);
		if(!file.exists()){
			return null;
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(targetFile), encoding));
		return reader;
	}
	/**
	 * 
	 * @param targetFile
	 * @param encoding
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getBufferedReader_UTF8(String targetFile) throws IOException{
		File file = new File(targetFile);
		if(!file.exists()){
			return null;
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(targetFile), DEFAULT_ENCODING));
		return reader;
	}
	/**
	 * 
	 */
	public static Vector<String> loadFilePerLine(String targetFile, String encoding){
		Vector<String> lineVector = new Vector<String>();
		try {
			BufferedReader reader = getBufferedReader(targetFile, encoding);
			String line;
			while(null != (line=reader.readLine())){
				if(line.length() > 0){
					lineVector.add(line);
				}				
			}
			reader.close();
			return lineVector;			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
		//
		return null;		
	}
	/**
	 * File format per line:
	 * Integer[usually a frequency]+"\t"+String[usually the text]
	 */
	public static Vector<StrInt> loadStrInts_LineFormat_Int_Str(String targetFile, String encoding){
		Vector<StrInt> lineVector = new Vector<StrInt>();
		try {
			BufferedReader reader = getBufferedReader(targetFile, encoding);
			String line;
			String [] array;
			while(null != (line=reader.readLine())){
				if(line.length() > 0){
					array = line.split("\t");
					lineVector.add(new StrInt(array[1], Integer.parseInt(array[0])));
				}				
			}
			reader.close();
			return lineVector;			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
		//
		return null;
	}
	/**
	 * File format per line: one-column format, and the line count is regarded as the id
	 * String[usually the text], e.g., userid
	 */
	public static HashMap<String, StrInt> loadStrInts_LineFormat_Str(String targetFile, String encoding){
		HashMap<String, StrInt> elementHashMap = new HashMap<String, StrInt>();
		try {
			BufferedReader reader = getBufferedReader(targetFile, encoding);
			String line;	
			int lineCount = 1;
			while(null != (line=reader.readLine())){
				if(line.length() > 0){	
					elementHashMap.put(line, new StrInt(line, lineCount));					
					lineCount++;
				}				
			}
			reader.close();
			return elementHashMap;			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
		//
		return null;
	}
	/**
	 * For unique files, i.e., line format: integer \t string
	 * while the line number that starts from 1 is regarded as the id  
	 * **/
	public static HashMap<String, IntStrInt> loadUniqueElements_LineFormat_IntTabStr(
			String targetFile, String encoding){
		//
		HashMap<String, IntStrInt> elementHashMap = new HashMap<String, IntStrInt>();		
		try {
			BufferedReader reader = getBufferedReader(targetFile, encoding);
			String line;
			//String [] array;
			int lineCount = 1;
			while(null != (line=reader.readLine())){
				if(line.length() > 0){					
					int tabIndex = line.indexOf("\t");
					String intString = line.substring(0, tabIndex);
					String elementString = line.substring(tabIndex+1);
					//id, text, frequency
					IntStrInt intStrInt = new IntStrInt(lineCount, elementString, Integer.parseInt(intString));
					elementHashMap.put(elementString, intStrInt);					
					lineCount++;					
				}				
			}
			reader.close();
			return elementHashMap;			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
		//
		return null;
	}
	/**
	 * 
	 * @param targetFile
	 * @return
	 * @throws IOException
	 */
 	public static BufferedWriter getBufferedWriter_UTF8_(String targetFile) throws IOException{
		//check exist
		File file = new File(targetFile);
		if(!file.exists()){
			file.createNewFile();
			file = new File(targetFile);
		}
		//generate
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
		return writer;
	}
	/**
	 * 
	 * @param targetFile
	 * @param encoding
	 * @return
	 * @throws IOException
	 */
	public static BufferedWriter getBufferedWriter_UTF8(String targetFile) throws IOException{
		//check exist
		File file = new File(targetFile);
		if(!file.exists()){
			file.createNewFile();
			file = new File(targetFile);
		}
		//generate
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), DEFAULT_ENCODING));
		return writer;
	}

}
