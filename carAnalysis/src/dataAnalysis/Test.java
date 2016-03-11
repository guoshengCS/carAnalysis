package dataAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.whu.nlp.util.Myobject;

import snooway.dao.DataDao;
import snooway.dao.PoolConection;
import snooway.dao.PoolConnectCfg;
import util.AnalysisProperties;
import whu.nlp.extracter.ExtractRegion;
import whu.nlp.extracter.beans.ResultRegion;
import whu.nlp.extracter.util.MarkRegion;


import javabean.ConceptTable;
import javabean.Keyword;
import javabean.MatchedResult;
import javabean.db.Event;
import javabean.db.KeywordGroup;
import javabean.db.Page;
import javabean.db.Template;

public class Test implements Callable<String>{
	int startFileIndex=0;
	int endFileIndex=0;
	int index=0;
	static String resultDir;
	static int annotatorNum=1;
	static ConcurrentHashMap<String, Integer> docEventNumMap=new ConcurrentHashMap<String, Integer>();
	public static File[] docFiles=null;
	static int isPrecision;
	
	private static String restrictWordList="无(?!法|证|照|资质|法|序|效|人管)|没(?!有?保障)|未(?!取得|经|审|能)|查办|反|勿|防止|预防|举报|打击|(?<!拒)不(?!查|报|明|清(?!除)|公|力|经|全|落实|肯|符|符|合|办|干|上|交|缴|理|正|知|严|分|作为|尊|遵|依|按|法|予|实|关心|具备|给|支付|过关|达标|制|透明|可信|准确)|严打|严查|严厉查处|严禁|抑制|控制|防|[杜拒]绝|防范|警惕|预防|谨防|减[少小轻]|遏制|禁止|治理|零|解决|取缔|惩治|整治|制裁|避免|降低|[清消]除|淘汰|清查|查处|破获|抓获|公布|以涉嫌|(?!不)依法|以免|祛除|如有|严打|排查|切?忌";
	static{
		WebPageAnalyzer.conceptPublic = WebPageAnalyzer.da.getEventByEventId(-1, "公共概念");
		ArrayList<KeywordGroup> global_kg = WebPageAnalyzer.conceptPublic.getConcepts();
		
		System.out.println("通用事件初始化中……………………" + global_kg.size());

		for (KeywordGroup global_k : global_kg) {
			initMap(global_k, WebPageAnalyzer.global_concept);
		}

		
		WebPageAnalyzer.eventList = WebPageAnalyzer.da.getEventList();// 初始化事件列表
		System.out.println("WebPageAnalyzer.da.getEventList()"
				+ WebPageAnalyzer.da);

		WebPageAnalyzer.events = WebPageAnalyzer.eventList.getEvents(); // 获取事件
		Event event_init = null;
		for (int i = 0; i < WebPageAnalyzer.events.size(); i++) {
			event_init = (Event) WebPageAnalyzer.events.get(i);
			ArrayList<Template> Template = event_init.getTemplates();
			for (Template template : Template) {
				for (KeywordGroup k : template.getConcepts()) // 匹配模板包含的所有概念
				{
					initMap(k, event_init.getConceptMap());
				}
			}
		}
		try {
			addRuleFromXML("ontology_car.xml",true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void addRuleFromXML(String addFileName,boolean dbFlag) throws Exception{
		List<Event> events=WebPageAnalyzer.eventList.getEvents();
		SAXReader saxReader=new SAXReader();
		FileInputStream in=new FileInputStream(addFileName);
		Document document=saxReader.read(in);
		in.close();
		Element rootEle=document.getRootElement();
		Element pubConceptEle=rootEle.element("concepts");
		if(pubConceptEle!=null){
			Event event=WebPageAnalyzer.conceptPublic;
			List<Element> conceptEles=pubConceptEle.elements("concept");
			if(conceptEles!=null&&conceptEles.size()>0){
				ArrayList<Element> addParentKgEles=new ArrayList<Element>();
				ArrayList<Integer> addParentKgHadFlags=new ArrayList<Integer>();
				for(Element conceptEle:conceptEles){
					String conceptName=conceptEle.attributeValue("name").trim();
					KeywordGroup concept;
					int hadConcept=0;
					if((concept=WebPageAnalyzer.global_concept.get(conceptName))!=null){//event中已有该concept
						hadConcept=1;
					}else {
						concept=new KeywordGroup();
						concept.setKeywordGroupName(conceptName);
						event.getConcepts().add(concept);
						WebPageAnalyzer.global_concept.put(conceptName, concept);
					}
					if (conceptEle.element("child")!=null) {
						addParentKgEles.add(conceptEle);
						addParentKgHadFlags.add(new Integer(hadConcept));
						continue;
					}
					String[] keywordNames=conceptEle.getText().split("\\s+");
					Keyword keyword;
					ArrayList<Keyword> hadKeywords=hadConcept==1?concept.getKeywords():new ArrayList<Keyword>();
					for(String keywordName:keywordNames){
						if(keywordName.trim().equals(""))
							continue;
						int flag=0;
						for(Keyword hadKeyword:hadKeywords){
							if(hadKeyword.getKeywordName().trim().equals(keywordName)){
								flag=1;
								break;
							}
						}
						if (flag==1) {
							continue;
						}
						keyword=new Keyword(1,keywordName,"");
						concept.getKeywords().add(keyword);
//						System.err.println(concept.generateExp());
					}
				}
				for(int i=0;i<addParentKgEles.size();i++){
					Element addParentKgEle=addParentKgEles.get(i);
					String addParentKgName=addParentKgEle.attributeValue("name").trim();
					KeywordGroup addParentKg=WebPageAnalyzer.global_concept.get(addParentKgName);
					Element childEle=addParentKgEle.element("child");
					String[] childKgNames=childEle.getTextTrim().split("\\s+");
					if (addParentKgHadFlags.get(i)==0) {
						List<KeywordGroup> childKeywordGroup=new ArrayList<KeywordGroup>();
						addParentKg.setChildKeywordGroup(childKeywordGroup);
						for(String childKgName:childKgNames){
							addParentKg.getChildKeywordGroup().add(WebPageAnalyzer.global_concept.get(childKgName));
						}	//event.getConceptMap().get(childKgName)==null?WebPageAnalyzer.global_concept.get(childKgName):event.getConceptMap().get(childKgName)
					}else {
						List<KeywordGroup> childKeywordGroup=addParentKg.getChildKeywordGroup();
						for(String childKgName:childKgNames){
							int flag=0;
							for(KeywordGroup keywordGroup:childKeywordGroup){
								if(keywordGroup.getKeywordGroupName().equals(childKgName)){
									flag=1;
									break;
								}
							}
							if (flag==0) {
								addParentKg.getChildKeywordGroup().add(WebPageAnalyzer.global_concept.get(childKgName));
							}
						}
						
					}
				}
			}
		}
		List<Element> eventAddEles=rootEle.elements("eventAdd");
		eventAddEles=new ArrayList<Element>(eventAddEles);
		for(int i=0;i<eventAddEles.size();i++){
			List<Element> childAddEles=null;
			if ((childAddEles=eventAddEles.get(i).elements("eventAdd"))!=null) {
//				String eventName=eventAddEles.get(i).attribute("name").getValue();
				eventAddEles.addAll(childAddEles);
			}
		}
		for(Element eventAddEle:eventAddEles){
			String eventName=eventAddEle.attribute("name").getValue();
			String eventID=eventAddEle.attribute("id").getValue();
			for(Event event:events){
				if (event.getEventName().trim().equals(eventName)) {
					List<Element> conceptEles=eventAddEle.elements("concept");
					if(conceptEles!=null&&conceptEles.size()>0){
						ArrayList<Element> addParentKgEles=new ArrayList<Element>();
						ArrayList<Integer> addParentKgHadFlags=new ArrayList<Integer>();
						for(Element conceptEle:conceptEles){
							String conceptName=conceptEle.attributeValue("name").trim();
							KeywordGroup concept;
							int hadConcept=0;
							if((concept=event.getConceptByName(conceptName))!=null){//event中已有该concept
//								System.err.println(eventName+conceptName);
								hadConcept=1;
							}else {
								concept=new KeywordGroup();
								concept.setKeywordGroupName(conceptName);
								event.getConcepts().add(concept);
								event.getConceptMap().put(conceptName, concept);
							}
							if (conceptEle.element("child")!=null) {
								addParentKgEles.add(conceptEle);
								addParentKgHadFlags.add(new Integer(hadConcept));
								continue;
							}
//							List<Keyword> keywords=new ArrayList<Keyword>();
							String[] keywordNames=conceptEle.getText().split("\\s+");
//							System.err.println(conceptEle.getText()+keywordNames.length);
							Keyword keyword;
							ArrayList<Keyword> hadKeywords=hadConcept==1?concept.getKeywords():new ArrayList<Keyword>();
							for(String keywordName:keywordNames){
								if(keywordName.trim().equals(""))
									continue;
								int flag=0;
								for(Keyword hadKeyword:hadKeywords){
									if(hadKeyword.getKeywordName().trim().equals(keywordName)){
										flag=1;
										break;
									}
								}
								if (flag==1) {
									continue;
								}
								keyword=new Keyword(1,keywordName,"");
								concept.getKeywords().add(keyword);
//								System.err.println(concept.generateExp());
							}
						}
						for(int i=0;i<addParentKgEles.size();i++){
							Element addParentKgEle=addParentKgEles.get(i);
							String addParentKgName=addParentKgEle.attributeValue("name").trim();
							KeywordGroup addParentKg=event.getConceptByName(addParentKgName);
							Element childEle=addParentKgEle.element("child");
							String[] childKgNames=childEle.getTextTrim().split("\\s+");
							if (addParentKgHadFlags.get(i)==0) {
								List<KeywordGroup> childKeywordGroup=new ArrayList<KeywordGroup>();
								addParentKg.setChildKeywordGroup(childKeywordGroup);
								for(String childKgName:childKgNames){
									addParentKg.getChildKeywordGroup().add(event.getConceptMap().get(childKgName)==null?WebPageAnalyzer.global_concept.get(childKgName):event.getConceptMap().get(childKgName));
//									if (WebPageAnalyzer.global_concept.get(childKgName)==null&&event.getConceptMap().get(childKgName)==null) {
//										System.out.println(eventName+" "+addParentKgName+" "+childKgName);
//									}
								}	//event.getConceptMap().get(childKgName)==null?WebPageAnalyzer.global_concept.get(childKgName):event.getConceptMap().get(childKgName)
							}else {
								List<KeywordGroup> childKeywordGroup=addParentKg.getChildKeywordGroup();
//								System.out.println(addFileName+eventName+addParentKg.getKeywordGroupName()+" "+childKeywordGroup.size());
								for(String childKgName:childKgNames){
									int flag=0;
									for(KeywordGroup keywordGroup:childKeywordGroup){
//										System.out.println(keywordGroup);
										if(keywordGroup.getKeywordGroupName().equals(childKgName)){
											flag=1;
											break;
										}
									}
									if (flag==0) {
//										System.err.println("haha"+childKgName+WebPageAnalyzer.global_concept.size());
										addParentKg.getChildKeywordGroup().add(event.getConceptMap().get(childKgName)==null?WebPageAnalyzer.global_concept.get(childKgName):event.getConceptMap().get(childKgName));
									}
								}
								
							}
						}
//						for(Element addParentKgEle:addParentKgEles){
//							String parengConceptName=addParentKgEle.attributeValue("name").trim();
//							Element childEle=addParentKgEle.element("child");
//							String[] childKgNames=childEle.getTextTrim().split("\\s+");
//							for(String childKgName:childKgNames){
//								event.getConceptByName(childKgName);
//							}
//						}
					}
					
					List<Element> templateElements=eventAddEle.elements("template");
					if(templateElements!=null&&templateElements.size()>0){
						for(Element templateElement:templateElements){
							Template template=new Template();
//							if()
							template.setTemplateId(event.getEventID()*100+event.getTemplates().size()+1);
							template.setTemplateName(templateElement.attributeValue("name"));
							template.setTemplateRule(templateElement.getText().replaceAll("\\s", ""));
							event.getTemplates().add(template);
						}
					}		
					break;
				}
			}
		}
	}
	public static void extractEvent(String dataDirPath)throws Exception{
		File dataDir=new File(dataDirPath);
		File resultDir=new File("eventResult");
		if (!resultDir.exists()) {
			resultDir.mkdir();
		}
		File[] dataFiles=dataDir.listFiles();
		for(File dataFile:dataFiles){
			Page page=new Page();
	    	page.setId(dataFile.getName());
	    	BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "utf8"));
	    	StringBuffer content=new StringBuffer();
	    	String str=null;
	    	while((str=reader.readLine())!=null){
	    		content.append(str);
	    	}
	    	reader.close();
			page.setContent(content.toString());
			HashMap<Event,ArrayList<MatchedResult>> eventResults=extractEvent(page);
			str=lableText(content.toString(), eventResults);
			PrintWriter writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream("eventResult/"+dataFile.getName()), "utf8"));
			writer.print(str);
			writer.close();
		}
	}
	public static String lableText(String text,HashMap<Event,ArrayList<MatchedResult>> eventResults){
		List<Event> events=WebPageAnalyzer.eventList.getEvents();
		ArrayList<MatchedResult> matchedResults=null;
		
		ArrayList<Integer> eventTypes=new ArrayList<Integer>();
		if (!WebPageAnalyzer.aprop.getEventName().equals("all")) {
			String[] types=WebPageAnalyzer.aprop.getEventName().split(",\\s*");
			for (int i = 0; i < types.length; i++) {
				eventTypes.add(Integer.parseInt(types[i]));
			}
		}
		
		for(Event event:events){
			if(WebPageAnalyzer.aprop.getEventName().equals("all")||eventTypes.contains(event.getEventID())){
				ArrayList<MatchedResult> tempResults=null;
				if ((tempResults=eventResults.get(event))!=null) {
					if (matchedResults==null) {
						matchedResults=new ArrayList<MatchedResult>(tempResults); 
					}else {
						matchedResults.addAll(tempResults);
					}
				}
				
			}
		}
		
		if(matchedResults!=null){
			Collections.sort(matchedResults,new Comparator<MatchedResult>() {
				@Override
				public int compare(MatchedResult o1, MatchedResult o2) {
					// TODO Auto-generated method stub
					if(o1.getStart()>o2.getStart())
						return 1;
					else if(o1.getStart()<o2.getStart()){
						return -1;
					}
					return 0;
				}
			});

		}
		
		if(matchedResults!=null){
			ArrayList<MatchedResult> mergedResults=new ArrayList<MatchedResult>();
			for(MatchedResult matchedResult:matchedResults){
				MatchedResult newMergeResult=new MatchedResult();
				int i=mergedResults.size()-1;
				while(i>=0&&matchedResult.getStart()<mergedResults.get(i).getStart()){
					i--;
				}
				int mergeFlag=0;
				if(i<mergedResults.size()-1){
					if(matchedResult.getEnd()>=mergedResults.get(i+1).getStart()){//和后一个有交叉
						newMergeResult.setStart(Math.min(matchedResult.getStart(),mergedResults.get(i+1).getStart()));
						newMergeResult.setEnd(Math.max(matchedResult.getEnd(), mergedResults.get(i+1).getEnd()));
						mergedResults.remove(i+1);
						mergedResults.add(i+1, newMergeResult);
						mergeFlag=1;
					}
				}
				if(i>=0){
					if(matchedResult.getStart()<=mergedResults.get(i).getEnd()){//和前一个有交叉
						newMergeResult.setStart(Math.min(matchedResult.getStart(),mergedResults.get(i).getStart()));
						newMergeResult.setEnd(Math.max(matchedResult.getEnd(), mergedResults.get(i).getEnd()));
						mergedResults.remove(i);
						mergedResults.add(i,newMergeResult);
						mergeFlag=1;
					}
				}
				if(mergeFlag==0){
					newMergeResult.setStart(matchedResult.getStart());
					newMergeResult.setEnd(matchedResult.getEnd());
					mergedResults.add(i+1,newMergeResult);
				}
			}
			StringBuffer htmlText=new StringBuffer(text);
			for(int j=mergedResults.size()-1;j>=0;j--){
				htmlText.insert(mergedResults.get(j).getEnd(), "</font>");
				htmlText.insert(mergedResults.get(j).getStart(), "<font color=\"red\">");
			}	
			return htmlText.toString();
		}else {
			return text;
		}
	}
	public static void extractEvent(String addFileName,String contentFileName) throws Exception{
		List<Event> events=WebPageAnalyzer.eventList.getEvents();
		BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(contentFileName), "utf8"));
		PrintWriter writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream(contentFileName.substring(0, contentFileName.lastIndexOf("."))+"_Result.txt"), "utf8"));
	    String leavingText;
	    int pageId=0;
	    while((leavingText=reader.readLine())!=null){
	    	Page page=new Page();
	    	page.setId((pageId++)+"");
			page.setContent(leavingText);
			HashMap<Event,ArrayList<MatchedResult>> eventResults=extractEvent(page);
			List<Event> eventsInMap=WebPageAnalyzer.eventList.getEvents();
			ArrayList<MatchedResult> matchedResults=null;
			
			ArrayList<Integer> eventTypes=new ArrayList<Integer>();
			if (!WebPageAnalyzer.aprop.getEventName().equals("all")) {
				String[] types=WebPageAnalyzer.aprop.getEventName().split(",\\s*");
				for (int i = 0; i < types.length; i++) {
					eventTypes.add(Integer.parseInt(types[i]));
				}
			}
			
			for(Event event:events){
//				if(event.getEventID()==Integer.parseInt(WebPageAnalyzer.aprop.getEventName())){
				if(WebPageAnalyzer.aprop.getEventName().equals("all")||eventTypes.contains(event.getEventID())){
//					matchedResults=eventResults.get(event)==null?null:new ArrayList<MatchedResult>(eventResults.get(event));
					ArrayList<MatchedResult> tempResults=null;
					if ((tempResults=eventResults.get(event))!=null) {
						if (matchedResults==null) {
							matchedResults=new ArrayList<MatchedResult>(tempResults); 
						}else {
							matchedResults.addAll(tempResults);
						}
					}
					
				}
			}
			
			if(matchedResults!=null){
				Collections.sort(matchedResults,new Comparator<MatchedResult>() {
					@Override
					public int compare(MatchedResult o1, MatchedResult o2) {
						// TODO Auto-generated method stub
						if(o1.getStart()>o2.getStart())
							return 1;
						else if(o1.getStart()<o2.getStart()){
							return -1;
						}
						return 0;
					}
				});

			}
			
			if(matchedResults!=null){
				ArrayList<MatchedResult> mergedResults=new ArrayList<MatchedResult>();
				for(MatchedResult matchedResult:matchedResults){
					MatchedResult newMergeResult=new MatchedResult();
					int i=mergedResults.size()-1;
					while(i>=0&&matchedResult.getStart()<mergedResults.get(i).getStart()){
						i--;
					}
					int mergeFlag=0;
					if(i<mergedResults.size()-1){
						if(matchedResult.getEnd()>=mergedResults.get(i+1).getStart()){//和后一个有交叉
							newMergeResult.setStart(Math.min(matchedResult.getStart(),mergedResults.get(i+1).getStart()));
							newMergeResult.setEnd(Math.max(matchedResult.getEnd(), mergedResults.get(i+1).getEnd()));
							mergedResults.remove(i+1);
							mergedResults.add(i+1, newMergeResult);
							mergeFlag=1;
						}
					}
					if(i>=0){
						if(matchedResult.getStart()<=mergedResults.get(i).getEnd()){//和前一个有交叉
							newMergeResult.setStart(Math.min(matchedResult.getStart(),mergedResults.get(i).getStart()));
							newMergeResult.setEnd(Math.max(matchedResult.getEnd(), mergedResults.get(i).getEnd()));
							mergedResults.remove(i);
							mergedResults.add(i,newMergeResult);
							mergeFlag=1;
						}
					}
					if(mergeFlag==0){
						newMergeResult.setStart(matchedResult.getStart());
						newMergeResult.setEnd(matchedResult.getEnd());
						mergedResults.add(i+1,newMergeResult);
					}
				}
				StringBuffer htmlText=new StringBuffer(page.getContent());
				for(int j=mergedResults.size()-1;j>=0;j--){
					htmlText.insert(mergedResults.get(j).getEnd(), "</span>");
					htmlText.insert(mergedResults.get(j).getStart(), "<span class=\"eventlabel\">");
				}	
				writer.println(htmlText);
			}else {
				writer.println(leavingText);
			}
			
	    }
		reader.close();
		writer.close();
	}
	public static HashMap<Event,ArrayList<MatchedResult>> extractEvent(Page page){
		Pattern pattern=PatternAgent.getPattern("不|不能|无法|没有");
		List<Event> events=WebPageAnalyzer.eventList.getEvents();
		//System.out.println(events.size());
		HashMap<Event,ArrayList<MatchedResult>> resultsMap=new HashMap<Event,ArrayList<MatchedResult>>();
		for (int i = 0; i < events.size() ; i++) {

			Event e = events.get(i);
//			try {
//			EventMatcher em = new EventMatcher();
			TempEventMatcher em = new DecEventMatcher();
				ArrayList<MatchedResult> mrList;

				// ArrayList<KeywordGroup> concepts = e.getConcepts();
				ArrayList<Template> templates = new ArrayList<Template>(
						e.getTemplates());
				ArrayList<MatchedResult> results=null;
				String[] templateName=new String[1];
				while ((mrList = em.find(page.getContent(), e, page.getId(), templates,templateName)) != null) {
					if (!templateName[0].contains("否定")) {
						ArrayList<MatchedResult> results2del=new ArrayList<MatchedResult>();
						for(MatchedResult mr:mrList){
							DecMatchedResult decmr=(DecMatchedResult)mr;
							String eventTail="";
							int start=0,end=0;
							end=decmr.keywordMRs.get(decmr.keywordMRs.size()-1).getEnd();
							if (decmr.keywordMRs.size()>1) {
								start=decmr.keywordMRs.get(decmr.keywordMRs.size()-2).getEnd();
							}else {
								start=Math.max(0, decmr.keywordMRs.get(decmr.keywordMRs.size()-1).getStart()-6);
							}
							eventTail=page.getContent().substring(start, end);
							Matcher matcher=pattern.matcher(eventTail);
							if (matcher.find()) {
								results2del.add(mr);
							}
						}
						mrList.removeAll(results2del);
					}
					if (mrList.size()==0) {
						continue;
					}
					if((results=resultsMap.get(e))==null){
						resultsMap.put(e, mrList);
					}else {
						results.addAll(mrList);
					}
				}
//			}
		}
		for (int i = 0; i < events.size() ; i++) {
			events.get(i).cleanMap(page.getId());
		}
		for (String kName : WebPageAnalyzer.global_concept.keySet()) {
			WebPageAnalyzer.global_concept.get(kName).getResultMaps().remove(page.getId());
		}
		/*List<Event> event2remove=new ArrayList<Event>();
		for(Event event:resultsMap.keySet()){
			ArrayList<MatchedResult> eventResults=resultsMap.get(event);
			ArrayList<MatchedResult> result2remove=new ArrayList<MatchedResult>();
			for(MatchedResult mr:eventResults){
				int resfirst = Math.max(mr.getStart()-30, 0);
				String context=page.getContent().substring(resfirst,mr.getStart());
				if(!context.contains("宝马")&&!context.contains("新三系")){
					result2remove.add(mr);
				}
			}
			eventResults.removeAll(result2remove);
			if(eventResults.size()==0){
				event2remove.add(event);
			}
		}
		for(Event event:event2remove){
			resultsMap.remove(event);
		}*/
		return resultsMap;
	}
	/**
	 * @param args
	 */
	private static void initMap(KeywordGroup kg,
			ConcurrentHashMap<String, KeywordGroup> map) {
		// System.out.println(kg.getKeywordGroupName());
		for (KeywordGroup child : kg.getChildKeywordGroup()) {
			initMap(child, map);
		}
		map.put(kg.getKeywordGroupName(), kg);
	}
	
	public static void stEventResults(String dataDir) throws Exception{
		int allExtractNum=0,availableDocNum=0;
		File dataDirfile=new File(dataDir);
		for(File dataFile:dataDirfile.listFiles()){
			System.out.println(dataFile.getAbsolutePath());
			Page page=new Page();
			page.setId(dataFile.getName().split("\\.")[0]);
			FileInputStream inputStream=new FileInputStream(dataFile.getAbsoluteFile());
			BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream, "utf8"));
			char[] cbuf=new char[1000];
			StringBuffer text=new StringBuffer();
			int len=0;
			while((len=reader.read(cbuf))!=-1){
				text.append(cbuf, 0, len);
			}
			page.setContent(text.toString().replaceAll("<.*?span.*?>", ""));
			HashMap<Event,ArrayList<MatchedResult>> eventResults=extractEvent(page);
			System.out.println("eventResult");
			List<Event> events=WebPageAnalyzer.eventList.getEvents();
			ArrayList<MatchedResult> matchedResults=null;
			for(Event event:events){
				if(event.getEventID()==Integer.parseInt(WebPageAnalyzer.aprop.getEventName())){
					matchedResults=eventResults.get(event)==null?null:new ArrayList<MatchedResult>(eventResults.get(event));
					if(matchedResults!=null){
//						Collections.sort(matchedResults,new Comparator<MatchedResult>() {
//							@Override
//							public int compare(MatchedResult o1, MatchedResult o2) {
//								// TODO Auto-generated method stub
//								if(o1.getStart()>o2.getStart())
//									return 1;
//								else if(o1.getStart()<o2.getStart()){
//									return -1;
//								}
//								return 0;
//							}
//						});
						allExtractNum+=matchedResults.size();
						availableDocNum++;
					}
					break;
				}
			}
			
		}
		System.out.println("allExtractNum="+allExtractNum);
		System.out.println("availableDocNum="+availableDocNum);
	}
	
	
	
	public Test(int startFileIndex, int endFileIndex,int index) {
		super();
		this.startFileIndex = startFileIndex;
		this.endFileIndex = endFileIndex;
		this.index=index;
	}
	

	public static void writeEventResultsOfDocs(String dataDir,String newResultDir) throws Exception{
		resultDir=newResultDir;
		File dataDirfile=new File(dataDir);
		File rsDirFile=new File(resultDir);
//		File rsHtmDir=new File(resultDir+"_html");
		if(!rsDirFile.exists())
			rsDirFile.mkdir();
//		if(!rsHtmDir.exists())
//			rsHtmDir.mkdir();
		docFiles=dataDirfile.listFiles();
		int threadNum=WebPageAnalyzer.aprop.getThreadsNum();
		ExecutorService executorService=Executors.newFixedThreadPool(threadNum);
		List<Callable<String>> list=new ArrayList<Callable<String>>();
		int perThreadNum=docFiles.length/threadNum;
		for(int i=0;i<threadNum-1;i++){
			Test test=new Test(i*perThreadNum, (i+1)*perThreadNum-1,i);
			list.add(test);
		}
		list.add(new Test((threadNum-1)*perThreadNum, docFiles.length-1, threadNum-1));
		List<Future<String>> futures=executorService.invokeAll(list);
		int allEventNum=0,validDocNum=0;
		for(Future<String> future:futures){
			String numStr=future.get();
			allEventNum+=Integer.parseInt(numStr.split(",")[0]);
			validDocNum+=Integer.parseInt(numStr.split(",")[1]);
		}
		System.out.println("allEventNum="+allEventNum);
		System.out.println("validDocNum="+validDocNum);
		PrintWriter writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream("docEventNumMap_"+WebPageAnalyzer.aprop.getEventName()+".txt"),"utf8"));
		for(String docName:docEventNumMap.keySet()){
			if (isPrecision==0||docEventNumMap.get(docName)>0) {
				writer.println(docName+"\t"+docEventNumMap.get(docName));
			}
		}
		writer.close();
	}
	
	public int writeEventResultsOfDoc(File dataFile,int resultDirNum) throws Exception{
		int eventNum=0;
		Page page=new Page();
		page.setId(dataFile.getName().split("\\.")[0]);
		FileInputStream inputStream=new FileInputStream(dataFile.getAbsoluteFile());
		BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream, "utf8"));
		char[] cbuf=new char[1000];
		StringBuffer text=new StringBuffer();
		int len=0;
		while((len=reader.read(cbuf))!=-1){
			text.append(cbuf, 0, len);
		}
		page.setContent(text.toString());
		HashMap<Event,ArrayList<MatchedResult>> eventResults=extractEvent(page);
		StringBuffer htmlText=new StringBuffer(page.getContent());
		List<Event> events=WebPageAnalyzer.eventList.getEvents();
		ArrayList<MatchedResult> matchedResults=null;
		ArrayList<Integer> eventTypes=new ArrayList<Integer>();
		if (!WebPageAnalyzer.aprop.getEventName().equals("all")) {
			String[] types=WebPageAnalyzer.aprop.getEventName().split(",\\s*");
			for (int i = 0; i < types.length; i++) {
				eventTypes.add(Integer.parseInt(types[i]));
			}
		}
		for(Event event:events){
//			if(event.getEventID()==Integer.parseInt(WebPageAnalyzer.aprop.getEventName())){
			if(WebPageAnalyzer.aprop.getEventName().equals("all")||eventTypes.contains(event.getEventID())){
//				matchedResults=eventResults.get(event)==null?null:new ArrayList<MatchedResult>(eventResults.get(event));
				ArrayList<MatchedResult> tempResults=null;
				if ((tempResults=eventResults.get(event))!=null) {
					if (matchedResults==null) {
						matchedResults=new ArrayList<MatchedResult>(tempResults); 
					}else {
						matchedResults.addAll(tempResults);
					}
				}
				
			}
		}
		
		if(matchedResults!=null){
			Collections.sort(matchedResults,new Comparator<MatchedResult>() {
				@Override
				public int compare(MatchedResult o1, MatchedResult o2) {
					// TODO Auto-generated method stub
					if(o1.getStart()>o2.getStart())
						return 1;
					else if(o1.getStart()<o2.getStart()){
						return -1;
					}
					return 0;
				}
			});
//			allExtractNum+=matchedResults.size();
//			availableDocNum++;
			eventNum=matchedResults.size();
			docEventNumMap.put(dataFile.getName().split("\\.")[0], matchedResults.size());
		}else {
			docEventNumMap.put(dataFile.getName().split("\\.")[0], 0);
		}
		
		if(matchedResults!=null){
			ArrayList<MatchedResult> mergedResults=new ArrayList<MatchedResult>();
			for(MatchedResult matchedResult:matchedResults){
				MatchedResult newMergeResult=new MatchedResult();
				int i=mergedResults.size()-1;
				while(i>=0&&matchedResult.getStart()<mergedResults.get(i).getStart()){
					i--;
				}
				int mergeFlag=0;
				if(i<mergedResults.size()-1){
					if(matchedResult.getEnd()>=mergedResults.get(i+1).getStart()){//和后一个有交叉
						newMergeResult.setStart(Math.min(matchedResult.getStart(),mergedResults.get(i+1).getStart()));
						newMergeResult.setEnd(Math.max(matchedResult.getEnd(), mergedResults.get(i+1).getEnd()));
						mergedResults.remove(i+1);
						mergedResults.add(i+1, newMergeResult);
						mergeFlag=1;
					}
				}
				if(i>=0){
					if(matchedResult.getStart()<=mergedResults.get(i).getEnd()){//和前一个有交叉
						newMergeResult.setStart(Math.min(matchedResult.getStart(),mergedResults.get(i).getStart()));
						newMergeResult.setEnd(Math.max(matchedResult.getEnd(), mergedResults.get(i).getEnd()));
						mergedResults.remove(i);
						mergedResults.add(i,newMergeResult);
						mergeFlag=1;
					}
				}
				if(mergeFlag==0){
					newMergeResult.setStart(matchedResult.getStart());
					newMergeResult.setEnd(matchedResult.getEnd());
					mergedResults.add(i+1,newMergeResult);
				}
			}
			for(int j=mergedResults.size()-1;j>=0;j--){
				htmlText.insert(mergedResults.get(j).getEnd(), "</span>");
				htmlText.insert(mergedResults.get(j).getStart(), "<span class=\"eventlabel\">");
			}
		}
		List<ResultRegion> allRegions=ExtractRegion.extract(htmlText.toString());
		String dirNumStr="";
		if (annotatorNum>1) {
			dirNumStr+="_"+resultDirNum;
		}
		File rsHtmDir=new File(resultDir+"_html"+dirNumStr);
		
		if(isPrecision==0||matchedResults!=null){
			if(!rsHtmDir.exists())
				rsHtmDir.mkdir();
			PrintWriter htmWriter=new PrintWriter(new OutputStreamWriter(new FileOutputStream(resultDir+"_html"+dirNumStr+File.separator+dataFile.getName().substring(0, dataFile.getName().indexOf("."))+".html"), "utf8"));
			htmWriter.println(MarkRegion.mark(htmlText.toString(), allRegions));
			htmWriter.close();
		}
		
		PrintWriter writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream(resultDir+File.separator+dataFile.getName()), "utf8"));
		for(Event event:eventResults.keySet()){
			int eventFirstWrite=0;
			
			for(MatchedResult eventResult:eventResults.get(event)){
				int eventNewsFlag=0;		
				int start = eventResult.getStart();
				int end = eventResult.getEnd();
				int first = Math.max(0, start - 30);
				int last = Math.min(text.length(), end + 30);
				String coretext = text.substring(start, end);
				StringBuffer sb = new StringBuffer();
				sb.append(text.substring(first, start));
				sb.append("<span class=\"eventlabel\">" + coretext + "</span>");
				sb.append(text.substring(end, last));
				String eContent = sb.toString();
				List<ResultRegion> result = ExtractRegion.extract(eContent);
				String markContent=MarkRegion.mark(eContent, result).replaceAll("\n", " ");
//				System.out.print(event.getEventName()+" "+eventResult.getT().getTemplateRule()+" "+page.getContent().substring(eventResult.getStart(),eventResult.getEnd())+" ");
				writer.print(event.getEventName()+"\t"+eventResult.getT().getTemplateRule()+"\t"+page.getContent().substring(eventResult.getStart(),eventResult.getEnd())+"\t"+markContent+"\t");
				if(eventFirstWrite==1){
//					System.out.println("已有同类事件入库");
					writer.println("已有同类事件入库");
					continue;
				}
				int resfirst = Math.max(0, start-4);          //判断是否包含限定词，取事件前后加上4个字
				int reslast = Math.min(text.length(), end+4);
				String restext = text.substring(resfirst, reslast);
				Pattern resp = PatternAgent.getPattern(restrictWordList);
				Matcher resm= resp.matcher(restext);
				if (resm.find()) {
					//如果匹配到的限定词，则返回
//					System.out.println("周围出现限定词:"+resm.group());
					writer.println("周围出现限定词:"+resm.group());
					continue;
				}
				if(result.size()==0){
//					System.out.println("无地名");
					writer.println("无地名");
					continue;
				}
//				System.out.println("写入数据库");
				writer.println("写入数据库");
				eventFirstWrite=1;
			}
		}
		writer.close();
		return eventNum;
	}
	public static void writeEventResults(String dataDir,String resultDir) throws Exception{
		int allExtractNum=0,availableDocNum=0;
		File dataDirfile=new File(dataDir);
		File rsDirFile=new File(resultDir);
		File rsHtmDir=new File(resultDir+"_html");
		if(!rsDirFile.exists())
			rsDirFile.mkdir();
		if(!rsHtmDir.exists())
			rsHtmDir.mkdir();
		for(File dataFile:dataDirfile.listFiles()){
			System.out.println(dataFile.getAbsolutePath());
			Page page=new Page();
			page.setId(dataFile.getName().split("\\.")[0]);
			FileInputStream inputStream=new FileInputStream(dataFile.getAbsoluteFile());
			BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream, "utf8"));
			char[] cbuf=new char[1000];
			StringBuffer text=new StringBuffer();
			int len=0;
			while((len=reader.read(cbuf))!=-1){
				text.append(cbuf, 0, len);
			}
			page.setContent(text.toString());
