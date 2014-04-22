package org.archive.nlp.htmlparser.pk;

public class Node{
	String  content;
	public String  tag;
	boolean hasContent;
	boolean isLeaf;
	boolean isBlock;                //��������п�ϲ���ɺ����ڱ����ÿ��Ƿ��������Ҫ�Ŀ�
	Node    parent;
	public int endpos;
	public Node(){
		content    ="";
		hasContent =false;
		parent     =null;
		isLeaf     =true;
	}
	/**
	 * @param content  ����
	 * @param tag      ���
	 * @param parent   ���ڵ�
	 * */
	public Node(String content,String tag,Node parent){
		this.content=content;
		this.tag=tag;
		if (content.equalsIgnoreCase(""))
			hasContent=false;
		else 
			hasContent=true;
		this.parent=parent;
		isLeaf=true;
	}
	public void addContent(String str){
		content+=str;
		//content+=str.trim();
		if (content.equalsIgnoreCase(""))
			hasContent=false;
		else 
			hasContent=true;
	}
	/**
	 * cwirf�����ã�Ϊһ�������ʼλ������кź�λ��
	 * */
	public void addContent2(String str,int pos){
		if(!hasContent&&!str.equalsIgnoreCase("")){
		    content+="<"+pos+">"+str;
		    hasContent=true;
		}
		else
			content+=str;
	}
	/**
	 * cwirf �����ã�Ϊһ������ӽ���λ��
	 * */
	public void addEndTag(int pos){
		if(hasContent)
			content+="<"+pos+">";
	}
	public void addEndTag(){
		if(hasContent){
			content+="<"+endpos+">";
			endpos=0;
		}
	}
	/**
	 * ���øýڵ���Ҷ�ӽڵ�
	 * */
	public void setLeaf(boolean is){
		isLeaf=is;
	}
	/**
	 * ���øýڵ㣬��list���ǲ���һ��block<p>
	 * ���������Ҫ����isBlockΪtrue�Ľڵ㡣
	 * */
	public void setBlock(boolean is){
		isBlock=is;
	}
	public String toString(){
		return content;
	}
	
}
