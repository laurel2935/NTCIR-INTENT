package org.archive.nlp.htmlparser.pk;

public class Symbol {
	static public String jump[]  ={"select",                //���������ı��,<>..</>,���ױ��֮������ݿ��Ժ���
		                    "script","h","ul",
		                    "marquee","object",
		                    "TEXTAREA","style"}; 
	static public String ignore[]={"/p","li","/li","br","form","/form","param","/img","strong","/strong",
							"span","/span",
							"font","/font","b","/b",
		                    "p","img","hr","!","input","?"};//������Ժ��Եı��<br><p></p>
	
	static public String remove[]={"&.+?;","&#[0-9]+;?","��"};    //д������ʽ,�˴�ɾ������&...; ��&#890;���͵��ַ���
	//static String ignore1[]={"li","br","form","param","strong","span","font","b","p","img","hr","!","input","?"};//������Ժ��Եı��<br><p></p>
}
