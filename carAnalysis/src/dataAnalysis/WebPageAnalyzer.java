package dataAnalysis;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.sun.org.apache.bcel.internal.generic.NEW;






import javabean.AnalysisUnits;
import javabean.EventList;
import javabean.Keyword;
import javabean.MatchedResult;
import javabean.db.Event;
import javabean.db.KeywordGroup;
import javabean.db.Page;
import javabean.db.Template;
import snooway.dao.DataDao;
import sun.misc.Signal;
import util.AnalysisProperties;
import util.AnalysisUtil;
import util.AnoDupNewsCache;
import util.DupNewsCache;
import util.SignalCatch;
import util.TimeUtil;
import whu.nlp.extracter.util.RegionConfig;

/**
 * �����������
 * @author Bys
 *
 */
public class WebPageAnalyzer implements Callable<Boolean> {

	public static AnalysisProperties aprop = AnalysisProperties.getInstance();
	public AnalysisUtil au = new AnalysisUtil();
	// provinceId=aprop.getProvinceId();

	private static String mustContainWord = aprop.getMustContainWords();
	private static String postMatchClass = aprop.getPostMatchClass();
	private static String eventName = aprop.getEventName();
	private static boolean loopOnce = aprop.isLoopOnce();
	private static int refreshTime = aprop.getRefreshTime();
	private static int threadsNum = aprop.getThreadsNum();
	private static int appNum=aprop.getAppNum();
	public static SignalCatch singalCatch;
	static final int NoStop = 0, DocMatchStop = 1, EventMatchStop = 2,
			TemMatchStop = 3;
	private static final boolean isShow = false;

	private static long get_page_time,dup_time,analysis_time,region_ex_time;
	
	
//	static int pageCount = 0;
//	static int recordCount = 0;
//	static long recordLong = 0L;
//	static long startLong = 0L;
//	static PrintStream threadRecord = null;
	
	int start = 0;
	int end = 0;
	int index = 0;
	public static DataDao da = DataDao.createDao();

	public static List<Page> pageList = null;
	public static EventList eventList = null;// ����¼��б�
	public static List<Event> events = null; // ��ȡ�¼�
	public static Event conceptPublic = null; // ��������
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static String todayDate = dateFormat.format(new Date())/*"2014-01-18"*/;
	public static ConcurrentHashMap<Integer, Integer> proSubCountMap=new ConcurrentHashMap<Integer, Integer>();
	public static ConcurrentHashMap<Integer, Integer> tempProSubMap=new ConcurrentHashMap<Integer, Integer>();
	public static ConcurrentHashMap<String, Integer> unAnaPageList = new ConcurrentHashMap<String, Integer>(30000);
	public static ConcurrentHashMap<String, KeywordGroup> global_concept = new ConcurrentHashMap<String, KeywordGroup>(0);
	
	private static boolean testOld=aprop.isDebug();
	
//	public static
	
	/**
	 * ������̷߳�������ʼ������page
	 * @param start ���̷߳��������
	 * @param end ���̷߳������յ�
	 * @param index �̺߳�
	 */
	WebPageAnalyzer(int start, int end, int index) // ������̷߳�������ʼ������page
	{
		this.start = start;
		this.end = end;
		this.index = index;
		System.out.println(start + " " + end + " " + index);
	}

	WebPageAnalyzer() // ������̷߳�������ʼ������page
	{

	}

	/**
	 * ��ʼ�������б�����������������ϣӳ��
	 * 
	 * @param kg
	 *            ��Ҫӳ��ĸ���
	 * @param map
	 *            ����ӳ���ϵ�Ĺ�ϣ��
	 */
	private static void initMap(KeywordGroup kg,
			ConcurrentHashMap<String, KeywordGroup> map) {
		// System.out.println(kg.getKeywordGroupName());
		for (KeywordGroup child : kg.getChildKeywordGroup()) {
			initMap(child, map);
		}
		map.put(kg.getKeywordGroupName(), kg);
	}

//	/**
//	 * ͳ�Ʒ���ʱ�����÷���
//	 */
//	static synchronized void addCount(){
//		pageCount ++;
//		if((pageCount+1)%1000==0){
//			recordCount++;
//			long lastRecord = recordLong;
//			recordLong = new Date().getTime();
//			threadRecord.println(aprop.getThreadsNum()+"���̷߳�������ǧƪ��ʱ"+(double)(recordLong-lastRecord)/1000+"��");
//			threadRecord.println(aprop.getThreadsNum()+"���̷߳���"+(pageCount+1)+"ƪ�ù���ʱ"+(double)(recordLong-startLong)/1000+"��\tƽ��ÿǧƪ��ʱ:"+(double)(recordLong-startLong)/1000/recordCount+"��");
//			if(recordCount == 10)
//				System.exit(0);
//		}
//	}
	
