package org.archive.util.format;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class StandardFormat {	
	//serial format for files
	private static DecimalFormat fileSerialFormat = null;
	
	//output format for double numbers
	private static DecimalFormat doubleOutputFormat = null;
	
	//
	
	/**
	 * time format
	 * @param date
	 * @param sourceFormat
	 * @param targetFormat
	 * @return
	 */
	public static String toStandardFormat(String date, 
			String sourceFormat, String targetFormat){		
		try{ 			
	          SimpleDateFormat sFormat = new SimpleDateFormat(sourceFormat);   
	          SimpleDateFormat tFormat = new SimpleDateFormat(targetFormat);
	          Date sDate = sFormat.parse(date);   
	          String time = tFormat.format(sDate);	          
	          return time;
	      }catch(Exception   ex){   
	          ex.printStackTrace();   
	      } 
	      return null;
	}
	/**
	 * 取得年份
	 * @param date
	 * @param foramt
	 * @return
	 */
	public static String getYear(String date, String foramt){
		try{
			SimpleDateFormat Format = new SimpleDateFormat(foramt);
	        Date sDate = Format.parse(date);
	        GregorianCalendar gc =new GregorianCalendar();
	        gc.setTime(sDate);
	        
	        return Integer.toString(gc.get(GregorianCalendar.YEAR));
		}catch(Exception e){
			e.printStackTrace();			
		}
		return null;
	}	
	/**
	 * 序列号命名格式
	 * @param num
	 * @param pattern "0000"
	 * @return
	 */
	public static String serialFormat(int num, String formatPattern){		
		if(null == fileSerialFormat){
			fileSerialFormat = new DecimalFormat(formatPattern);
		}
		//
		return fileSerialFormat.format(num);
	}
	/**
	 * @param target
	 * @param formatPattern "#.####""
	 * **/
	public static String toDoubleOutputFormat(double target, String formatPattern){
		if(Double.isNaN(target)){
			return Double.toString(target);
		}else{
			if(null == doubleOutputFormat){
				doubleOutputFormat = new DecimalFormat(formatPattern);
			}
			//
			return doubleOutputFormat.format(target);
		}		
	}
}

