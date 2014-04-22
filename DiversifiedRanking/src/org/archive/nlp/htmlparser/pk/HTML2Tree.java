package org.archive.nlp.htmlparser.pk;

import java.util.regex.*;

public class HTML2Tree {
	private Stack<Node> myStack;               //�������Ĺ����еĸ���ջ
	private HTree tree;
	public HTML2Tree(){
		tree=new HTree();
		myStack=new Stack<Node>();
	}
	public void emptyTree(){
		tree=new HTree();
	}
	public void main(StringBuffer sb){
		String s="",tag="",content,tmp="";
		int pos=0,start=0,end;                  //start��end����ȡ���ݵ���ʼ��ֹλ��
		int num=0,position=0;
		int status;                             //0 ��ʾ��ʼ״̬������body���
		                                        //1 ��ʾ����һ��< ��� 
		                                        //2 ��ʾ�� >���
		                                        //3 ��ʾ������Ǻ��������ݣ���<stript></stript>
		                                        //4 ��ʾ��ȡ�������֮�������
		
		boolean getNewLine;                     //��ȡ������
		int posByteLen=0;                       //cwirf�������õ��ֽ���
		status=0;
		getNewLine=true;
		insert("html"); /* ������ڵ� */
		String[] lines;
		//sb=delNestTag(sb);
		lines=sb.toString().split("\\n");
		int index=0;
		boolean hasBegin=false;
		while(true){    
			if (getNewLine){
				if(lines.length == 0)
					break;
		   	    s=lines[index++];
		   	    tmp=s;
		   	    num++;
		   	    //�Ĺ���ԭ��Ϊ����
		   	    if (index>=lines.length) break;
		   	    
		   	    if(hasBegin){
		   	    	position+=(s.getBytes()).length+1;
		   	    	//System.out.println(s);
		   	    	//tmp=(myStack.getTop()).content;
		   	    	//System.out.println(tmp);
		   	    	//tmp=(myStack.getTop()).tag;
		   	    	//System.out.println(position);
		        }
		   	    else if(s.trim().equals(""))
		   	    	hasBegin=true;
		   	    if(num==75){
		   	    	//System.out.println(s);
		   	    	//tmp=(myStack.getTop()).content;
		   	    	//System.out.println(tmp);
		   	    	
		   	    }
		        if (s.equalsIgnoreCase(""))
		        	continue;
		        //s=s.toLowerCase();
		        s=s.replaceAll("</p>|<p>|<br>","\n");
		        pos=0;
		        //System.out.println(s);
		    }
		    if (status==0){                // �ҵ�body��ʼ��λ��
		    	pos=s.indexOf("<body");
		    	if (pos<0) {
		    		getNewLine=true;
		    		continue;
		    	}
		    	insert("body");
		    	status=1;
		    	pos++;
		    	getNewLine=false;
		    }
		    else if(status==1){            // ����һ��< ���
		    	pos=s.indexOf("<",pos);         
		    	if (pos<0) {               //û�ҵ���ת����һ��
		    		getNewLine=true;
		    		pos=0;
		    		continue;
		    	}
		    	tag=getTag(s,pos+1);
		        if (tag!=null){            //�ҵ��˱��
		        	if (isEndTag(tag)){    //�ǽ������
		        	    if (mayIgnor(tag)){//�ǿ��Ժ��Եı��
		        	        status=2;
		        	        getNewLine=false;
		        	        continue;
		        	    }
		        		if (match(tag)){   //�뵱ǰջ��ƥ��ı��
		        		    //if((myStack.getTop()).hasContent)
		        			//(myStack.getTop()).addEndTag(position-(tmp.substring(pos).getBytes()).length-1);
		        		    //(myStack.getTop()).addEndTag(position);
		        			//ע�͵�
		        			//(myStack.getTop()).addEndTag();
		        			myStack.pop();
		        		    if (myStack.empty())
		        		    	break;
		        	        pos++;
		        	        getNewLine=false;
		        	        status=4;
		        	        start=s.indexOf(">",pos);
		        	        if (start<0){
		        	        	getNewLine=true;
		        	        	pos=0;
		        	        	status=1;
		        	        	continue;
		        	        }
		        	        else 
		        	        	start+=1;
		        	        pos=start;
		        	        continue;
		        	    }
		        	    else{              //��ƥ���ʾ��ҳ�д��󣬵����ɺ��Եı��
		        	    	//System.out.println(num);
		        	    	//myStack.print();
		        	    	//System.out.println();
		        	    	status=2;
		        	    	getNewLine=false;
		        	    	continue;
		        	    }
		        	}
		        	else{                  //���ǽ������
		        		if (mayIgnor(tag.trim())){//�ǿ��Ժ��Եı��
		        	        status=2;
		        	        getNewLine=false;
		        	        continue;
		        		}
		        	    else{              //�����Ժ��Եı��    
		        	    	getNewLine=false;
		        	    	if (!isJump(tag)){
		        	    		status=2;
		        	    		insert(tag);
		        	    	}
		        	    	else{          //���������ı��
		        	    		status=3;
		        	    	}
		        	    }
		        	}
		        }
		        else {
		        	getNewLine=true;
		        }
		        continue;
		    }
		    else if (status==2){           /* ��>���*/
		    	if(tag.indexOf("!")>=0){
		    		start=s.indexOf("-->",pos);
		    		if (start<0){              //��ǰ��û�ҵ������ȡ��һ��
			    		getNewLine=true;
			    		pos=0;
			    		continue;
			    	}
		    		start+=3;
		    	}else{
		    		start=s.indexOf(">",pos);
		    		if (start<0){              //��ǰ��û�ҵ������ȡ��һ��
		    			getNewLine=true;
		    			pos=0;
		    			continue;
		    		}
		    		start++;
		    	}
		    	//posByteLen=(s.substring(start).getBytes()).length;
		    	status=4;
		    	getNewLine=false;
		    }
		    else if (status==3){           /* ��������</...>���*/
		    	pos=s.indexOf("</"+tag+">",pos);
		    	if (pos<0){
		    		getNewLine=true;
		    		pos=0;
		    		continue;
		    	}
		    	pos=s.indexOf(">",pos);
		    	pos++;
		    	//posByteLen=(s.substring(pos).getBytes()).length;
		    	start=pos;
		    	status=4;
		    	getNewLine=false;
		    	continue;
		    }
		    else if(status==4){            /* ��ȡ�������<> <>֮�������*/
		    	end=s.indexOf("<",start);
		    	if (end<0){                /* �������쵽����һ��*/
		    			//��ӿո�
	                   content=remove(s.substring(start)).trim();
	                   //content=remove(s.substring(start));
	                   if (!content.equalsIgnoreCase("")){
	                      posByteLen=(tmp.substring(start).getBytes()).length;
	                	   (myStack.getTop()).addContent(content+" ");
	                      //(myStack.getTop()).addContent2(content,position-posByteLen-1);
	                      (myStack.getTop()).endpos=position;
	                   }
	                   getNewLine=true;
	                   start=0;
	                   continue;
		    	}
		    	content=s.substring(start,end);
		    	//��ӿո�
		    	content=remove(content).trim();
		    	//content=remove(content);
		    	//posByteLen=(s.substring(end+1).getBytes()).length;
		    	if(!content.equalsIgnoreCase("")){
		    		posByteLen=(tmp.substring(start).getBytes()).length;
		    		(myStack.getTop()).addContent(content+"");
		    		//(myStack.getTop()).addContent2(content,position);
		    	    //(myStack.getTop()).addContent2(content,position-posByteLen-1);
		    	    (myStack.getTop()).endpos=position-(tmp.substring(end).getBytes()).length;
		    	}
                status=1;
                pos=end;
                getNewLine=false;
		    }
		    
		}
		//tree.repair();
	}
	/**
	 * ��ȡ�������
	 * �磬���<html>��������html
	 * @param line ���������
	 * @param pos  <λ��
	 * */
	private String getTag(String line,int pos){
		int end1,end2;
		end1=line.indexOf(">",pos);
		end2=line.indexOf(" ",pos);
		if ((end1<0 && end2>=0))               //<html sad �� <html sdfadfa
			return line.substring(pos,end2);
		if (end1>0 && end2>=0 && end2<end1)    // <html fds>
			return line.substring(pos,end2);
		if (end2<0 && end1>=0)                 // <html>
			return line.substring(pos,end1);
		if (end1<0 && end2<0)                  //�������ʾ����<���ϲ���tag���
			return line.substring(pos);
		if (end1>=0 && end2>=0 && end1<end2)   //��ʾ��<html>����
			return line.substring(pos,end1);
		return line.substring(pos);            //��һ����<html���ͽ��������ǲ�����һ����Ƿ������е����<ht
	}
	/**
	 * �жϵ�ǰ����Ƿ���Ӧ������������
	 * ��, <script>
	 * @param tag ���
	 * */
	private boolean isJump(String tag){
		//String s=(String)myStack.getTop();
		int size=Symbol.jump.length;
		for(int i=0;i<size;i++){
			if (tag.equalsIgnoreCase(Symbol.jump[i]))
			    return true;
		}
		return false;
	}
	/**
	 * ���ҵ���<���ʱ
	 * �жϵ�ǰ����ǿ�ʼ���<html>���ǽ������</html>
	 * */
	private boolean isEndTag(String tag){
		int pos;
		pos=tag.indexOf("/");
		if (pos<0) return false;
		return true;
	}
	/**
	 * ��ǰ������Ǻ�ջ���ı����ͬһ����
	 * */
	private boolean match(String tag){
		String str;
		Node node;
		node=myStack.getTop();
		str=node.tag;
		int pos;
		pos=tag.indexOf(str);
		if (pos<0) return false;
		return true;
	}
	/**
	 * ���������ͬһ����
	 * @param beginTag  ��ʼ���
	 * @param endTag    �������
	 * */
	private boolean match2(String beginTag,String endTag){
		int pos;
		pos=endTag.indexOf(beginTag);
		if (pos<0) return false;
		return true;
	}
	/**
	 * �ǿ��Ժ��Եı����
	 * */
	private boolean mayIgnor(String tag){
		boolean res;
		
		for(int i=0,size=Symbol.ignore.length;i<size;i++){
			res=tag.equalsIgnoreCase(Symbol.ignore[i]);
			if (res) return true;
			if (tag.matches(".*!.+|.*\\?.+")) return true;
		}
		return false;
	}
	/**
	 * �ж��뵱ǰ����δ��������Ƿ�ƥ��
	 * @param  unEndTag   ��ǰ����δ����ʱ�ı��
	 * @param  tag        ��ǰ�Ľ������
	 * */
	private boolean unEndTagMatch(String unEndTag, String tag){
		int pos=tag.indexOf(unEndTag);
		if(pos<0) return false;
		return true;
	}
	/**
	 * ȥ��һ���ַ����е���Symbol.remove�г��ֵ��ַ�
	 * */
	public String remove(String str){
		int i=0;
		String regx="";
		for(;i<Symbol.remove.length-1;i++){
			regx+=Symbol.remove[i]+"|";
		}
		regx+=Symbol.remove[i];                  
		str=str.replaceAll(regx," ");
		return str;
	}
	/**
	 * ջ�Ͳ������������
	 * @param tag    ���
	 * */
	private void insert(String tag){
		Node node;
		if (myStack.empty())
			node=new Node("",tag,null);
		else
			node=new Node("",tag,myStack.getTop());
		myStack.push(node);
		tree.insert(node);
	}
	/**
	 * ���ؽ��õ���
	 * */
	public HTree getTree(){
		return tree;
	}
	/**
	 * ɾ��<script>���֮�������
	 * */
	private static StringBuffer delNestTag(StringBuffer sb){
		String str=sb.toString();
		String regx="(<script((?!<script).)*/script>)";
		Pattern p=Pattern.compile(regx,Pattern.CASE_INSENSITIVE);
		try{
			Matcher m=p.matcher(str);
			if(m.find()){
				str=m.replaceAll("");
			}
		}catch(Exception e){}
		return new StringBuffer(str);
	}
}