	/**
	 * ����pageId�ڹ��������в�����ӳ���
	 * 
	 * @param pageId
	 *            ��Ҫ���������id
	 */
	public void cleanConceptmap(String pageId) {
		for (String kName : global_concept.keySet()) {
			global_concept.get(kName).getResultMaps().remove(pageId);
		}
	}

	/**
	 * ����ĳ��page
	 * @param page ĳƪ�ĵ�
	 * @throws Exception
	 */
	public void AnalyzePage(Page page) throws Exception {
		boolean estop;// ��ʾ�Ƿ�ֹͣƥ��ĳ���¼�
		boolean dstop = false;// ��ʾ�Ƿ�ֹͣƥ��ĳƪ����
		boolean isMatch = false;// ��ʾ�Ƿ�ƥ�䵽
		// List<Event> events = null;
		events = eventList.getEvents(); // ��ȡ�¼�
		/*
		 * ��ÿ�����ӱ���ÿ���¼� modified by syq
		 */
		PostMatcher htpm = null;
		String str = page.getContent();
		String pageId = page.getId();

		if (str == null) {
			System.out.println("id" + pageId + "���ݻ�ȡʧ��");
			return;
		}
		// Vector<String> used = new Vector<String>(0);
		// usedMap.put(pageId, used);
		try {
			htpm = (PostMatcher) Class.forName(postMatchClass).newInstance();

			if (AnalysisProperties.getInstance().isDebug()) {
				System.out.println("[" + this.index + "] PostMatcher loaded");
				System.out.println("[" + this.index + "] " + events.size()
						+ " events to be go through");
			}
			Event e = null;
			isMatch = false;	
			
			long befindTime,findTime=0;
			
			for (int i = 0; i < events.size() - 1; i++) {

				estop = false;

				e = (Event) events.get(i);
				// ���Ҫ��ĳһ�����¼��Ĺ���������ѭ��
				if (!eventName.equals("all")
						&& !e.getEventName().equals(eventName))
					continue;
				try {
					EventMatcher em = new EventMatcher();
					ArrayList<MatchedResult> mrList;

					// ArrayList<KeywordGroup> concepts = e.getConcepts();
					ArrayList<Template> templates = new ArrayList<Template>(
							e.getTemplates());
					
					befindTime=System.currentTimeMillis();
					while ((mrList = em.find(str, e, pageId, templates)) != null) {
						findTime+=System.currentTimeMillis()-befindTime;
						
						
						System.out.println("[" + this.index + "] "+pageId+" ƥ�䵽�¼�������������");
//						System.out.println(str.substring(mrList.get(0).getStart(),mrList.get(0).getEnd()));int flag=1;if(flag==1)continue;
						for (MatchedResult mr : mrList) {
							System.out.println("[" + this.index + "] "+pageId+" postMatcher start������������");
							int stopFlag = htpm.postMatcher(mr, page, e);
							System.out.println("[" + this.index + "] "+pageId+" postMatcher end������������");
							System.out.println("stopFlag" + stopFlag);
							if (isShow)
								System.out.println(stopFlag + "!!!!1"
										+ e.getEventID());
							switch (stopFlag) {
							case NoStop:
								break;
							case TemMatchStop:
								break;
							case DocMatchStop:
								dstop = true;
								break;
							case EventMatchStop:
								isMatch = true;
								estop = true;
								break;
							}
							if (estop || dstop)
								break;
						}
						
						befindTime=System.currentTimeMillis();
						
						if (estop || dstop)
							break;
						
						
						
					}
					
					findTime+=System.currentTimeMillis()-befindTime;
					
					
					if (em != null) {
						em = null;
					}
				} catch (Exception eventGerE) {
					eventGerE.printStackTrace();
				}

				if (dstop)
					break;
			}
			
			if(aprop.isTestTime())
				page.findDocTime=TimeUtil.decimalFormat.format(((double)findTime)/1000);
			
			if (!isMatch)
				System.out.println("[" + this.index + "] "+pageId+"û��ƥ�䵽�¼���");
			if (isShow)
				System.out.println("[" + this.index + "]  events ƥ�����ˡ�");
			
			System.out.println("[" + this.index + "] "+pageId+" ������ɡ�����������");
			for (int i = 0; i < events.size() - 1; i++) {
				events.get(i).cleanMap(pageId);
			}
			
			cleanConceptmap(pageId);
			
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} finally {
			// System.gc();
		}

	}

