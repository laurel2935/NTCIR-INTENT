package org.archive.ntcir.sm.similarity.editdistance.definition;



public class Block<T> {

	private int globalPosition;
	/** 块的内容 */
	private SuperString<T> data;
	/** 前后指针 */
	private Block<T> prev, next;
	/** 是否已经进行划分 */
	private boolean divideFlag = false;

	public Block(SuperString<T> string){
		this.data = string;
		this.globalPosition = 0;		
	}
	
	public Block(SuperString<T> string, int globalBegin){
		this.data = string;
		this.globalPosition = globalBegin;
	}	

	public int getGlobalPosition() {
		return globalPosition;
	}

	public void setGlobalPosition(int globalPosition) {
		this.globalPosition = globalPosition;
	}

	public SuperString<T> getData() {
		return data;
	}

	public void setData(SuperString<T> data) {
		this.data = data;
	}

	public Block<T> getPrev() {
		return prev;
	}

	public void setPrev(Block<T> prev) {
		this.prev = prev;
	}

	public Block<T> getNext() {
		return next;
	}

	public void setNext(Block<T> next) {
		this.next = next;
	}

	public boolean isDivideFlag() {
		return divideFlag;
	}

	public void setDivideFlag(boolean divideFlag) {
		this.divideFlag = divideFlag;
	}

	public void divide(int start, int length){
		if(start==0 && length==data.length()){
			this.divideFlag = true;
			return;
		}else if(start==0){
			//前面为已经分割的标记，后面应该为未分割的标记
			Block<T> tail = new Block<T>(data.substring(length), globalPosition + start);
			this.setDivideFlag(true);
			this.setData(data.substring(0, length));
			tail.next = this.next;
			if(tail.next!=null)	tail.next.prev = tail;
			this.next = tail;
			tail.prev = this;
		}else if(start+length == data.length()){
			//后面为已经分割的标记，前面应该为未分割的标记
			Block<T> head = new Block<T>(data.substring(0, start), globalPosition);
			
			this.setDivideFlag(true);
			this.setData(data.substring(start));
			
			head.prev = this.prev;
			if(head.prev!=null)	head.prev.next = head;
			head.next = this;
			this.prev = head;
		}else{
			//中间为已经分割的标记，前面和后面应该为未分割的标�?
			Block<T> head = new Block<T>(data.substring(0, start), globalPosition);
			Block<T> tail = new Block<T>(data.substring(start+length), globalPosition + start+length);
			
			this.setDivideFlag(true);
			this.setData(data.substring(start, start+length));
			this.setGlobalPosition(globalPosition + start);
			
			head.prev = this.prev;
			if(head.prev!=null)	head.prev.next = head;
			head.next = this;
			this.prev = head;
			
			tail.next = this.next;
			if(tail.next!=null)	tail.next.prev = tail;
			this.next = tail;
			tail.prev = this;
		}

	}
}
