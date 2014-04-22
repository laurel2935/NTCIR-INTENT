package org.archive.nlp.htmlparser.pk;

import java.util.*;

public class Stack <T>{
	private LinkedList<T> list;
	private boolean isEmpty;
	public Stack(){
		list=new LinkedList<T>();
		isEmpty=true;
	}
	/**
	 * ��ջ
	 * */
	public void push(T node){
		list.add(node);
		if (isEmpty)
			isEmpty=false;
	}
	/**
	 * ��ջ
	 * */
	public T pop(){
		T node;
		if (!isEmpty){
			node=list.getLast();
		    list.removeLast();
		    if (list.size()==0)
		    	isEmpty=true;
		    return node;
		}
		else
			return null;
	}
	/**
	 * �õ�ջ��Ԫ�ص�ֵ
	 * */
	public T getTop(){
		if (!isEmpty)
		    return list.getLast();
		return null;
	}
	/**
	 * ջ�Ĵ�С
	 * */
	public int getSize(){
		return list.size();
	}
	/**
	 * ȡ��ջ��ĳ��λ�õ�Ԫ��
	 * */
	public T getElement(int index){
		int size;
		size=getSize();
		if (index<0||index>size)
			return null;
		return list.get(index);
	}
	/**
	 * ɾ��ջ��ĳ��Ԫ��
	 * */
	public void remove(int index){
		list.remove(index);
	}
	/**
	 * ջ�ǿյ���
	 * */
	public boolean empty(){
		if (isEmpty) return true;
		return false;
	}
	/**
	 * ��ӡ
	 * */
	public void print(){
		int size=list.size();
		for(int i=size-1;i>=0;i--){
			System.out.print(list.get(i).toString()+" ");
		}
	}
}