//			page.setContent("医院黑心医生南京造假");
			HashMap<Event,ArrayList<MatchedResult>> eventResults=extractEvent(page);
			System.out.println("eventResult");
			//E:\workspace\testRegion\evaResult
			
			StringBuffer htmlText=new StringBuffer(page.getContent());
			List<Event> events=WebPageAnalyzer.eventList.getEvents();
			ArrayList<MatchedResult> matchedResults=null;
			for(Event event:events){
				if(event.getEventID()==Integer.parseInt(WebPageAnalyzer.aprop.getEventName())){
					matchedResults=eventResults.get(event)==null?null:new ArrayList<MatchedResult>(eventResults.get(event));
					if(matchedResults!=null){
						Collections.sort(matchedResults,new Comparator<MatchedResult>() {
							@Override
							public int compare(MatchedResult o1, MatchedResult o2) {
								// TODO Auto-generated method stub
								if(o1.getStart()>o2.getStart())
									return 1;
								else if(o1.getStart()<o2.getStart()){
									return -1;
								}
								return 0;
							}
						});
						allExtractNum+=matchedResults.size();
						availableDocNum++;
					}
					break;
				}
			}
			if(matchedResults!=null){
//				System.err.println(matchedResults.size());
				ArrayList<MatchedResult> mergedResults=new ArrayList<MatchedResult>();
				for(MatchedResult matchedResult:matchedResults){
//					System.err.println(matchedResult.getStart()+" "+matchedResult.getEnd());
					MatchedResult newMergeResult=new MatchedResult();
					int i=mergedResults.size()-1;
					while(i>=0&&matchedResult.getStart()<mergedResults.get(i).getStart()){
						i--;
					}
					int mergeFlag=0;
					if(i<mergedResults.size()-1){
						if(matchedResult.getEnd()>=mergedResults.get(i+1).getStart()){//和后一个有交叉
							newMergeResult.setStart(Math.min(matchedResult.getStart(),mergedResults.get(i+1).getStart()));
							newMergeResult.setEnd(Math.max(matchedResult.getEnd(), mergedResults.get(i+1).getEnd()));
							mergedResults.remove(i+1);
							mergedResults.add(i+1, newMergeResult);
							mergeFlag=1;
						}
					}
					if(i>=0){
						if(matchedResult.getStart()<=mergedResults.get(i).getEnd()){//和前一个有交叉
							newMergeResult.setStart(Math.min(matchedResult.getStart(),mergedResults.get(i).getStart()));
							newMergeResult.setEnd(Math.max(matchedResult.getEnd(), mergedResults.get(i).getEnd()));
							mergedResults.remove(i);
							mergedResults.add(i,newMergeResult);
							mergeFlag=1;
						}
					}
					if(mergeFlag==0){
						newMergeResult.setStart(matchedResult.getStart());
						newMergeResult.setEnd(matchedResult.getEnd());
						mergedResults.add(i+1,newMergeResult);
					}
				}
//				System.err.println(mergedResults.size());
				for(int j=mergedResults.size()-1;j>=0;j--){
//					System.err.println("add");
					htmlText.insert(mergedResults.get(j).getEnd(), "</span>");
//					htmlText.insert(mergedResults.get(j).getStart(), "<span style=\"color:red\">");
					htmlText.insert(mergedResults.get(j).getStart(), "<span class=\"eventlabel\">");
				}
			}
			List<ResultRegion> allRegions=ExtractRegion.extract(htmlText.toString());
