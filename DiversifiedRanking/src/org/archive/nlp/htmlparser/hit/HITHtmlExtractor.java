package org.archive.nlp.htmlparser.hit;
/**
 * @author Xin Chen
 * Created on 2009-11-11
 * Updated on 2010-08-09
 * Email:  xchen@ir.hit.edu.cn
 * Blog:   http://hi.baidu.com/����ͬ��_����
 */
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.mozilla.intl.chardet.nsPSMDetector;


/**
 * TextExtractor���ܲ�����.
 */

public class HITHtmlExtractor {
	
	private TextExtract textExtract = new TextExtract();
	
	//
	private static String detCharset = null;
	private static Pattern pGB2312 = Pattern.compile("GB2312", Pattern.CASE_INSENSITIVE);
	private static Pattern pUTF8 = Pattern.compile("(UTF8)|(UTF-8)", Pattern.CASE_INSENSITIVE);
	
	private String getHTML(String strURL) throws IOException {
		URL url = new URL(strURL);
		BufferedInputStream in =  new BufferedInputStream(url.openStream());
		byte[] bytes = new byte[1024000];
		int len = -1;
		int pos = 0;
		while ((len = in.read(bytes, pos, bytes.length - pos)) != -1) {
			pos += len;
		}
		
		detectCharset(bytes);
		
		String html = null;
		if (detCharset != null)
		{
			html = new String(bytes, 0, pos, this.detCharset);
		}
		else
		{
			return new String(bytes, 0, pos);
		}
		System.out.println("Detcharset = " + detCharset);
		return html;
	}
	
	private String getHTML_A1(String localHtml) throws IOException {
		
		BufferedInputStream in =  new BufferedInputStream(new FileInputStream(new File(localHtml)));
		
		byte[] bytes = new byte[1024000];
		int len = -1;
		int pos = 0;
		while ((len = in.read(bytes, pos, bytes.length - pos)) != -1) {
			pos += len;
		}
		
		detectCharset(bytes);
		
		String html = null;
		if (detCharset != null)
		{
			html = new String(bytes, 0, pos, this.detCharset);
		}
		else
		{
			return new String(bytes, 0, pos);
		}
		//System.out.println("Detcharset = " + detCharset);
		return html;
	}
	
	private void detectCharset(byte[] content)
	{
		String html = new String(content); 
		Matcher m = pGB2312.matcher(html);
		if (m.find())
		{
			detCharset = "gb2312";
			return ;
		}
		m = pUTF8.matcher(html);
		if (m.find())
		{
			detCharset = "utf-8";
			return;
		}
		
		int lang = nsPSMDetector.ALL;
		nsDetector det = new nsDetector(lang);
		det.Init(new nsICharsetDetectionObserver() {
			public void Notify(String charset) {
				detCharset = charset;
			} 
		});
		boolean isAscii = true;

		if (isAscii)
			isAscii = det.isAscii(content, content.length);

		if (!isAscii)
			det.DoIt(content, content.length, false);

		det.DataEnd();

		boolean found = false;
		if (isAscii) {
			this.detCharset = "US-ASCII";
			found = true;
		}

		if (!found && detCharset == null) {
			detCharset = det.getProbableCharsets()[0];
		}
	}
	
	public String extractFromHtml (String localHtmlUrl) throws IOException{
		String htmlString = this.getHTML_A1(localHtmlUrl);
		if(htmlString.length() > 10){
			return textExtract.parse(htmlString);
		}else {
			return null;
		}		
	}
	
	public static void main(String[] args) throws IOException {
		
		/* 
		 * ������վ��
		 * �ٶȲ��Ϳռ�             http://hi.baidu.com/liyanhong/
		 * ��������������������Ϣ	http://ent.sina.com.cn/music/roll.html
		 * ��Ѷ������������Ϣ		http://ent.qq.com/m_news/mnews.htm
		 * �Ѻ���������				http://music.sohu.com/news.shtml
		 * ��������ҵ��ѧУ����Ϣ�� http://today.hit.edu.cn/
		 * ��������ҵ��ѧУ�������� http://news.hit.edu.cn/
		 */


		/* ע�⣺����ֻΪչʾ��ȡЧ������������ҳ�������⣬getHTMLֻ�ܽ���GBK�������ҳ�������������� */
		//String content = new UseDemo().getHTML(args[0]);
		//String htm = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/doc/top-100/0201/a2a6db21a7a5d8ba-1368c369d8c38c60.htm";
		String htm_2 = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/doc/top-100/0004/1f86a7e6678bf8a1-071d026ec8f3fbc0.htm";
		String content = new HITHtmlExtractor().getHTML_A1(htm_2);

		// http://ent.sina.com.cn/y/2010-04-18/08332932833.shtml
		// http://ent.qq.com/a/20100416/000208.htm
		// http://ent.sina.com.cn/y/2010-04-18/15432932937.shtml
		// http://ent.qq.com/a/20100417/000119.htm
		// http://news.hit.edu.cn/articles/2010/04-12/04093006.htm
	

		/* 
		 * ������ȡ����ҳ�����������ɿ�����ű���δ�޳�ʱ��ֻҪ�������ֵ���ɡ�
		 * �෴������Ҫ��ȡ���������ݳ��Ƚ϶̣�����ֻ��һ�仰�����ţ����С����ֵ���ɡ�
		 * ��ֵ����׼ȷ���������ٻ����½���ֵ��С��������󣬵����Ա�֤�鵽ֻ��һ�仰������ 
		 */
		//TextExtract.setThreshold(76); // Ĭ��ֵ86

		System.out.println("got html");
		String html = new TextExtract().parse(content);
		
		System.out.println(html);
	}
}