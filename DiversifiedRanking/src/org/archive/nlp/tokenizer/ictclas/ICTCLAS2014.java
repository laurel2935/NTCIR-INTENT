package org.archive.nlp.tokenizer.ictclas;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class ICTCLAS2014 {
	//
	private static final boolean DEBUG = false;
	
	private static boolean CONFIGED = false;

	//
	public interface CLibrary extends Library {
		//
		CLibrary Instance = (CLibrary) Native.loadLibrary(
				"./ICTCLAS/NLPIR", CLibrary.class);
		
		public int NLPIR_Init(String sDataPath, int encoding,
				String sLicenceCode);
				
		public String NLPIR_ParagraphProcess(String sSrc, int bPOSTagged);

		public String NLPIR_GetKeyWords(String sLine, int nMaxKeyLimit,
				boolean bWeightOut);
		public int NLPIR_AddUserWord(String sWord);//add by qp 2008.11.10
		public int NLPIR_DelUsrWord(String sWord);//add by qp 2008.11.10
		
		public void NLPIR_Exit();
	}

	public static String transString(String aidString, String ori_encoding, String new_encoding) {
		try {
			return new String(aidString.getBytes(ori_encoding), new_encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void iniConfig(){
		String _DataDir = "ICTCLAS/";
		// String system_charset = "GBK";//GBK----0
		//String system_charset = "UTF-8";
		int charset_type = 1;
		
		int init_flag = CLibrary.Instance.NLPIR_Init(_DataDir, charset_type, "0");

		if (0 == init_flag) {
			System.err.println("error!");
			return;
		}else{
			System.out.println("ok！");
		}
	}
	
	public static void exitConfig(){
		try {
			CLibrary.Instance.NLPIR_Exit();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	//interface
	public static ArrayList<String> segment(String text){		
		if(!CONFIGED){
			iniConfig();
			CONFIGED = true;
		}
		//
		ArrayList<String> wordList = new ArrayList<String>();
		String sentence = ICTCLAS2014.CLibrary.Instance.NLPIR_ParagraphProcess(text, 0);
		String [] array = sentence.split(" ");
		for(int i=0; i<array.length; i++){
			wordList.add(array[i]);
		}
		if(DEBUG){
			System.out.println(wordList);
		}
		return wordList.size()>0? wordList:null;		
	}
	
	public static void check(){
		String _DataDir = "ICTCLAS/";
		// String system_charset = "GBK";//GBK----0
		String system_charset = "UTF-8";
		int charset_type = 1;
		
		int init_flag = CLibrary.Instance.NLPIR_Init(_DataDir, charset_type, "0");

		if (0 == init_flag) {
			System.err.println("锟斤拷始锟斤拷失锟杰ｏ拷");
			return;
		}

		String sInput = "锟斤拷悉锟斤拷锟绞硷拷锟杰撅拷锟窖斤拷锟斤拷锟斤拷锟叫癸拷锟斤拷锟斤拷俅锟酵�锟斤拷锟斤拷锟斤拷锟斤拷要锟斤拷锟斤拷锟斤拷锟斤拷强锟斤拷锟戒华锟斤拷锟阶的诧拷锟斤拷锟斤拷源锟斤拷锟斤拷锟戒及锟街达拷锟饺伙拷锟节的管控达拷施锟斤拷锟斤拷效锟斤拷锟斤拷锟戒华锟斤拷锟阶憋拷未锟斤拷锟揭癸拷农业锟斤拷锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷准锟斤拷转锟斤拷锟斤拷品系锟斤拷染锟斤拷";

		String nativeBytes = null;
		try {
			int bPOSTagged = 0;
			
			nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, bPOSTagged);			

			System.out.println("锟街词斤拷锟轿�锟斤拷 " + nativeBytes);
			
			CLibrary.Instance.NLPIR_AddUserWord("要锟斤拷锟斤拷锟斤拷锟斤拷强锟斤拷锟斤拷 n");
			CLibrary.Instance.NLPIR_AddUserWord("锟斤拷锟斤拷锟阶的诧拷锟斤拷锟斤拷源 n");
			nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, 1);
			System.out.println("锟斤拷锟斤拷锟矫伙拷锟绞碉拷锟街词斤拷锟轿�锟斤拷 " + nativeBytes);
			
			CLibrary.Instance.NLPIR_DelUsrWord("要锟斤拷锟斤拷锟斤拷锟斤拷强锟斤拷锟斤拷");
			nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, 1);
			System.out.println("删锟斤拷锟矫伙拷锟绞碉拷锟街词斤拷锟轿�锟斤拷 " + nativeBytes);
			
			
			int nCountKey = 0;
			String nativeByte = CLibrary.Instance.NLPIR_GetKeyWords(sInput, 10,false);

			System.out.print("锟截硷拷锟斤拷锟斤拷取锟斤拷锟斤拷牵锟�" + nativeByte);


			CLibrary.Instance.NLPIR_Exit();

		} catch (Exception ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		
		//1
		//ICTCLAS2014.check();
		
		//2
		ICTCLAS2014.segment("中华");

	}
}