//			MarkRegion.mark(htmlText.toString(), allRegions);
			PrintWriter htmWriter=new PrintWriter(new OutputStreamWriter(new FileOutputStream(resultDir+"_html"+File.separator+dataFile.getName().substring(0, dataFile.getName().indexOf("."))+".html"), "utf8"));
//			"<meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\"><style>span.arealabel{color:green} span.eventlabel{color:red}</style>"+
			htmWriter.println(MarkRegion.mark(htmlText.toString(), allRegions));
			htmWriter.close();
			/*int eventFirstWrite=0;
			for(MatchedResult eventResult:matchedResults){
				int eventNewsFlag=0;
				
				int start = eventResult.getStart();
				int end = eventResult.getEnd();
				int first = Math.max(0, start - 30);
				int last = Math.min(text.length(), end + 30);
				String coretext = text.substring(start, end);
				StringBuffer sb = new StringBuffer();
				sb.append(text.substring(first, start));
				sb.append("<span class=\"eventlabel\">" + coretext + "</span>");
				sb.append(text.substring(end, last));
				String eContent = sb.toString();
				List<ResultRegion> result = ExtractRegion.extract(eContent);
				String markContent=MarkRegion.mark(eContent, result).replaceAll("\n", " ");
				System.out.print("医疗"+" "+eventResult.getT().getTemplateRule()+" "+page.getContent().substring(eventResult.getStart(),eventResult.getEnd())+" ");
				if(eventFirstWrite==1){
					System.out.println("已有同类事件入库");
					continue;
				}
//				int start = eventResult.getStart();
//				int end = eventResult.getEnd();
//				int first = Math.max(0, start - 30);
//				int last = Math.min(text.length(), end + 30);
//				String coretext = text.substring(start, end);
				int resfirst = Math.max(0, start-4);          //判断是否包含限定词，取事件前后加上4个字
				int reslast = Math.min(text.length(), end+4);
				String restext = text.substring(resfirst, reslast);
				Pattern resp = PatternAgent.getPattern(restrictWordList);
				Matcher resm= resp.matcher(restext);
				if (resm.find()) {
					//如果匹配到的限定词，则返回
					System.out.println("周围出现限定词:"+resm.group());
					continue;
				}
//				StringBuffer sb = new StringBuffer();
//				sb.append(text.substring(first, start));
//				sb.append("<span class=\"eventlabel\">" + coretext + "</span>");
//				sb.append(text.substring(end, last));
//				String eContent = sb.toString();
//				List<ResultRegion> result = ExtractRegion.extract(eContent);
				if(result.size()==0){
					System.out.println("无地名");
					continue;
				}
				System.out.println("写入数据库");
				eventFirstWrite=1;
			}*/
