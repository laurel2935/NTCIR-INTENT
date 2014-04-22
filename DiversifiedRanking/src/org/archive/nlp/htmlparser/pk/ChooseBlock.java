package org.archive.nlp.htmlparser.pk;


import java.util.Arrays;

/**
 * ��ѡ�����������ڵĿ�
 * */
public class ChooseBlock {
	private double threshold;            //ȷ��web�Ƿ��������ݵľ�������ֵ
    int[]   sizeBlock;                   //�洢ÿ����Ĵ�С
    boolean[]  staBlock;                 //true��ʾ�ö�Ӧ��block�����ڷ������
	String[]   contBlock;                //���ݿ�
    double err_val;
    
	public ChooseBlock(){
    	sizeBlock =null;
    	staBlock  =null;
    	contBlock =null;
    }
	/**
	 * ��ʼ������
	 * */
	public boolean initiate(HTree tree,double th){
		int len;
		this.threshold=th;
		contBlock=tree.getBlock();
		if (contBlock==null)
			return false;
		sizeBlock=getSizeBlock(contBlock);
		len=sizeBlock.length;
		if (len==0) return false;
		getMaxNoiseBlock(contBlock);           //�Ѹ���������ƽ
		//for(int ii=0;ii<len;ii++){System.out.println(sizeBlock[ii]);}
		staBlock=new boolean[len];             //�������飬�ڼ��㷽��ʱ���ų��ڼ�����Ŀ� ��staBlock����Ϊtrue
		iniStaBlock();
		return true;
	}
	/**
	 * ����web page��������<p>
	 * �������null ��ʾ����������,���ô˺���ǰ�����ȵ���initiate����
	 * */
	public String getContent(){
		int i,len,index,aidIndex,maxIndex=0;
		double val,tmp,max;
		int[] aid;
		double err;
		String str="";
		
		len=sizeBlock.length;
		aid=new int[len];
		val=calError();
		err_val=val;
		//System.out.println(val+"******************");
		
		//System.out.println("val:"+val);
		if (val<threshold) return null;
		i=0;
		max=0;
		while(true){
			index=getIndex2();
			aid[i]=index;
			setBlock(index);
			tmp=val;
			val=calError();
			err=tmp-val;
			if (err>max){
				max=err;
				maxIndex=i;
			}
			if(err>val) break;
			i++;
		}
		if (maxIndex>5) return null;   //������Ϊ �����ݿ�Ӧ������5���Ǻ����
		for(i=0;i<=maxIndex;i++){
			index=aid[i];
			str+=contBlock[index];
		}
		return str;
	}
	/**
	 * ���ؿ�Ĵ�С������
	 * */
	public int[] getSizeBlock(String[] contBlock){
		int len;
		int[] sizeBlock;
		len=contBlock.length;
		sizeBlock=new int[len];
		for(int i=0;i<len;i++){
			sizeBlock[i]=contBlock[i].length();
		}
		//Arrays.sort(sizeBlock);   �˴�����������Ϊ���ݿ��size���Ƕ�Ӧ�ģ�����size�齫�����߲��ܶ�Ӧ
		return sizeBlock;
	}
	/**
	 * ����ĳ�����״̬Ϊ��
	 * */
	private void setBlock(int index){
		staBlock[index]=true;
	}
	private void iniStaBlock(){
		int len;
		len=staBlock.length;
		for(int i=0;i<len;i++){
			staBlock[i]=false;
		}
	}
	/**
	 * �����ܿ�ķ���
	 * */
	private double calError(){
		double avg,val,err;
		int sum=0,num=0,len;
		len=sizeBlock.length;
		if(len<=0) return 0;
		for(int i=0;i<len;i++){
			if (staBlock[i])
				continue;
			num++;
			sum+=sizeBlock[i];
		}
		avg=sum/(1.0*num);
		err=0;
		for(int i=0;i<len;i++){
			if (staBlock[i])
				continue;
			val=sizeBlock[i]-avg;
			val*=val;
			err+=val;
		}
		return Math.sqrt(err)/(1.0*num);
	}
	/**
	 * ����Ŀǰ������
	 * <p>sizeBlock�����Ѿ���С��������
	 * */
	private int getIndex(){
		int index=0;
		int len;
		len=sizeBlock.length;
		for(int i=len-1;i>=0;i--){
			if (staBlock[i]) continue;
				return i;
		}
		return index;
	}
	/**
	 * ����Ŀǰ������
	 * */
	private int getIndex2(){
		int index=0,max;
		int len;
		max=0;
		len=sizeBlock.length;
		for(int i=0;i<len;i++){
			if (staBlock[i]) continue;
			if (sizeBlock[i]>max){
				max=sizeBlock[i];
				index=i;
			}
		}
		return index;
	}
	/**
	 * �����ҳ�ķ���ֵ,����֮ǰӦ���ȵ�����intiate����
	 * */
	public double getWebError(){
		int len;
		if (contBlock==null) return 0;
		len=sizeBlock.length;
		return calError();
	}
	public double getErr(){
		return err_val;
	}
	/**
	 * ���һ����ҳ���ɷֲ��Ħ�ֵ������֮ǰӦ���ȵ�����initiate����
	 * */
	public double getWebAlpha(){
		if (contBlock==null) return 0;
		return getAlpha2();
	}
	/**
	 * �ҳ��������У��ڸ�һ�鵽�������б�ʣ������б����Ϊ���ɷֲ���alphaֵ����
	 * */	
	private double getAlpha(){
		int size=sizeBlock.length;
		int[] sizeBlock2=new int[size];
		double val,max=0;
		double fir;
		System.arraycopy(sizeBlock,0,sizeBlock2,0,size);
		Arrays.sort(sizeBlock2);
		//for(int ii=0;ii<size;ii++){System.out.println(sizeBlock2[ii]);}
		fir=sizeBlock2[size-1];
		for(int i=size-2;i>=0;i--){
			if((fir)<max){          //ѭ����ֹ����,������ֵ
				break;
			}
			val=calAlpha(fir,sizeBlock2[i],size-i-1);
			if (val>max) max=val;
		}
		return max;
	}

