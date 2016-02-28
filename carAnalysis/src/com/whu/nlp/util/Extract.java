package com.whu.nlp.util;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javabean.Keyword;
import javabean.MatchedResult;
import javabean.db.Event;
import javabean.db.KeywordGroup;
import javabean.db.Page;
import javabean.db.Template;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import dataAnalysis.Test;
import dataAnalysis.WebPageAnalyzer;





public class Extract
{
	static HashMap<Integer, String> eventID2Name=new HashMap<Integer, String>();
	static{
		eventID2Name.put(1, "overall");
		eventID2Name.put(2, "smell");
		eventID2Name.put(3, "airout");
		eventID2Name.put(4, "secondoverall");
		eventID2Name.put(5, "autoair");
		eventID2Name.put(6, "airloop");
		eventID2Name.put(7, "trouble");
		eventID2Name.put(8, "parts");
	}
	public Myobject extract(String input) throws Exception
	{
		Myobject reObject = new Myobject(); 
		reObject.init();
		reObject.setContent(input);
		String objectID=UUID.randomUUID().toString().replaceAll("-", "");
		Page page=new Page();
		page.setId(objectID);
		page.setContent(input);
		HashMap<Event,ArrayList<MatchedResult>> eventResults=Test.extractEvent(page);
		String labeledText=Test.lableText(input, eventResults);
		reObject.setContent(labeledText);
		HashMap<String, String> name2text=new HashMap<String, String>();
		for(Event event:eventResults.keySet()){
			String textParts="";
			for(MatchedResult matchedResult:eventResults.get(event)){
				
				textParts+=input.substring(matchedResult.getStart(),matchedResult.getEnd());
			}
			name2text.put(eventID2Name.get(event.getEventID()),textParts);
		}
		reObject.setResult(name2text);
		return reObject;

	}



	public static void main(String[] args) throws Exception
	{
		Extract extract=new Extract();
		Myobject myobject=extract.extract("宝马空调不够力");
		System.out.println(myobject.getContent());
		System.out.println(myobject.getResult().get("overall"));
	}
}