//			break;
			PrintWriter writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream(resultDir+File.separator+dataFile.getName()), "utf8"));
			for(Event event:eventResults.keySet()){
				int eventFirstWrite=0;
				
				for(MatchedResult eventResult:eventResults.get(event)){
					int eventNewsFlag=0;
					
					int start = eventResult.getStart();
					int end = eventResult.getEnd();
					int first = Math.max(0, start - 30);
					int last = Math.min(text.length(), end + 30);
					String coretext = text.substring(start, end);
					StringBuffer sb = new StringBuffer();
					sb.append(text.substring(first, start));
					sb.append("<span class=\"eventlabel\">" + coretext + "</span>");
					sb.append(text.substring(end, last));
					String eContent = sb.toString();
					List<ResultRegion> result = ExtractRegion.extract(eContent);
					String markContent=MarkRegion.mark(eContent, result).replaceAll("\n", " ");
//					System.out.print(event.getEventName()+" "+eventResult.getT().getTemplateRule()+" "+page.getContent().substring(eventResult.getStart(),eventResult.getEnd())+" ");
					writer.print(event.getEventName()+"\t"+eventResult.getT().getTemplateRule()+"\t"+page.getContent().substring(eventResult.getStart(),eventResult.getEnd())+"\t"+markContent+"\t");
					if(eventFirstWrite==1){
//						System.out.println("已有同类事件入库");
						writer.println("已有同类事件入库");
						continue;
					}
//					int start = eventResult.getStart();
//					int end = eventResult.getEnd();
//					int first = Math.max(0, start - 30);
//					int last = Math.min(text.length(), end + 30);
//					String coretext = text.substring(start, end);
					int resfirst = Math.max(0, start-4);          //判断是否包含限定词，取事件前后加上4个字
					int reslast = Math.min(text.length(), end+4);
					String restext = text.substring(resfirst, reslast);
					Pattern resp = PatternAgent.getPattern(restrictWordList);
					Matcher resm= resp.matcher(restext);
					if (resm.find()) {
						//如果匹配到的限定词，则返回
//						System.out.println("周围出现限定词:"+resm.group());
						writer.println("周围出现限定词:"+resm.group());
						continue;
					}
//					StringBuffer sb = new StringBuffer();
//					sb.append(text.substring(first, start));
//					sb.append("<span class=\"eventlabel\">" + coretext + "</span>");
//					sb.append(text.substring(end, last));
//					String eContent = sb.toString();
//					List<ResultRegion> result = ExtractRegion.extract(eContent);
					if(result.size()==0){
//						System.out.println("无地名");
						writer.println("无地名");
						continue;
					}
//					System.out.println("写入数据库");
					writer.println("写入数据库");
					eventFirstWrite=1;
				}
			}
			writer.close();
		}
		System.out.println("allExtractNum="+allExtractNum);
		System.out.println("availableDocNum="+availableDocNum);
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
//		extractEvent("Copy of 所有修改.xml", "testContent1.txt");
//		addRuleFromXML("ontology_db.xml",true);
//		addRuleFromXML("finalAdd.xml",false);
		System.out.println(WebPageAnalyzer.conceptPublic);
