package org.archive.nlp.htmlparser.pk;

import java.util.LinkedList;
/**
 * �洢HTML�ĵ������ṹ
 * */
public class HTree {
	private LinkedList<Node> list;
	private LinkedList<Node> leafList;
	public HTree(){
		list=new LinkedList<Node>();
		leafList=new LinkedList<Node>();
	}
	/**
	 * ����һ���ڵ�
	 * */
	public void insert(Node node){
		list.add(node);
	}
	/**
	 * ���뵽Ҷ�ӽڵ�
	 * */
	private void insertLeaf(Node node){
		leafList.add(node);
	}
	/**
	 * ��ӡ��
	 * */
	public void print(){
		int len=list.size();
		String str;
		Node node;
		int k=0;
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			str=node.content.trim();
			if (str.equals(""))continue;
			System.out.println(k+++":"+str);
		}
	}
	/**
	 * ��ӡ��
	 * */
	public void print2(){
		int len=list.size();
		String str;
		Node node;
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			if (!node.isBlock)continue;
			System.out.println(node.content);
		}
	}
	
	/**
	 * ����������ı������������ı����ݱ�
	 * float��32λ��double��64λ�� 
	 * ���Ǹ����͵��Ǳ�ʾ��Χ�ǲ�һ���ģ�ת����ʱ��Ȼ����ʾ������ʧ����Ȼ������������������ж��ǲ�����ġ�
	 * ���㲻����ʱ��Ĭ��Ϊdouble��
	 */
	public double contentRatio(){
		int linkContent = 0, nonLinkContent = 0;
		int len = list.size();
		Node node;
		String str, del;
		
		for(int i=len - 1; i>=0; i--){
			node = list.get(i);
			str = node.content.trim();
			if(str.equals(""))
				continue;
			if(node.tag.equals("a")){
				//System.out.println(str);
				del=str.replaceAll("<[0-9]*>", "");
				linkContent+=del.getBytes().length;
				//System.out.println(del);
			}else{
				del=str.replaceAll("<[0-9]*>", "");
				nonLinkContent+=del.getBytes().length;
				//System.out.println(del);
			}
		}
		if(nonLinkContent!=0 && linkContent!=0){
			//System.out.println(nonLinkContent);
			//System.out.println(linkContent);
			return (0.1*nonLinkContent)/(0.1*linkContent);
		}
		return 0;		
	}
	
	
	/**
	 * �ϲ��飬���ڸ��������ݶ��ӿ���Ҳ�����ݵĺϲ��ӿ鵽����<p>
	 * �Ե����Ϻϲ�
	 * */
	public void mergee(){
		Node node,next,curr;
		String str,temp;
		int len=list.size();
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			if (!node.isLeaf)
				continue;
			curr=node;
			str=curr.content;
			for(;curr!=null;){
				next=curr.parent;
				if (next!=null){
					if (next.hasContent){      //������ڵ�������
						temp=next.content;
						next.addContent(str);
						next.setLeaf(true);
						node.setBlock(false);
						break;
					}
					else
						next.setLeaf(false);
					curr=next;
				}
				else
					curr=null;
			}
			if (curr==null){
				node.setBlock(true);
			}
		}
	}
	/**
	 * Ҷ�ӽڵ�ĸ��ڵ������������ݣ������Ϻϲ���ֱ�����ڵ�������
	 * */
	public void merge2(){
		Node node,next,curr;
		String str,temp;
		int len=list.size();
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			if (!node.isLeaf)
				continue;
			curr=node;
			str=curr.content;
			for(;curr!=null;){
				next=curr.parent;
				if (next!=null){
					if (next.hasContent){      //������ڵ�������
						temp=next.content;
						next.addContent(str);
						next.setLeaf(true);
						node.setBlock(false);
						//break;
					}
					else{
						next.setLeaf(false);
						node.setBlock(true);
					}
					break;
				}
				else
					curr=null;
			}
			if (curr==null){
				node.setBlock(true);
			}
		}
	}
	/**
	 * �������ݿ�
	 * <p>���û�����ݿ� ����null
	 * */
	public String[] getBlock(){
		String[] contBlock;
		String[] res;
		int len=list.size();
		int num=0;
		String str;
		Node node;
		//for(int i=len-1;i>=0;i--){
		//	node=list.get(i);
		//	str=node.content.trim();
		//	if (str.equals(""))continue;
		//	num++;
		//}
		//num=len;
		contBlock=new String[len];
		//if (num<=0) return null;
		//num=0;
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			str=node.content.trim();
			if (str.equals(""))continue;
			contBlock[num++]=str;
		}
		res=new String[num];
		System.arraycopy(contBlock,0,res,0,num);
		return res;
		
	}
	/**
	 * ��ÿ�����ݿ� ���н�����־����
	 * */
	public void repair(){
		int len=list.size();
		Node node;
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			if (node.hasContent&&node.endpos!=0)
			    node.content+="<"+node.endpos+">";;
		}
	}
}

