package org.archive.nlp.chunk.lpt.addon;

public class LTPRelationTag {
	
	//��ϵ����	Tag	Description	Example
	//��ν��ϵ	
	public final static String SBV ="SBV"; //subject-verb	������һ���� (�� <-- ��)
	//������ϵ	
	public final static String VOB ="VOB"; //ֱ�ӱ��verb-object	������һ���� (�� --> ��)
	//�����ϵ	
	public final static String IOB ="IOB"; //��ӱ��indirect-object	������һ���� (�� --> ��)
	//ǰ�ñ���	
	public final static String FOB ="FOB"; //ǰ�ñ��fronting-object ��ʲô�鶼�� (�� <-- ��)
	//����	
	public final static String DBL ="DBL"; //double	�����ҳԷ� (�� --> ��)
	//���й�ϵ	
	public final static String ATT ="ATT"; //attribute	��ƻ�� (�� <-- ƻ��)
	//״�нṹ	
	public final static String ADV ="ADV"; //adverbial	�ǳ����� (�ǳ� <-- ����)
	//�����ṹ	
	public final static String CMP ="CMP"; //complement	��������ҵ (�� --> ��)
	//���й�ϵ	
	public final static String COO ="COO"; //coordinate	��ɽ�ʹ� (��ɽ --> ��)
	//�����ϵ	
	public final static String POB ="POB"; //preposition-object	��ó������ (�� --> ��)
	//�󸽼ӹ�ϵ	
	public final static String LAD ="LAD"; //left adjunct	��ɽ�ʹ� (�� <-- ��)
	//�Ҹ��ӹ�ϵ	
	public final static String RAD ="RAD"; //right adjunct	������ (���� --> ��)
	//�����ṹ	
	public final static String IS ="IS"; //independent structure	���������ڽṹ�ϱ˴˶���
	//���Ĺ�ϵ	
	public final static String HED ="HED"; //head	ָ�������ӵĺ���

}