	/**
	 * ����������־
	 * �Ժ��Ƴ�����
	 */
	public static void cleanOldLog() {
		File logDir = new File(aprop.getLogLocaltion());
		Date logDate = null;
		Calendar calendar=Calendar.getInstance();   
		try {
			calendar.setTime(dateFormat.parse(todayDate));
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		calendar.set(Calendar.DAY_OF_MONTH,calendar.get(Calendar.DAY_OF_MONTH)-6);
		Date deleteDate = calendar.getTime();
		System.out.println(logDateFormat.format(deleteDate));
		if (!logDir.isDirectory()) {
			System.err.println(logDateFormat.format(new Date()) + ":��־·������");
		} else {
			try {
//				File[] logs = logDir.listFiles();
				File[] logs = logDir.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						// TODO Auto-generated method stub
						if(pathname.getName().endsWith(".log"))
							return true;
						return false;
					}
				});
				for (int i = 0; i < logs.length; i++) {
					if (logs[i].isFile()) {
						if (logs[i].getName().contains("analysis")) {
							logDate = dateFormat.parse(logs[i].getName().split("analysis")[1].split("//.")[0]);
							if(logDate.before(deleteDate)){								
								System.out.println("ɾ��������־��"+ logs[i].getAbsolutePath());
								logs[i].delete();		
							}
						} else if (logs[i].getName().contains("error")) {
							logDate = dateFormat.parse(logs[i].getName().split("error")[1].split("//.")[0]);
							if(logDate.before(deleteDate)){								
								System.out.println("ɾ��������־��"+ logs[i].getAbsolutePath());
								logs[i].delete();		
							}
						}
					}
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void cleanOldCount(){
		WebPageAnalyzer.da.cleanSubjectCount();
	}
	public static void initProSubCount(){
		WebPageAnalyzer.da.initProSubCount();
	}
	/*public static void addOntologys(String addFileName){
		List<Event> events=WebPageAnalyzer.eventList.getEvents();
		SAXReader saxReader=new SAXReader();
		Document document=null;
		try {
			document = saxReader.read(new FileInputStream(addFileName));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		Element rootEle=document.getRootElement();
		List<Element> eventAddEles=rootEle.elements("eventAdd");
		for(Element eventAddEle:eventAddEles){
			String eventName=eventAddEle.attribute("name").getValue();
			String eventID=eventAddEle.attribute("id").getValue();
			if(eventID.equals("-1")){
				Event event=WebPageAnalyzer.conceptPublic;
				List<Element> conceptEles=eventAddEle.elements("concept");
				if(conceptEles!=null&&conceptEles.size()>0){
					ArrayList<Element> addParentKgEles=new ArrayList<Element>();
					ArrayList<Integer> addParentKgHadFlags=new ArrayList<Integer>();
					for(Element conceptEle:conceptEles){
						String conceptName=conceptEle.attributeValue("name").trim();
						KeywordGroup concept;
						int hadConcept=0;
						if((concept=WebPageAnalyzer.global_concept.get(conceptName))!=null){//event�����и�concept
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
							}
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
				continue;
			}
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
							if((concept=event.getConceptByName(conceptName))!=null){//event�����и�concept
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
								}
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
										addParentKg.getChildKeywordGroup().add(event.getConceptMap().get(childKgName)==null?WebPageAnalyzer.global_concept.get(childKgName):event.getConceptMap().get(childKgName));
									}
								}
								
							}
						}
					}
					
					List<Element> templateElements=eventAddEle.elements("template");
					if(templateElements!=null&&templateElements.size()>0){
						for(Element templateElement:templateElements){
							Template template=new Template();
							template.setTemplateName(templateElement.attributeValue("name"));
							template.setTemplateRule(templateElement.getText().replaceAll("\\s", ""));
							event.getTemplates().add(template);
						}
					}		
					break;
				}
			}
		}
	}*/
	/**
	 * �����ڱ��ʱ�������в���
	 * ��Ҫ�������µ���ʱ����todayDate������һ��ǰ��־����������־������ȥ�ػ���
	 */
	public static void dayChange() {
		todayDate = dateFormat.format(new Date());
		
		DupNewsCache.removeCache(new Date(new Date().getTime()-24L*3600*1000*2));
		DupNewsCache.addCache(new Date());
		if(appNum!=1){
			AnoDupNewsCache.removeCache(new Date(new Date().getTime()-24L*3600*1000*2));
			AnoDupNewsCache.addCache(new Date());
			cleanOldCount();
		}
		
		/*
		 * ����ֻҪeventnews_all
		 * 2014-6-17 15:26:51
		dupNews.clear();
		*/
		cleanOldLog();
//		cleanOldCount();
		if (AnalysisProperties.getInstance().isDebug() == false) {
			FileOutputStream outfile;
			try {
				outfile = new FileOutputStream(aprop.getLogLocaltion()
						+ "/analysis" + dateFormat.format(new Date()) + ".log",
						true);
				PrintStream ps = new PrintStream(outfile);
				FileOutputStream errfile = new FileOutputStream(
						aprop.getLogLocaltion() + "/error"
								+ dateFormat.format(new Date()) + ".log", true);
				PrintStream psErr = new PrintStream(errfile);
				System.setOut(ps);
				System.setErr(psErr);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void init() {
		todayDate = dateFormat.format(new Date());
		
		
		
		
		
		/*
		 * ����ֻҪeventnews_all
		 * 2014-6-17 15:26:51
		dupNews.clear();
		*/
		cleanOldLog();
		if (AnalysisProperties.getInstance().isDebug() == false) {
			FileOutputStream outfile;
			try {
				outfile = new FileOutputStream(aprop.getLogLocaltion()
						+ "/analysis" + dateFormat.format(new Date()) + ".log",
						true);
				PrintStream ps = new PrintStream(outfile);
				FileOutputStream errfile = new FileOutputStream(
						aprop.getLogLocaltion() + "/error"
								+ dateFormat.format(new Date()) + ".log", true);
				PrintStream psErr = new PrintStream(errfile);
				System.setOut(ps);
				System.setErr(psErr);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		DupNewsCache.removeCache(new Date(new Date().getTime()-24L*3600*1000*2));
		DupNewsCache.addCache(new Date());
		if(appNum!=1){
			AnoDupNewsCache.removeCache(new Date(new Date().getTime()-24L*3600*1000*2));
			AnoDupNewsCache.addCache(new Date());
		}
		
	}
	
	/*public static void main(String[] args) {
		init();
		long startMili = System.currentTimeMillis();// ��ǰʱ���Ӧ�ĺ�����
		System.out.println("��ʼ " + logDateFormat.format(new Date()));

		conceptPublic = WebPageAnalyzer.da.getEventByEventId(-1, "��������");
		ArrayList<KeywordGroup> global_kg = conceptPublic.getConcepts();
		
		System.out.println("ͨ���¼���ʼ���С���������������" + global_kg.size());

		for (KeywordGroup global_k : global_kg) {
			initMap(global_k, global_concept);
		}

		
		eventList = WebPageAnalyzer.da.getEventList();// ��ʼ���¼��б�
		System.out.println("WebPageAnalyzer.da.getEventList()"
				+ WebPageAnalyzer.da);

		events = eventList.getEvents(); // ��ȡ�¼�
		Event event_init = null;
		for (int i = 0; i < events.size() - 1; i++) {
			event_init = (Event) events.get(i);
			ArrayList<Template> Template = event_init.getTemplates();
			for (Template template : Template) {
				for (KeywordGroup k : template.getConcepts()) // ƥ��ģ����������и���
				{
					initMap(k, event_init.getConceptMap());
				}
			}
		}
		// ��ʼ��conceptMap
		try {
			addRuleFromXML("addRules.xml");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			Date readDoc_start = new Date();
			Page page=da.getPageByID("0B33E966F9B46003D5F17EFCC0BC65FD");
			Date readDoc_end = new Date();
			if(aprop.isTestTime())
				page.readDocTime=TimeUtil.decimalFormat.format(((double)(readDoc_end.getTime()-readDoc_start.getTime()))/1000);
			Date doc_start = new Date();
			new WebPageAnalyzer(0, 0, 0).AnalyzePage(page);
			Date doc_end = new Date();
			if(aprop.isTestTime())
				page.analysisDocTime=TimeUtil.decimalFormat.format(((double)(doc_end.getTime()-doc_start.getTime()))/1000);
//				page.readDocTime=TimeUtil.decimalFormat.format(((double)(readDoc_end.getTime()-readDoc_start.getTime()))/1000);
//			System.out.println(TimeUtil.statTimeBySecond(doc_start, doc_end));
			System.out.println(page.getTimeStr());
//				Files.append(TimeUtil.statTimeBySecond(readDoc_start, readDoc_end)+"\r\n", new File(aprop.getLogLocaltion()+"/readDocTime.record"), Charsets.UTF_8);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}*/
	
	public static void main(String[] args) {
//		try {
//			threadRecord = new PrintStream("record_Thread_"+aprop.getThreadsNum()+".log");
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		
		Date date1 = new Date();
	
		singalCatch = new SignalCatch();
//		System.out.println(aprop.getMaxTryNum());
//		System.out.println(singalCatch.analysisUnits.toString());
//		System.out.println(singalCatch.analysisUnits.analysisUnits);
//		try {
//			singalCatch.analysisUnits.removePage("2CB186735FEEF84A7C9E79A3008529EA",logDateFormat.parse("2015-06-24 11:55:00"));
//		} catch (ParseException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		System.exit(0);
		// install signals
		Signal.handle(new Signal("TERM"), singalCatch);
		Signal.handle(new Signal("INT"), singalCatch);
		
//		if (args[1].equals("2"))
//		if(appNum!=1)
//			initProSubCount();
		
		if(!testOld)
			init();
//		DupNewsCache.addCache(new Date());
//		System.exit(0);
		try {
			long startMili = System.currentTimeMillis();// ��ǰʱ���Ӧ�ĺ�����
			System.out.println("��ʼ " + logDateFormat.format(new Date()));

			conceptPublic = WebPageAnalyzer.da.getEventByEventId(-1, "��������");
			ArrayList<KeywordGroup> global_kg = conceptPublic.getConcepts();
			
			System.out.println("ͨ���¼���ʼ���С���������������" + global_kg.size());

			for (KeywordGroup global_k : global_kg) {
				initMap(global_k, global_concept);
			}

			
			eventList = WebPageAnalyzer.da.getEventList();// ��ʼ���¼��б�
			System.out.println("WebPageAnalyzer.da.getEventList()"
					+ WebPageAnalyzer.da);

			events = eventList.getEvents(); // ��ȡ�¼�
			Event event_init = null;
			for (int i = 0; i < events.size() - 1; i++) {
				event_init = (Event) events.get(i);
				ArrayList<Template> Template = event_init.getTemplates();
				for (Template template : Template) {
					for (KeywordGroup k : template.getConcepts()) // ƥ��ģ����������и���
					{
						initMap(k, event_init.getConceptMap());
					}
				}
			}
			// ��ʼ��conceptMap
			addRuleFromXML("ontology_merge.xml", true);
//			addRuleFromXML("ontology_db.xml",true);
//			addRuleFromXML("finalAdd.xml",false);
//			addRuleFromXML("addRules.xml",false);
//			Page page=da.getPageByID("6AA9DA88759352642883F1FEE114A955");// F8C90A03148A0736BA0CE18191A46AD3 A7F5578C33E21A38C121770E593B5B39 B1A805188B9D1D7DBAD30FAA889DCB22 B25127D58E59D852BD29118678389390
//			System.out.println(page.getContent());
			/*System.out.println(page.getDownloadDate());
			DupNewsCache.addCache(new Date());
//			Page page=new Page();
//			page.setId("hahaha");
//			page.setPublishDate(new Date());
//			page.setTitle("a");
//			page.setContent("�����˲Ƶġ����󹤳̡����������̡��� ��������������ʱ�����������г����磬������");
//			System.out.println(page.getContent());
//			DupNewsCache.addCache(new Date());
			try {
				WebPageAnalyzer test=new WebPageAnalyzer();
				System.out.println("[" + test.index + "] " + "Page("
						+ page.getId() + ") analysis start");
				new WebPageAnalyzer().AnalyzePage(page);
				System.out.println("[" + test.index + "] " + "Page("
						+ page.getId() + ") analysis complete");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/
//			System.exit(0);
//			System.setOut(new PrintStream("test.log"));
			if (args[0].equals("all")) {
				da.truncateTable();
				File save = new File("analysis.save");
				if (save.exists()) {
					save.delete();
				}
				singalCatch.analysisUnits = new AnalysisUnits();

			}

			

			Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler());
			ExecutorService executorService = Executors
					.newFixedThreadPool(threadsNum);
			Collection<Callable<Boolean>> list = new LinkedList<Callable<Boolean>>();
			Date date2 = new Date();
				
			
			System.out.println("��ʼ��������ʱ��"+(double)(date2.getTime() - date1.getTime())/1000+"��");
			
			if(!testOld){
				DupNewsCache.addCache(new Date());
//				DupNewsCache.addCache(new Date(new Date().getTime()-24L*3600*1000));
//				DupNewsCache.addCache(new Date(new Date().getTime()-24L*3600*1000*2));
				System.out.println("��ʼ���ظ���Ϣ��ʱ��"+(double)(new Date().getTime() - date2.getTime())/1000+"��");
				if(appNum!=1){
					AnoDupNewsCache.addCache(new Date());
//					AnoDupNewsCache.addCache(new Date(new Date().getTime()-24L*3600*1000));
//					AnoDupNewsCache.addCache(new Date(new Date().getTime()-24L*3600*1000*2));
				}
			}else{
				DupNewsCache.addCache(dateFormat.parse("2014-01-18"));
				if(appNum!=1){
					AnoDupNewsCache.addCache(dateFormat.parse("2014-01-18"));
				}
			}
			
			if(aprop.isOld()==true)
				initProSubCount();
			
			long re_regionCongfig=0;
			
			while (true) {
				// ���û�е�����־���½�������־
				// Redirect output stream
				if (!dateFormat.format(new Date()).equals(todayDate)&&!testOld) {
					dayChange();
				}
				
				long currentMili = System.currentTimeMillis();// ��ǰʱ���Ӧ�ĺ�����
				System.out
						.println("��ѭ����ʼ��:" + logDateFormat.format(new Date()));

				if(aprop.isOld()==true){
					Set<Integer> proIds=proSubCountMap.keySet();
					for(Integer proId:proIds){
						tempProSubMap.put(proId, 0);
					}
				}
				
				
				
				
				Date get_page_startDate = new Date();
				pageList = da.getUnAnalysedPages();
				System.out.println("ȡ"+pageList.size()+"ƪ�ĵ���ʱ:"+(new Date().getTime() - get_page_startDate.getTime())+"����");
				int page_num = pageList.size();

				if (page_num > 0) {

					
//					if(startLong == 0L){
//						startLong = new Date().getTime();
//						recordLong = new Date().getTime();
//					}
					
					System.out.println(new Date() + "  Found " + page_num
							+ " unanalynized pages.");
					System.out.println("Start analysising for " + page_num
							+ " pages ...");
					try {
						int per_num = page_num / threadsNum;
						System.out.println(per_num);
						for (int i = 0; i < threadsNum - 1; i++) {
							list.add(new WebPageAnalyzer(i * per_num, (i + 1)
									* per_num, i + 1));
						}
						list.add(new WebPageAnalyzer(
								(threadsNum - 1) * per_num, page_num,
								threadsNum));

						executorService.invokeAll(list);
						System.out.println("Finished all threads");

					} catch (Exception e) {
						executorService.shutdown();
						e.printStackTrace();
					} finally {
						list.clear();
						pageList.clear();
						pageList = null;
					}
				}

				System.out.println(new Date() + "   analysis finished.");
				if(aprop.isOld()==true)
					WebPageAnalyzer.da.updateSubjectCount();
				long endMili = System.currentTimeMillis();
				System.out.println("���� " + logDateFormat.format(new Date()));
				System.out.println("����ѭ����ʱ��" + (endMili - currentMili) / 1000
						+ "�룬������" + page_num + "ƪ����");
				
				if(aprop.isTestTime())
					try {
						Files.append("Date:"+logDateFormat.format(new Date())+"\n",new File(WebPageAnalyzer.aprop.getLogLocaltion()+"/cacheDis.record"), Charsets.UTF_8);
						Files.append("Date:"+logDateFormat.format(new Date())+"\n",new File(WebPageAnalyzer.aprop.getLogLocaltion()+"/allTime.record"), Charsets.UTF_8);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//				Files.append("����ѭ����ʱ��" + TimeUtil.decimalFormat.format(((double)(endMili - currentMili) / 1000))+ "�룬������" + page_num + "ƪ����\r\n", new File(aprop.getLogLocaltion()+"/analysisTime.record"), Charsets.UTF_8);
				
				AnalysisProperties.storeRecord(singalCatch.analysisUnits);

				if (loopOnce)
					break;
				
				re_regionCongfig+=endMili-currentMili;
				if(re_regionCongfig>1000*3600){
					refreshOntology();
					System.out.println("refreshDate:"+logDateFormat.format(new Date()));
					RegionConfig.RefreshConfig();
					re_regionCongfig=0;
				}
					
				Thread.sleep(refreshTime * 1000);
				
//				System.out.println(logDateFormat.format(new Date(singalCatch.analysisUnits.get(singalCatch.analysisUnits.size()-1).getStart()))+"  "+logDateFormat.format(new Date(singalCatch.analysisUnits.get(singalCatch.analysisUnits.size()-1).getEnd())));
				
				
				System.out
						.println("�ܺ�ʱΪ��" + (endMili - startMili) / 1000 + "��");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (events.size() > 0) {
				events.clear();
			}
			if (eventList != null) {
				eventList = null;
			}
		}

	}

	/**
	 * ExecutorService ��callable���Ҫ��ʵ�ֵĽӿڣ�ÿ���߳��ɴ�����
	 */
	public Boolean call() {
		// TODO Auto-generated method stub
		// Pre-filter downloaded pages, works only for mustContainWord being not
		// null or empty
		Page page = null;
		int maxTryNum = aprop.getMaxTryNum();
		if (mustContainWord != null && mustContainWord.trim().length() > 0) {
			System.out.println("Pre-filtering for '" + mustContainWord
					+ "' ...");
			// Pattern p=Pattern.compile(mustContainWord);

			for (int i = this.start; i < this.end; i++) {
				page = pageList.get(i);
				// DownloadDoc dc=wpa.getDownloadDoc(page);
				String content = null;
				content = au.getCoreContent_new(page);
				if (null == content)
					continue;
				// Matcher m1=p.matcher(content);
				// if(!m1.find()){}
				// da.PageAnalysed(page.getId());
			}
		}
		try {
			String str;
			if (isShow)
				System.out.println("[" + this.index
						+ "] Start analysising for " + this.start + "-"
						+ this.end + " pages ...");
			for (int i = this.start; i < this.end; i++) {
				str = null;
				page = pageList.get(i);
//				if (isShow)
					System.out.println("[" + this.index + "] ��ȡpage("+page.getId()+")������");

				Date readDoc_start = new Date();
				str = au.getCoreContent_new(page);
				Date readDoc_end = new Date();
				
				if(aprop.isTestTime())
					page.readDocTime=TimeUtil.decimalFormat.format(((double)(readDoc_end.getTime()-readDoc_start.getTime()))/1000);
//					Files.append(TimeUtil.statTimeBySecond(readDoc_start, readDoc_end)+"\r\n", new File(aprop.getLogLocaltion()+"/readDocTime.record"), Charsets.UTF_8);
				
//				str = au.getCoreContent(page);
				if (null == str) {
					System.out.println("[" + this.index + "] ��ȡpage("+page.getId()+")������ʧ��");
					synchronized(unAnaPageList){
						// ��ȡ�ļ�ʧ�����¼����+1��ʧ�ܴ����ﵽ�������������������
						if (unAnaPageList.containsKey(page.getId())) {
							if (unAnaPageList.get(page.getId()) == maxTryNum - 1) {
								singalCatch.analysisUnits.removePage(page.getId(),page.getDownloadDate());
								unAnaPageList.remove(page.getId());
								System.err.println("["+ this.index+ "] id"+ page.getId()+ " Page "+ page.getFn().substring(0,page.getFn().length() - 4) + "txt"+ " �õ������ļ����� ");
								// System.out.println("["+this.index+"] id"+page.getId()+
								// " ���Ƴ� ");
							} else {
								unAnaPageList.replace(page.getId(),
										unAnaPageList.get(page.getId()) + 1);
							}
						} else
							unAnaPageList.put(page.getId(), 1);
						continue;
					}
				}
				page.setContent(str);
//				if (isShow)
					System.out.println("[" + this.index + "] " + "Page("
							+ page.getId() + ") ...... ");
					
				Date analysisDoc_start = new Date();	
				try{
					System.out.println("[" + this.index + "] " + "Page("
							+ page.getId() + ") analysis start");
					this.AnalyzePage(page);
					System.out.println("[" + this.index + "] " + "Page("
							+ page.getId() + ") analysis complete");
				}catch (Exception e) {
					System.err.println(page.getId()+" analysisError");
					throw e;
				}
				Date analysisDoc_end = new Date();
				if(aprop.isTestTime()){
					page.analysisDocTime=TimeUtil.decimalFormat.format(((double)(analysisDoc_end.getTime()-analysisDoc_start.getTime()))/1000);
//					Files.append(TimeUtil.statTimeBySecond(analysisDoc_start, analysisDoc_end)+"\r\n", new File(aprop.getLogLocaltion()+"/analysisDocTime.record"), Charsets.UTF_8);
					Files.append(page.getTimeStr()+"\r\n", new File(aprop.getLogLocaltion()+"/allTime.record"), Charsets.UTF_8);
				}
				// da.setPageAnalysed(page.getId());
//				addCount();
				// �����ɹ� ��δ�����б����Ƴ�
				singalCatch.analysisUnits.removePage(page.getId(),page.getDownloadDate());

				// �����ɹ� ��δ�ɹ���ȡ�б����Ƴ����ǲ��Ƿ���ʹ��unAnaPageList�ˣ������Ļ�PageUnit��unAnalysis��ֻ���յ��ж��ź�ʱ��δ������page����������ȡ���ݲ��ɹ���page
				synchronized(unAnaPageList){
					if (unAnaPageList.containsKey(page.getId()))
//						unAnaPageList.remove(unAnaPageList);
						unAnaPageList.remove(page.getId());
				}
				if (isShow)
					System.out.println("[" + this.index + "]  Page "
							+ page.getId() + " analysed!");
				System.out.println("[" + this.index + "]  �Ѿ�������"
						+ (i - this.start + 1) + "ƪ���¡�����Ŭ����");

				/**** ------------------------------------------- ******/
			}
			System.out.println("[" + this.index + "] End analysis,return!!!!");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return true;

	}
	public static void refreshOntology() throws Exception{
		events.clear();
		eventList = WebPageAnalyzer.da.getEventList();// ��ʼ���¼��б�
		events = eventList.getEvents(); // ��ȡ�¼�
		conceptPublic=new Event();
		conceptPublic.setEventID(-1);
		conceptPublic.setEventName("��������");
		global_concept.clear();
//		Event event_init = null;
//		for (int i = 0; i < events.size() - 1; i++) {
//			event_init = (Event) events.get(i);
//			ArrayList<Template> Template = event_init.getTemplates();
//			for (Template template : Template) {
//				for (KeywordGroup k : template.getConcepts()) // ƥ��ģ����������и���
//				{
//					initMap(k, event_init.getConceptMap());
//				}
//			}
//		}
//		addRuleFromXML("ontology_db.xml", true);
//		addRuleFromXML("finalAdd.xml", false);
		addRuleFromXML("ontology_merge.xml", true);
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
					if((concept=WebPageAnalyzer.global_concept.get(conceptName))!=null){//event�����и�concept
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
							if((concept=event.getConceptByName(conceptName))!=null){//event�����и�concept
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
								}
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
										addParentKg.getChildKeywordGroup().add(event.getConceptMap().get(childKgName)==null?WebPageAnalyzer.global_concept.get(childKgName):event.getConceptMap().get(childKgName));
									}
								}
								
							}
						}
					}
					
					List<Element> templateElements=eventAddEle.elements("template");
					if(templateElements!=null&&templateElements.size()>0){
						for(Element templateElement:templateElements){
							Template template=new Template();
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
	public static void addRuleFromXML(String addFileName) throws Exception{
		List<Event> events=WebPageAnalyzer.eventList.getEvents();
		SAXReader saxReader=new SAXReader();
		Document document=saxReader.read(new FileInputStream(addFileName));
		Element rootEle=document.getRootElement();
		List<Element> eventAddEles=rootEle.elements("eventAdd");
		for(Element eventAddEle:eventAddEles){
			String eventName=eventAddEle.attribute("name").getValue();
			String eventID=eventAddEle.attribute("id").getValue();
			if(eventID.equals("-1")){
				Event event=WebPageAnalyzer.conceptPublic;
				List<Element> conceptEles=eventAddEle.elements("concept");
				if(conceptEles!=null&&conceptEles.size()>0){
					ArrayList<Element> addParentKgEles=new ArrayList<Element>();
					ArrayList<Integer> addParentKgHadFlags=new ArrayList<Integer>();
					for(Element conceptEle:conceptEles){
						String conceptName=conceptEle.attributeValue("name").trim();
						KeywordGroup concept;
						int hadConcept=0;
						if((concept=WebPageAnalyzer.global_concept.get(conceptName))!=null){
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
							}
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
				continue;
			}
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
							if((concept=event.getConceptByName(conceptName))!=null){
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
								}	
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
										addParentKg.getChildKeywordGroup().add(event.getConceptMap().get(childKgName)==null?WebPageAnalyzer.global_concept.get(childKgName):event.getConceptMap().get(childKgName));
									}
								}
								
							}
						}
					}
					
					List<Element> templateElements=eventAddEle.elements("template");
					if(templateElements!=null&&templateElements.size()>0){
						for(Element templateElement:templateElements){
							Template template=new Template();
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
}

/**
 * ÿ���̵߳�δ�����쳣���
 * @author Bys
 *
 */
class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		// TODO Auto-generated method stub
		System.out.println(t.getId() + " cought" + e);
	}
}