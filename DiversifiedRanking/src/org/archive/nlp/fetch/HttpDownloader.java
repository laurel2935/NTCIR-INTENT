package org.archive.nlp.fetch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpRetryException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;


/**
 * 网页下载类，含有重试错误处理的实现
 * 
 */
class HttpMethodRetryHandlerImpl implements HttpMethodRetryHandler
{
	/**
	 * 重试次数
	 */
	int	m_retryTimes = 5;

	/**
	 * 重试时间间隔
	 */
	int	m_retryInterval	= 5000;
	
	public void setRetryTimes(int times){
		this.m_retryTimes = times;
	}
	public int getRetryTimes(){
		return this.m_retryTimes;
	}
	public void setRetryInterval(int interval){
		this.m_retryInterval = interval;
	}
	public int getRetryInterval(){
		return this.m_retryInterval;
	}
	/**
	 * 重试接口
	 */
	public boolean retryMethod(HttpMethod method, IOException exception,
			int executionCount)
	{
		// 重试次数过多
		if (executionCount > m_retryTimes)
			return false;

		boolean ret = false;
		// 可以重新链接的错误
		if (ConnectException.class.isInstance(exception) == true
				|| HttpRetryException.class.isInstance(exception) == true
				|| NoRouteToHostException.class.isInstance(exception) == true
				|| ProtocolException.class.isInstance(exception) == true
				|| SocketException.class.isInstance(exception) == true
				|| SocketTimeoutException.class.isInstance(exception) == true)
		{
			ret = true;
		}
		if (ret == false)
			return false;
		// 等待一个时间间隔
		try
		{
			Thread.sleep(m_retryInterval);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		// 重试
		return true;
	}
}
/**
 * 获取查询URL对应的返回页面
 * @author T
 *
 */
public class HttpDownloader {
	// search url
	private String urlStr;
	//http重试错误处理
	private static HttpMethodRetryHandlerImpl retryHandler = new HttpMethodRetryHandlerImpl();
	
	/////////////////////////////////////////
	/**
	 * 设定 search url
	 */
	public void setUrlStr(String url){
		this.urlStr = url;
	}
	public String getUrlStr(){
		return this.urlStr;
	}
	/**
	 * 注：输入的queryUrl如果含有中文词需要预先用－URLEncoder　encode();
	 * @param queryUrl
	 * @return
	 */
	public StringBuffer getContent(String queryUrl){
		StringBuffer result = null;
		setUrlStr(queryUrl);
		HttpClient httpClient = new HttpClient();
		//设置 Http 连接超时为5秒
		//httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
		//httpClient.getHttpConnectionManager().getParams().se
		
		GetMethod getMethod = new GetMethod(getUrlStr());
		//设置 get 请求超时为 5 秒
		//getMethod.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, 5000);
		//设置请求重试处理，重试处理：请求5次,间隔为 5 秒
		getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);
		
		try{
			int statusCode = httpClient.executeMethod(getMethod);
			if(statusCode != HttpStatus.SC_OK){
				System.err.println("Method failed: " + getMethod.getStatusLine());
			}else{
				BufferedReader reader = new BufferedReader(new InputStreamReader(getMethod.getResponseBodyAsStream()));
				String line;
				result = new StringBuffer();
				while(null != (line=reader.readLine())){
					result.append(line);
				}
				reader.close();
			}		
		}catch(HttpException e){
			// 发生致命的异常，可能是协议不对或者返回的内容有问题
			System.out.println("Please check your provided http address!");
			e.printStackTrace();
		}catch(IOException e){
			// 发生网络异常  
			System.out.println("发生网络异常!");
			e.printStackTrace();
		}finally{
			/*6 .释放连接*/   
			getMethod.releaseConnection(); 
		}
		return result;
	}
	//////////////////////////////test
	/*
	 *存在：需要将汉字编码的问题，有可能被google翻译为繁体字；
	 * */
	public static void main(String [] args){
		try{
			//System.out.println(queryUrl);
			//System.out.println(URLEncoder.encode(queryUrl, "utf-8"));
			
			StringBuffer buffer;
			String oov = "american online";
			String encodedOOV = URLEncoder.encode(oov, "utf-8");
			String queryUrl = "http://www.google.com/search?hl=zh-CN&as_epq="+encodedOOV+"&num=10&start=0";
			//System.out.println(queryUrl);
			
			
			//String another = "http://www.google.cn/search?hl=zh-CN&as_epq=\"%E7%BE%8E%E5%9B%BD%E7%BA%BF%E4%B8%8A\"&q=America&num=100&start=0";
			HttpDownloader downLoader = new HttpDownloader();
			buffer = downLoader.getContent(queryUrl);
			System.out.println(buffer.toString());
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
}