//		System.out.println("媒体曝广".matches("媒体(?!.{0,4}?(曝|报道|：|表示|称|核实|质疑|广泛|称|公布|盘点|求助))"));
//		System.exit(0);
		//addRuleFromXML("ontology_car.xml",true);
		//extractEvent("data");
//		outputOntology();
		//extractEvent("finalAdd.xml", "测试.txt");
		//writeTestData(32,"党政","2015-07-29 15:00:00","2015-07-30 15:00:00",100);
		/*Thread.sleep(8000);
		WebPageAnalyzer.refreshOntology();
		extractEvent("finalAdd.xml", "testData\\准确率测试.txt");*/
//		extractEvent("addRules2.xml", "testData\\征地拆迁.txt");
//		writeEventResults("E:\\workspace\\testRegion\\evaData_14", "E:\\workspace\\testRegion\\evaResult_14");
//		writeEventResults("C:\\Users\\Bys\\Desktop\\1\\公权滥用33\\evaData_33", "temp");
//		stEventResults("E:\\workspace\\testRegion\\evaData_14");
//		stEventResults(args[0]);
//		annotatorNum=2;
//		writeEventResultsOfDocs("C:\\Users\\Bys\\Desktop\\1\\公权滥用33\\evaData_33", "evaResult_33");
//		addRuleFromXML("finalAdd.xml");
//		isPrecision=1;
//		writeEventResultsOfDocs("hahaha", "hehehe");
		/*if(args[0].equals("write")){
			if (args.length>3) {
				annotatorNum=Integer.parseInt(args[3]);
				if(args.length>4){
					addRuleFromXML(args[4]);
					if (args.length>5) {
						isPrecision=Integer.parseInt(args[5]);
					}
				}
			}
			writeEventResultsOfDocs(args[1], args[2]);
		}
		else {
			stEventResults(args[1]);
		}*/
