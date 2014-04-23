package org.archive.sm.data;


public class Modifier implements Comparable{
	public String moStr;
	public String posTag;
	private int fre = 0;
	//
	public Modifier(String moStr, String posTag){
		this.moStr = moStr;
		this.posTag = posTag;
	}
	//
	public void setFre(int fre){
		this.fre = fre;
	}
	public int getFre(){
		return this.fre;
	}
	//
	public int hashCode()
    {
		return this.moStr.hashCode()+this.posTag.hashCode();
    }
	//
	public boolean equals(Object other){
		if(this == other){
			return true;
		}
		if(other instanceof Modifier){
			final Modifier cmpMO = (Modifier)other;
			//
			return this.moStr.equals(cmpMO.moStr) && this.posTag.equals(cmpMO.posTag);						
		}else{
			return false;
		}
	}
	//
	public  int compareTo(Object o){
		Modifier comp = (Modifier)o;
		if(this.fre > comp.fre){
			return -1;
		}else if(this.fre < comp.fre){
			return 1;
		}else{
			return 0;
		}
	}
}
