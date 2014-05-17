package org.archive.ntcir.sm.similarity.editdistance.definition;

/**
 * 编辑单元
 * 
 * @author <a href="mailto:iamxiatian@gmail.com">夏天</a>
 * @organization 中国人民大学信息资源管理学院 知识工程实验�?
 */
public abstract class EditUnit {
	/**
	 * 获取编辑单元的内部字符串
	 * @return
	 */
	public abstract String getUnitString();
	
	/**
	 * 获取替换代价，默认替换代价当替换单元的内容相同时�?�?
	 * 不同时为1
	 */
	public double getSubstitutionCost(EditUnit other){
		return this.equals(other)?0:1;
	}
	
	/**
     * 获取删除代价,标准算法的默认�?�?.0, 此处也设�?.0
     * 具体的编辑单元可以�?过覆盖该方法设置不同的删除代�?
     * @return 删除代价
     */
    public double getDeletionCost(){
        return 1.0;
    }    
    
    /**
     * 获取插入代价,标准算法的默认�?�?.0.
     * 具体的编辑单元可以�?过覆盖该方法设置不同的插入代�?
     */
    public double getInsertionCost(){
        return 1.0;
    }
    	
    @Override
	public boolean equals(Object other){
    	if(!(other instanceof EditUnit)) return false;
		return getUnitString().equals(((EditUnit)other).getUnitString());
	}
	
	@Override
	public String toString(){
		return getUnitString();
	}
}