//		writeEventResults(args[0], args[1]);nohup java -jar Analysis.jar write evaData_100 evaResult_100 1
		System.exit(0);
//		WebPageAnalyzer analyzer=new WebPageAnalyzer();
//		analyzer.conceptPublic = WebPageAnalyzer.da.getEventByEventId(-1, "公共概念");
//		ArrayList<KeywordGroup> global_kg = analyzer.conceptPublic.getConcepts();
//		
//		System.out.println("通用事件初始化中……………………" + global_kg.size());
//
//		for (KeywordGroup global_k : global_kg) {
//			initMap(global_k, analyzer.global_concept);
//		}
//
//		
//		analyzer.eventList = WebPageAnalyzer.da.getEventList();// 初始化事件列表
//		System.out.println("WebPageAnalyzer.da.getEventList()"
//				+ WebPageAnalyzer.da);
//
//		analyzer.events = analyzer.eventList.getEvents(); // 获取事件
//		Event event_init = null;
//		for (int i = 0; i < analyzer.events.size() - 1; i++) {
//			event_init = (Event) analyzer.events.get(i);
//			ArrayList<Template> Template = event_init.getTemplates();
//			for (Template template : Template) {
//				for (KeywordGroup k : template.getConcepts()) // 匹配模板包含的所有概念
//				{
//					initMap(k, event_init.getConceptMap());
//				}
//			}
//		}
		Page page=new Page();
		FileInputStream inputStream=new FileInputStream("testEvent.txt");
		BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream, "GBK"));
		char[] cbuf=new char[1000];
		StringBuffer text=new StringBuffer();
		int len=0;
		while((len=reader.read(cbuf))!=-1){
			text.append(cbuf, 0, len);
		}
		page.setContent("医院黑心医生");
