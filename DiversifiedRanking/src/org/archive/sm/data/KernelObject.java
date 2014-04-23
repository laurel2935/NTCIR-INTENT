package org.archive.sm.data;

public class KernelObject {
	public String koStr;
	public String posTag;
	//
	public KernelObject(String str, String posTag){
		this.koStr = str;
		this.posTag = posTag;
	}
	public int hashCode()
    {
		return this.koStr.hashCode()+this.posTag.hashCode();
    }
	//
	public boolean equals(Object other){
		if(this == other){
			return true;
		}
		if(other instanceof KernelObject){
			final KernelObject cmpKO = (KernelObject)other;
			//
			return this.koStr.equals(cmpKO.koStr) && this.posTag.equals(cmpKO.posTag);						
		}else{
			return false;
		}
	}
}
