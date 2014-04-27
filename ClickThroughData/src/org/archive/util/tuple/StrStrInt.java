package org.archive.util.tuple;

public class StrStrInt extends Triple<String, String, Integer> {
	public StrStrInt(String first, String second, Integer third){
		super(first, second, third);
	}
	//
	public void upThird(int deltaInt){
		this.third += deltaInt;
	}
}