//		List<ResultRegion> results=ExtractRegion.extract(text.toString());
		page.setId("testcase1");
		ExtractRegion.extract("");
		PrintWriter writer=null;
		String id="00013E8C7A05E69DC011C9DC2CC26FF3";
		try {
			
			HashMap<Event,ArrayList<MatchedResult>> eventResults=extractEvent(page);
			System.out.println("eventResult");
			//E:\workspace\testRegion\evaResult
//			writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream("E:\\workspace\\testRegion\\evaResult\\"+id+".txt"), "utf8"));
			for(Event event:eventResults.keySet()){
				int eventFirstWrite=0;
				for(MatchedResult eventResult:eventResults.get(event)){
					int eventNewsFlag=0;
					System.out.print(event.getEventName()+" "+eventResult.getT().getTemplateRule()+" "+page.getContent().substring(eventResult.getStart(),eventResult.getEnd())+" ");
//					writer.print(event.getEventName()+" "+eventResult.getT().getTemplateRule()+" "+page.getContent().substring(eventResult.getStart(),eventResult.getEnd())+" ");
					if(eventFirstWrite==1){
						System.out.println("已有同类事件入库");
//						writer.println("已有同类事件入库");
						continue;
					}
					int start = eventResult.getStart();
					int end = eventResult.getEnd();
					int first = Math.max(0, start - 30);
					int last = Math.min(text.length(), end + 30);
					String coretext = text.substring(start, end);
					int resfirst = Math.max(0, start-4);          //判断是否包含限定词，取事件前后加上4个字
					int reslast = Math.min(text.length(), end+4);
					String restext = text.substring(resfirst, reslast);
					Pattern resp = PatternAgent.getPattern(restrictWordList);
					Matcher resm= resp.matcher(restext);
					if (resm.find()) {
						//如果匹配到的限定词，则返回
						System.out.println("周围出现限定词:"+resm.group());
//						writer.println("周围出现限定词:"+resm.group());
						continue;
					}
					StringBuffer sb = new StringBuffer();
					sb.append(text.substring(first, start));
					sb.append("<span class=\"eventlabel\">" + coretext + "</span>");
					sb.append(text.substring(end, last));
					String eContent = sb.toString();
					List<ResultRegion> result = ExtractRegion.extract(eContent);
					if(result.size()==0){
						System.out.println("无地名");
//						writer.println("无地名");
						continue;
					}
					System.out.println("写入数据库");
//					writer.println("写入数据库");
					eventFirstWrite=1;
				}
						
			}
//			System.out.println(MarkRegion.mark(text.toString(), results));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if (writer!=null) {
				writer.close();
			}
		}
	}
	@Override
	public String call() throws Exception {
		// TODO Auto-generated method stub
		int allEventNum=0,validDocNum=0;
		int perRsDirNum=docFiles.length/annotatorNum;
		for(int i=startFileIndex;i<=endFileIndex;i++){
			int resultDirNum=i/perRsDirNum+1;
			if (resultDirNum>annotatorNum) {
				resultDirNum=(int)(Math.random()*annotatorNum);
			}
			int eventNum=writeEventResultsOfDoc(docFiles[i],resultDirNum);
			allEventNum+=eventNum;
			if (eventNum!=0) {
				validDocNum++;
			}
		}
		return allEventNum+","+validDocNum;
	}
	
	public static void addConceptEle(KeywordGroup concept,Element element){
		Element conceptEle=element.addElement("concept");
		conceptEle.addAttribute("name", concept.getKeywordGroupName());
		StringBuffer keywordsStr=new StringBuffer();
		if(concept.getChildKeywordGroup()!=null&&concept.getChildKeywordGroup().size()>0){
			Element childEle=conceptEle.addElement("child");
			for(KeywordGroup childGroup:concept.getChildKeywordGroup()){
				keywordsStr.append(childGroup.getKeywordGroupName()+" ");
			}
			childEle.addText(keywordsStr.toString().trim());
		}else {
			for(Keyword keyword:concept.getKeywords()){
				keywordsStr.append(keyword.getKeywordName()+" ");
			}
			conceptEle.addText(keywordsStr.toString().trim());
		}
		
	}
	public static void addEventEle(Event event,Element element,Element rootEle){
		if(event.getParentEventId()>0&&element==rootEle){
			return;
		}
		List<Event> events=WebPageAnalyzer.eventList.getEvents();
		Element eventEle=element.addElement("event");
		eventEle.addAttribute("name", event.getEventName());
		eventEle.addAttribute("id", event.getEventID()+"");
		int flag=0;
		for(Event tempEvent:events){
			if (tempEvent.getParentEventId()==event.getEventID()) {
				addEventEle(tempEvent, eventEle,rootEle);
				flag=1;
			}
		}
		if (flag==0) {
			List<KeywordGroup> concepts=event.getConcepts();
			for(KeywordGroup concept:concepts){
				addConceptEle(concept, eventEle);
			}
			for(Template template:event.getTemplates()){
				Element templateEle=eventEle.addElement("template");
				templateEle.addAttribute("name", template.getTemplateName());
				templateEle.addText(template.getTemplateRule());
			}
		}
	}
	public static void outputOntology() throws Exception{
		SAXReader saxReader=new SAXReader();
		InputStream cfgIn=new FileInputStream("ontology_all.xml");
		Document document=saxReader.read(cfgIn).getDocument();
		cfgIn.close();
		Element rootElement=document.getRootElement();
		Element pubConceptsEle=rootElement.addElement("concepts");
		List<KeywordGroup> publicConcepts=WebPageAnalyzer.conceptPublic.getConcepts();
		for(KeywordGroup publicConcept:publicConcepts){
			addConceptEle(publicConcept, pubConceptsEle);
		}
		for(Event event:WebPageAnalyzer.eventList.getEvents()){
			addEventEle(event, rootElement, rootElement);
		}
		OutputFormat format=OutputFormat.createPrettyPrint();
		OutputStream out=new FileOutputStream("ontology_all_rs.xml");
		XMLWriter writer=new XMLWriter(out, format);
		writer.write(document);
		writer.close();
	}
	
	public static void writeTestData(int eventID,String eventName,String startDate,String endDate,int num) throws FileNotFoundException{
		int[] provinceIds=new int[]{13,11,12,41,42,14,15,22,23,32,33,34,35,36,37,43,44,45,46,50,51,52,53,54,61,62,63,64,65};
		FileOutputStream os=new FileOutputStream("准确率/"+WebPageAnalyzer.dateFormat.format(new Date())+"_"+eventName+".xlsx");
		Workbook workbook=new XSSFWorkbook();
		Sheet sheet=workbook.createSheet();
		Font redFont=workbook.createFont();
		redFont.setColor(HSSFColor.RED.index);
		redFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		Font blueFont=workbook.createFont();
		blueFont.setColor(HSSFColor.BLUE.index);
		blueFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		Font boldFont=workbook.createFont();
		boldFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		List<Event> events=WebPageAnalyzer.eventList.getEvents();
		Event e=null;
		for(Event tempEvent:events){
			if (tempEvent.getEventID()==eventID) {
				e=tempEvent;
				break;
			}
		}
		PoolConnectCfg poolconcfg = new PoolConnectCfg(AnalysisProperties.getInstance().getDirPath()+"DataAccess_app_Test.properties");
		PoolConection pl = null;
		try{
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			String sql="SELECT id,url,summary,publishDate from wdyq_eventnews where publishDate>'"+startDate+"' and publishDate<'"+endDate+"' and is_mainInfo=1 and eventId="+eventID+" and provinceId=";
			class TempEntity{
				String pageId;
				String context;
				String pureText;
				String template;
				Date publishDate;
			}
			int count=0,time=0;
			while(count<num){
				if(time>=provinceIds.length){
					System.err.println("num:"+count);
					break;
				}
				int provinceId=provinceIds[time++];
				String provinceSql=sql+provinceId;
				pl.prepareSql(provinceSql);
				ResultSet rs=pl.executeQuery();
				while(rs.next()){
					String context=rs.getString("summary");
	//				String context="哈哈哈<span class=\"eventlabel\">医生和医院眼科黑心</span>";
					String src=getCoretext(context);
					int eventLabelIndex=context.indexOf("<span class=\"eventlabel\">");
					if(eventLabelIndex<0){
						System.err.println(context);
						continue;
					}
					int foreLen=context.substring(0, eventLabelIndex).replaceAll("<span .*?>|</span>", "").length();
					if(src.length()==0)
						continue;
					String pageId=rs.getString("id");
					String url=rs.getString("url");
	//				String pageId="haha"+count;
					DecEventMatcher em = new DecEventMatcher();
					ArrayList<Template> templates = new ArrayList<Template>(e.getTemplates());
					ArrayList<MatchedResult> mrList=em.find(src, e, pageId, templates);
					if(mrList==null){
						System.err.println(context);
						continue;
					}
					ArrayList<MatchedResult> keywordsList=((DecMatchedResult)mrList.get(0)).keywordMRs;
					ArrayList<String> keywordNames=((DecMatchedResult)mrList.get(0)).keywordNames;
					Row row=sheet.createRow(count);
					int j=0;
					Cell cell=row.createCell(j++);
					cell.setCellValue(pageId);
					cell=row.createCell(j++);
					cell.setCellValue(url);
					cell=row.createCell(j++);
					cell.setCellValue(context);
					cell=row.createCell(j++);
					cell.setCellValue(mrList.get(0).getT().getTemplateRule());
					cell=row.createCell(j++);
					cell.setCellValue(WebPageAnalyzer.logDateFormat.format(rs.getTimestamp("publishDate")));
					cell=row.createCell(j++);
					String pureText=context.replaceAll("<span .*?>|</span>", "");
					RichTextString richString = new XSSFRichTextString(pureText);
					richString.applyFont(foreLen, foreLen+src.length(), boldFont);
					for(int i=0;i<keywordsList.size();i++){
						Font font=e.getConceptByName(keywordNames.get(i))!=null?blueFont:redFont;
						richString.applyFont(foreLen+keywordsList.get(i).getStart(), foreLen+keywordsList.get(i).getEnd(), font);
					}
					cell.setCellValue(richString);
					count++;
					if(count>=num)
						break;
	//				System.out.println(keywordsList);
					
				}
				rs.close();
			}
			sheet.autoSizeColumn(0);
			workbook.write(os);
			os.close();
			pl.close();
		}catch (Exception ex) {
			// TODO: handle exception
			ex.printStackTrace();
			if (pl!=null) {
				pl.close();
			}
			try {
				os.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	 public static void getConceptmap(KeywordGroup k,String src,String pageId)   //获取概念的文本匹配映射
	    {
			ArrayList<MatchedResult> concept_result = new ArrayList<MatchedResult>(0);
	    	if(k.getResultMaps().containsKey(pageId)){
	    		return;
	    	}
	    	else
	    	{
	    		if(k.getChildKeywordGroup().size()>0)
	    		{
	    		   List<KeywordGroup> childs =k.getChildKeywordGroup();
	    		   for(KeywordGroup ck:childs)
	    		   {
	    			   getConceptmap(ck,src,pageId);
	    			   ConceptTable childTable = ck.getResultMaps().get(pageId);
	    			   concept_result.addAll(childTable.getStartResultMaps());
	    		   }
	    		}
	    		else
	    		{
	    			Pattern p = Pattern.compile(k.generateExp());
	    			Matcher m = p.matcher(src);
	    			while(m.find())
	    			{
	    				concept_result.add(new MatchedResult(m.start(),m.end()));
	    			}
	    		}    		
	    		k.getResultMaps().put(pageId, new ConceptTable(concept_result,k.getKeywordGroupName()));
	    	}
	    }
	public static String getCoretext(String text) {
		String _text = text;
		StringBuffer coreText=new StringBuffer("");
		String eventLabel="<span class=\"eventlabel\">";
		String areaLabel="<span class=\"arealabel\">";
		String endLabel="</span>";
		Pattern pattern=Pattern.compile(eventLabel);
		Matcher matcher=pattern.matcher(text);
		int start=0,end=0,count=0;//count当栈使用
		if(matcher.find()){
			count=1;
			pattern=Pattern.compile(endLabel+"|"+areaLabel);
			text=text.substring(matcher.end());
			matcher=pattern.matcher(text);
			while(count!=0){
				if(matcher.find()){
					String label=matcher.group();
					if(label.equals(endLabel))
						count--;
					else 
						count++;
					coreText.append(text.substring(start,matcher.start()));
					start=matcher.end();
				}else{
					System.err.println(_text);
					return "";
				}
			}
		}
		return coreText.toString();
		

	}
	
	public static void writeData2xls(String fileDirStr)throws Exception{
		FileOutputStream os=new FileOutputStream(fileDirStr+"/data.xlsx");
		Workbook workbook=new XSSFWorkbook();
		Sheet sheet=workbook.createSheet();
		List<File> files=new ArrayList<File>();
		File fileDir=new File(fileDirStr+"/data");
		for(File file:fileDir.listFiles()){
			files.add(file);
		}
		fileDir=new File(fileDirStr+"/newdata");
		for(File file:fileDir.listFiles()){
			files.add(file);
		}
		int index=0;
		for(File file:files){
			StringBuffer text=new StringBuffer();
			BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf8"));
			String str=null;
			while((str=reader.readLine())!=null){
				text.append(str+"\n");
			}
			reader.close();
			Row row=sheet.createRow(index);
			Cell cell=row.createCell(0);
			cell.setCellValue(text.substring(0,text.length()-1));
			index++;
		}
		workbook.write(os);
		os.close();
	}
}
class DecMatchedResult extends MatchedResult{
	ArrayList<MatchedResult> keywordMRs=new ArrayList<MatchedResult>();
	ArrayList<String> keywordNames=new ArrayList<String>();
	public DecMatchedResult(int start,int end){
		super(start,end);
	}
}
class DecEventMatcher extends TempEventMatcher{

	@Override
	public ConceptTable parentheses_op(ConceptTable ct1, ConceptTable ct2,
			int distance) {
		ArrayList<MatchedResult> resultList = new ArrayList<MatchedResult>(0);
		boolean isStart = false;
		MatchedResult scope = new MatchedResult();
		for(MatchedResult mr:ct1.getStartResultMaps()){
			isStart = false;
			scope.setStart(0);
			scope.setEnd(mr.getEnd()+distance);
			
			for(MatchedResult mr2:ct2.getStartResultMaps()){
				if(mr.cross(mr2)){
					continue;
				}
//				如果不在范围内，且曾经开始过，也就是超出范围，则其后项必定也超出范围
				else if(!scope.contains(mr2)){
					if(isStart)
						break;
					}
//				如果mr2与mr扩展后的范围有交集，则将mr与mr2合并，设开始标记为真
				else{
					if(mr2.getEnd()>=mr.getStart()-distance){
						DecMatchedResult temp = new DecMatchedResult(mr.getStart()<mr2.getStart()?mr.getStart():mr2.getStart(),
								mr.getEnd()>mr2.getEnd()?mr.getEnd():mr2.getEnd());
						temp.keywordMRs.addAll(((DecMatchedResult)mr).keywordMRs);
						temp.keywordMRs.addAll(((DecMatchedResult)mr2).keywordMRs);
						temp.keywordNames.addAll(((DecMatchedResult)mr).keywordNames);
						temp.keywordNames.addAll(((DecMatchedResult)mr2).keywordNames);
						if(temp.getEnd()-temp.getStart()<30){
							resultList.add(temp);
							isStart = true;
						}
					}
				}
			}
		}
		scope = null;
		return new ConceptTable(resultList,ct1.getConceptName()+"({"+distance+"}"+ct2.getConceptName()+")");
	}

	@Override
	public ConceptTable plus_op(ConceptTable ct1, ConceptTable ct2,
			int distance) {
		ArrayList<MatchedResult> resultList = new ArrayList<MatchedResult>(0);
		boolean isStart = false;
		MatchedResult scope = new MatchedResult();
		for(MatchedResult mr:ct1.getStartResultMaps()){
			isStart = false;
			scope.setStart(mr.getEnd());
			scope.setEnd(mr.getEnd()+distance);
			
			for(MatchedResult mr2:ct2.getStartResultMaps()){
				if(mr.cross(mr2)){
					continue;
				}
//				如果不在范围内，且曾经开始过，也就是超出范围，则其后项必定也超出范围
				if(!scope.contains(mr2)){
					if(isStart)
						break;
				}
//				如果mr2与mr扩展后的范围有交集，则将mr与mr2合并，设开始标记为真
				else{
					DecMatchedResult temp = new DecMatchedResult(mr.getStart()<mr2.getStart()?mr.getStart():mr2.getStart(),
							mr.getEnd()>mr2.getEnd()?mr.getEnd():mr2.getEnd());						
					temp.keywordMRs.addAll(((DecMatchedResult)mr).keywordMRs);
					temp.keywordMRs.addAll(((DecMatchedResult)mr2).keywordMRs);
					temp.keywordNames.addAll(((DecMatchedResult)mr).keywordNames);
					temp.keywordNames.addAll(((DecMatchedResult)mr2).keywordNames);
					if(temp.getEnd()-temp.getStart()<30){
						resultList.add(temp);
						isStart = true;
					}
				}
			}
		}
		scope = null;
		return new ConceptTable(resultList,ct1.getConceptName()+"{"+distance+"}"+ct2.getConceptName());
	}

	@Override
	public void getConceptmap(KeywordGroup k, String src, String pageId) {
		ArrayList<MatchedResult> concept_result = new ArrayList<MatchedResult>(0);
    	if(k.getResultMaps().containsKey(pageId)){
    		return;
    	}
    	else
    	{
    		if(k.getChildKeywordGroup().size()>0)
    		{
    		   List<KeywordGroup> childs =k.getChildKeywordGroup();
    		   for(KeywordGroup ck:childs)
    		   {
    			   getConceptmap(ck,src,pageId);
    			   ConceptTable childTable = ck.getResultMaps().get(pageId);
    			   concept_result.addAll(childTable.getStartResultMaps());
    		   }
    		}
    		else
    		{
    			Pattern p = Pattern.compile(k.generateExp());
    			Matcher m = p.matcher(src);
    			while(m.find())
    			{
    				DecMatchedResult temp = new DecMatchedResult(m.start(),m.end());
    				temp.keywordMRs.add(new MatchedResult(m.start(),m.end()));
    				temp.keywordNames.add(k.getKeywordGroupName());
    				concept_result.add(temp);
    			}
    		}    		
    		k.getResultMaps().put(pageId, new ConceptTable(concept_result,k.getKeywordGroupName()));


    	}
	}
	
//	public static void getRecallData(){
//		PoolConnectCfg poolconcfg = new PoolConnectCfg(AnalysisProperties.getInstance().getDirPath()+"DataAccess_app_Test.properties");
//		PoolConection pl = null;
//
////		pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
//
//	}
	
}
