package org.archive.nlp.tokenizer.ictclas;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class ICTCLAS2014 {
	//
	private static final boolean DEBUG = false;
	
	public static boolean CONFIGED = false;

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
			System.out.println("ok��");
		}
		//
		CONFIGED = true;
	}
	
	public static void exitConfig(){
		try {
			CLibrary.Instance.NLPIR_Exit();
			CONFIGED = false;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	//interface
	public static ArrayList<String> segment(String text){		
		if(!CONFIGED){
			iniConfig();
			//CONFIGED = true;
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
			System.err.println("��ʼ��ʧ�ܣ�");
			return;
		}

		String sInput = "��Ϥ���ʼ��ܾ��ѽ������й�����ٴ�ͨ��������Ҫ��������ǿ���仪���׵Ĳ�����Դ�����估�ִ��Ȼ��ڵĹܿش�ʩ����Ч�����仪���ױ�δ���ҹ�ũҵ����ȫ��������׼��ת����Ʒϵ��Ⱦ��";

		String nativeBytes = null;
		try {
			int bPOSTagged = 0;
			
			nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, bPOSTagged);			

			System.out.println("�ִʽ��Ϊ�� " + nativeBytes);
			
			CLibrary.Instance.NLPIR_AddUserWord("Ҫ��������ǿ���� n");
			CLibrary.Instance.NLPIR_AddUserWord("�����׵Ĳ�����Դ n");
			nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, 1);
			System.out.println("�����û��ʵ��ִʽ��Ϊ�� " + nativeBytes);
			
			CLibrary.Instance.NLPIR_DelUsrWord("Ҫ��������ǿ����");
			nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, 1);
			System.out.println("ɾ���û��ʵ��ִʽ��Ϊ�� " + nativeBytes);
			
			
			int nCountKey = 0;
			String nativeByte = CLibrary.Instance.NLPIR_GetKeyWords(sInput, 10,false);

			System.out.print("�ؼ�����ȡ����ǣ�" + nativeByte);


			CLibrary.Instance.NLPIR_Exit();

		} catch (Exception ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		
		//1
		ICTCLAS2014.check();
		
		//2 adobe����Դ���뱻��
		//System.out.println(ICTCLAS2014.segment("adobe����Դ���뱻��"));

	}
}

