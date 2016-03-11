package com.whu.nlp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
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

import snooway.dao.DataDao;
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
			/*String textParts="";
			for(MatchedResult matchedResult:eventResults.get(event)){
				textParts+=input.substring(matchedResult.getStart(),matchedResult.getEnd());
			}
			name2text.put(eventID2Name.get(event.getEventID()),textParts);*/
			HashMap<Event,ArrayList<MatchedResult>> eventResults1class=new HashMap<Event, ArrayList<MatchedResult>>();
			eventResults1class.put(event, eventResults.get(event));
			//name2text.put(eventID2Name.get(event.getEventID()),Test.lableText(input, eventResults1class));
			name2text.put(event.getEventID()+" "+event.getEventName()+" "+event.getStatus(),Test.lableText(input, eventResults1class));
		}
		reObject.setResult(name2text);
		return reObject;

	}

	public void extractBat(String fileDirStr)throws Exception{
		DataDao dao=WebPageAnalyzer.da;
		File fileDir=new File(fileDirStr);
		File[] files=fileDir.listFiles();
		for(File file:files){
			StringBuffer text=new StringBuffer();
			BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf8"));
			String str=null;
			while((str=reader.readLine())!=null){
				text.append(str+"\n");
			}
			reader.close();
			Myobject eventResults=extract(text.substring(0, text.length()-1).toString());
			for(String eventInfoStr:eventResults.getResult().keySet()){
				String[] eventInfos=eventInfoStr.split(" ");
				String id=dao.insertCarEvent(file.getName(), eventInfos[1], eventResults.getResult().get(eventInfoStr), Integer.parseInt(eventInfos[2]));
//				System.out.println(eventInfoStr);
//				System.out.println(eventResults.getResult().get(eventInfoStr));
			}
		}
	}
	public List<CarEvent> extractBat(List<String> inputs)throws Exception{
		List<CarEvent> results=new ArrayList<CarEvent>();
		for(String input:inputs){
			Myobject eventResults=extract(input);
			for(String eventInfoStr:eventResults.getResult().keySet()){
				String[] eventInfos=eventInfoStr.split(" ");
				String objectID=UUID.randomUUID().toString().replaceAll("-", "");
				results.add(new CarEvent(objectID, eventInfos[1], eventResults.getResult().get(eventInfoStr), Integer.parseInt(eventInfos[2])));
			}
		}
		return results;
	}
	public static void main(String[] args) throws Exception
	{
		Extract extract=new Extract();
		List<String> inputs=new ArrayList<String>();
		inputs.add("前排空调异味");//新凯越空调出风口风量小。 请教大家一个问题空调很智能
		System.out.println(extract.extractBat(inputs));
//		Test.writeData2xls("E:/whucsgs/实验室/汽车项目");
//		extract.extractBat("newdata");
//		Myobject myobject=extract.extract("新凯越空调出风口风量小。 请教大家一个问题空调很智能");
//		System.out.println(myobject.getContent());
//		System.out.println(myobject.getResult().get("smell"));
//		System.out.println(myobject.getResult().get("parts"));
		
	}
}
class CarEvent{
	String id;
	String eventName;
	String content;
	int polarity;
	public CarEvent(String id, String eventName, String content, int polarity) {
		super();
		this.id = id;
		this.eventName = eventName;
		this.content = content;
		this.polarity = polarity;
	}
	@Override
	public String toString() {
		return "CarEvent [id=" + id + ", eventName=" + eventName + ", content="
				+ content + ", polarity=" + polarity + "]";
	}
	
}