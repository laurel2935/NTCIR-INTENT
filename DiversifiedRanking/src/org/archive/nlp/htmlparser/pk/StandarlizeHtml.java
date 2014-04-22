package org.archive.nlp.htmlparser.pk;

import java.io.File;

/**
 * ����html��ҳ�����ļ�������С������Ԥ����������ҳ���迼�ǣ����С�ȣ�
 * @author T
 *
 */
public class StandarlizeHtml {
	/**ͳ�Ƽ���*/
	private static int okNumber = 0;	
	
	/**
	 * ��Ŀ¼�µ�����html�ĳ�������
	 * @param file
	 */
	public  static void checkHtmlsCh(File file){
		if(null == file){
			System.out.println("null file");
			return;
		}
		if(file.isDirectory()){
			File [] subFiles = file.listFiles();
			for(int i=0; i<subFiles.length; i++){
				checkHtmlsCh(subFiles[i]);
			}
		}else{
			if(file.isFile()){
				if(checkHtmlCh(file)){
					okNumber ++;
				}
			}			
		}
	}
	/**
	 * �Ե���Ӣ��html�ĳ�������
	 * @param file
	 * @return
	 */
	public static boolean checkHtmlEn(File file){
		if(checkSizeCh(file) && checkFileNameCh(file)){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * �Ե�������html�ĳ�������
	 * @param file
	 * @return
	 */
	public static boolean checkHtmlCh(File file){
		if(checkSizeCh(file) && checkFileNameCh(file)){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * ������html��С�и��������ƣ������ٶ���ȡ���ı���С��������
	 * @param file
	 * @return
	 */
	public static boolean checkSizeCh(File file){		
		if(((double)file.length()/1000) > 10){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * ��������html�ļ������ˣ���Ҫ����content��������content_4557735_8.htm
	 * @param file
	 * @return
	 */
	public static boolean checkFileNameCh(File file){
		boolean single = false;
		boolean contentStyle = false;
		String name = file.getName();
		if(name.indexOf("content") != -1){
			contentStyle = true;
		}
		if(name.indexOf("_") == name.lastIndexOf("_")){
			single = true;
		}
		if(contentStyle && single){
			return true;
		}else{
			return false;
		}
	}
	
	//test
	public void filterHtml(){
		try{
			File file = new File("D:/content_10656445_1.htm");
			//FileInputStream fis = new FileInputStream(file);
			System.out.println(file.length());
			//System.out.println((double)fis.available()/1000);
			if(checkSizeCh(file)){
				System.out.println("size ok");
			}else{
				System.out.println("size not ok");
			}
			if(checkFileNameCh(file)){
				System.out.println("filename ok");
			}else{
				System.out.println("filename not ok");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//>>>>>>> 1.2
	
	public static void main(String args[]){
		StandarlizeHtml standard = new StandarlizeHtml();
		//standard.filterHtml();
		String directory = "";
		standard.checkHtmlsCh(new File(directory));
		System.out.println("ok number:		" + okNumber);
	}

}