	/**
	 * �ҳ��������У����б��
	 * */	
	private double getAlpha2(){
		double max=0,val;
		int size=sizeBlock.length;
		int[] sizeBlock2=new int[size];
		System.arraycopy(sizeBlock,0,sizeBlock2,0,size);
		Arrays.sort(sizeBlock2);
		//for(int ii=0;ii<size;ii++){System.out.println(sizeBlock2[ii]);}
		for(int i=size-1;i>=0;i--){
			if(sizeBlock2[i]<max)break;
			val=getOneAlpha(i,sizeBlock2);
			if (val>max) max=val;
		}
		return max;
	}
	/**
	 * �������Ŀ��ϣ��ӵ�index���鿪ʼ�Ҹÿ�����б��
	 * */
	private double getOneAlpha(int index,int[] sizeBlock2){
		double val,max=0;
		double fir;
		fir=sizeBlock2[index];
		for(int i=index-1;i>=0;i--){
			if(fir<max)break;          //ѭ����ֹ����,������ֵ
			val=calAlpha(fir,sizeBlock2[i],index-i);
			if (val>max) max=val;
		}
		return max;
	}
	private double calAlpha(double fir,double ith,int index){
		return (fir-ith)/index;
	}
	/**
	 * ����ֲ����ˣ�20%���У����ʹδ����������ƽ<p>
	 * ��ʱ�����ݿ��У�����Ŀ�����ҳ�����Ŀ飬��ƽ���ʹδ�Ŀ����ڽ�ÿ����ҳ��copyright
	 * ��һ���ֺ������ϴ�ĸ��Ų��֣��������Ƕ������ݿ���ȡ���źܴ�
	 * @return �����±�
	 * */
	private void getMaxNoiseBlock(String[] content){
		int index=0,len,max=0,nextIndex=0;
		int size;
		size=content.length;
		for(int i=0;i<size;i++){
			len=content[i].length();
			if (len>max){
				nextIndex=index;index=i;max=len;
			}
		}
		if(judge(size,index))
			sizeBlock[index]    =10;
		if(judge(size,nextIndex))
			sizeBlock[nextIndex]=10;
	}
	/**
	 * �������������ڵ������ĵ����е�λ�����ж��Ƿ�ɾ������
	 * �����������ĵ���ֲ������ˣ�20%����ͬʱ�ھ�������6����֮�ڡ�����������
	 * */
	private boolean judge(int size,int pos){
		if (pos<=(size*0.2)||pos>=(size*0.8))
			if(pos<2||pos>size-2)
				return true;
			else
				return false;
		else 
			return false;
	}
}




