package com.whu.nlp.util;

import java.util.HashMap;


public class Myobject
{
	
	private String content;
	private HashMap<String,String> result;
	

	public void init(){
		result = new HashMap<String,String>();
		result.put("overall", ""); 
		result.put("smell", "");
		result.put("airout", "");
		result.put("secondoverall", "");
		result.put("autoair", "");
		result.put("airloop", "");
		result.put("trouble", "");
		result.put("parts", "");
		
	}

	public String toString()
	{
		return "Content:"+content;

	}

	public HashMap<String, String> getResult() {
		return result;
	}

	public void setResult(HashMap<String, String> result) {
		this.result = result;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}
}
