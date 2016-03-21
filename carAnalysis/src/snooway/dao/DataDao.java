package snooway.dao;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.io.Files;



import dataAnalysis.WebPageAnalyzer;
import javabean.DupNewsAll;
import javabean.EventList;
import javabean.Keyword;
import javabean.PagesUnit;
import javabean.RegionList;
import javabean.db.Event;
import javabean.db.EventNews;
import javabean.db.KeywordGroup;
import javabean.db.Page;
import javabean.db.Region;
import javabean.db.Template;


import snooway.dao.PoolConection;
import util.AnalysisProperties;
import util.AnalysisUtil;
import util.JiangxiUtil;
import util.TimeUtil;


/**
 * ���ݿ�Ĳ�����������ʵ��
 * @author syq
 */
public class DataDao {
	static AnalysisProperties aprop = AnalysisProperties.getInstance(); 
//	/**
//	 * 2014-07-04 00:00:00
//	 */
//	static final long START_LONG = 1404403200000L;
//	
	/**
	 * 2014-01-18 00:00:00
	 */
	static final long START_LONG = 1388505600000L;
	
	private static Pattern PT_fromTable = Pattern
			.compile(
					"\\s+from\\s+(\\w+(\\s+(?!WHERE|GROUP|HAVING|ORDER|LIMIT|PROCEDURE|INTO|FOR)\\w+)?(,\\s*\\w+(\\s+(?!WHERE|GROUP|HAVING|ORDER|LIMIT|PROCEDURE|INTO|FOR)\\w+)?)*)",
					Pattern.CASE_INSENSITIVE);
	private static Pattern PT_insertTable = Pattern.compile(
			"insert\\s+into\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
	private static Pattern PT_updateTable = Pattern.compile("update\\s+(\\w+)",
			Pattern.CASE_INSENSITIVE);
	private static Pattern PT_truncateTable = Pattern.compile(
			"truncate\\s+table\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
	

	private static PoolConnectCfg poolconcfg_ontology = new PoolConnectCfg(aprop.getDirPath()+"DataAccess_ontology_db.properties"); 
	private static PoolConnectCfg poolconcfg = new PoolConnectCfg(aprop.getDirPath()+"DataAccess_app_server.properties"); 
	private static PoolConnectCfg poolconcfg_server = new PoolConnectCfg(aprop.getDirPath()+"DataAccess_doc_server.properties");
	private static PoolConnectCfg poolconcfg_app2 = new PoolConnectCfg(aprop.getDirPath()+"DataAccess_app2_server.properties"); 
	/**
	 * ���� dao��ͳһ�ط����ɼ���oracle
	 * 
	 * @return
	 */
	public static DataDao createDao() {
		AnalysisProperties instance = AnalysisProperties.getInstance();
		if (instance.getDatabaseType().equalsIgnoreCase("oracle"))
			return new OracleDataDao();
		else
			return new DataDao();
	}
	
	/**
	 * ���� dao��ͳһ�ط�
	 * 
	 * @return
	 */
	public static DataDao createDao(String className) {
		DataDao da = null;
		try {
			da = (DataDao) Class.forName(className).newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return da;

	}


	/**
	 * ��ֹ���ⲿ�������������ʹ�� DataDao.createDao
	 */
	protected DataDao() {

	}
	
	
	
	/**
	 * ����������Χ��δ�������£����ظ÷�Χ���ҵ���������
	 * @author Bys
	 * @param pages �鵽��page
	 * @param sql ��ѯ���
	 * @param needInsert ����Ƿ�Χ��ѯ���棩���ǵ����ļ���ѯ����
	 * @param units ���Ϊ��Χ��ѯ�򽫸÷�Χ������б�
	 * @return ��������������������
	 */
	private int setUnAnalysedUnit(List<Page> pages,String sql,boolean needInsert,PagesUnit unit){
//		if(needInsert&&unit.getEnd()<START_LONG){
//			return 0;
//		}
		List<Page> list = new ArrayList<Page>();
		PoolConection pl = null;
		int resultNum = 0;
//		PagesUnit pu	= new PagesUnit(-1,-1);
		try {
//			System.out.println("enter setUnAnalysedUnit");
			pl = new PoolConection(poolconcfg_server.getDrv(),poolconcfg_server.getUrl(),poolconcfg_server.getUsername(),poolconcfg_server.getPassword(),0);
//			System.out.println("connection has been got");
			ResultSet rs = pl.executeQuery(sql);
			while (rs.next()) {
//				resultNum ++;
				
				Page page = new Page();
				page.setId(rs.getString("id"));
				page.setType(rs.getInt("type"));
				//�����΢��������ֱ�Ӷ�summary
				if(page.getType()==3){
					String summary = rs.getString("summary");
					if(summary==null){
						System.err.println("pageid:"+rs.getString("id"));
						System.err.println(summary);continue;
					}
					int byteLength = summary.getBytes("UTF-8").length;
					if(byteLength>200){							
						while(byteLength>180){
							summary = summary.substring(0,summary.length()-1);
							byteLength = summary.getBytes("UTF-8").length;
						}
						summary += "����";
					}
					page.setTitle(summary);
				}
				else {
					page.setTitle(rs.getString("title"));
				}
//				page.setFn(rs.getString("fn"));
				page.setWebSite(rs.getString("webSite"));
				page.setUrl(rs.getString("url"));
				page.setDownloadDate(rs.getTimestamp("downloadDate"));
				page.setWebSiteplate(rs.getString("webSiteplate"));
				page.setPublishDate(rs.getTimestamp(("publishDate")));
				page.setSummary(rs.getString("summary"));
				page.setNewsLevel(rs.getInt("newsLevel"));
				page.setSitePriority(rs.getInt("sitePriority"));
				list.add(page);
				resultNum ++;
				if(needInsert){
					unit.addUnanalysis(page.getId());
//					System.err.println("id"+page.getId()+"����ӣ�");
				}
			}
//			if(needInsert&&list.size()>0){
//				pu.setStart(list.get(list.size()-1).getId());
//				pu.setEnd(list.get(0).getId());
//				units.add(pu);
//			}
			if(list.size()>0)
				pages.addAll(list);
			rs.close();	
			pl.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resultNum;
	}
	
	
	public List<Page> getUnAnalysedPages(){
		List<PagesUnit> units = new ArrayList<PagesUnit>();
		List<Page> list = new ArrayList<Page>();
		List<Page> temp = new ArrayList<Page>();
		for(int i = WebPageAnalyzer.singalCatch.analysisUnits.size()-1;i>=0;i--){
			if(WebPageAnalyzer.singalCatch.analysisUnits.get(i).getUnanalysis().size()>0){
				int count=0;//ͳ�ƿ���ƴ��sql��unAnalysedPages����Ŀ����ֹ���������²�ѯ����
				String innerSql = "SELECT id,title,webSite,webSiteplate,url,downloadDate,summary,type,publishDate,sitePriority,newsLevel FROM wdyq_pages where id = ";
				for(String id:WebPageAnalyzer.singalCatch.analysisUnits.get(i).getUnanalysis()){
					innerSql+="'"+id+"' or id = ";
					count++;
					if (count>100)
						break;
				}
				innerSql = innerSql.substring(0,innerSql.length()-9);
				setUnAnalysedUnit(temp,innerSql,false,new PagesUnit(-1, -1));
				if(temp.size()>0)
					break;
			}
		}
		
		try {
			while(list.size()==0){
				list.addAll(getUnAnalysedPages(units));
				for(PagesUnit unit:units){					
//					if(unit.getEnd()<START_LONG){
//						list.addAll(temp);
//						return list;
//					}
					System.out.println("������Χ��"+AnalysisUtil.getTimeString(unit.getStart()).substring(5)+"����"+AnalysisUtil.getTimeString(unit.getEnd()).substring(5)+":"+unit.getUnanalysis().size()+"ƪ");
					WebPageAnalyzer.singalCatch.analysisUnits.add(unit);
					System.out.println("������Χ���");
				}
				units.clear();
			}
			list.addAll(temp);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return list;
	}
	

	
	
	/**
	 * @author Bys
	 * @return ��������δ������ҳ��
	 * @throws ParseException 
	 */
	public List<Page> getUnAnalysedPages(List<PagesUnit> units) throws ParseException {//ÿ�δ����ݿ�ȡ1-2��timeGap�ڵ�page����
		final long MIN_MILL = 60*1000;
		String nowString = AnalysisUtil.downloadSdf.format(new Date());
		//ȡһ��ǰ�������Է���©����ȡ��ǰ���ܵ�ǰʱ��ļ�¼δ�������
		long nowLong = AnalysisUtil.downloadSdf.parse(nowString).getTime()-MIN_MILL;
		
		List<Page> list = new ArrayList<Page>();
//		List<PagesUnit> units = new ArrayList<PagesUnit>();
		long timeGap = MIN_MILL*aprop.getTimeGap();
		
			String sql;
			String tableName = "wdyq_pages";
			//������з�ΧΪ�գ����ʼ����ֱ�ӻ�ȡ2��ʱ����������
			if(WebPageAnalyzer.singalCatch.analysisUnits.size() == 0){
				sql = "SELECT id,title,webSite,webSiteplate,url,downloadDate,summary,type,publishDate,sitePriority,newsLevel FROM "+ tableName +" where downloadDate >'"+AnalysisUtil.getTimeString(nowLong-timeGap*2)+"' and downloadDate<='"+AnalysisUtil.getTimeString(nowLong)+"' and (indexedStatus=1 or indexedStatus=3 or indexedStatus=20 or (indexedStatus=0 and type in(3,6)))" ;
				PagesUnit unit	= new PagesUnit(nowLong-timeGap*2,nowLong);
				setUnAnalysedUnit(list,sql,true,unit);
				units.add(unit);
			}else{
				//ȡ���һ�η�����Χ�Ľ�����Ϊ���εĿ�ʼ
				long start = WebPageAnalyzer.singalCatch.analysisUnits.get(WebPageAnalyzer.singalCatch.analysisUnits.size()-1).getEnd();

				//����ϴ��������������ڳ���2��ʱ������ֱ��ȡ2��ʱ��������������ΪĿǰ�µ�page�϶࣬Ϊ�˱�֤�������ݵĴ������ȡ
				if(nowLong - start>=timeGap*2){
					sql = "SELECT id,title,webSite,webSiteplate,url,downloadDate,summary,type,publishDate,sitePriority,newsLevel FROM "+ tableName +" where downloadDate >'"+AnalysisUtil.getTimeString(nowLong-timeGap*2)+"' and downloadDate<='"+AnalysisUtil.getTimeString(nowLong)+"' and (indexedStatus=1 or indexedStatus=3 or indexedStatus=20 or (indexedStatus=0 and type in(3,6)))" ;
					PagesUnit unit	= new PagesUnit(nowLong-timeGap*2,nowLong);
					setUnAnalysedUnit(list,sql,true,unit);
					units.add(unit);
				}
				//�������2��ʱ������Ҳ����һ�ζ�ȡ��page���ϵĴ���ʱ����٣�����Ŀǰ�µ�page���٣������ڳ�һ��timeGap������ǰ��page���ݿ���©ȡ��page����ΪΪ�˱�֤ʱЧ������ȡ���nowǰ2��timeGap�ڵ�page��������ϴν���һֱȡ�����ڣ����������������ǰ
				else{ 
					if(nowLong - start >=MIN_MILL){
						sql = "SELECT id,title,webSite,webSiteplate,url,downloadDate,summary,type,publishDate,sitePriority,newsLevel FROM "+ tableName +" where downloadDate >'"+AnalysisUtil.getTimeString(start)+"' and downloadDate<='"+AnalysisUtil.getTimeString(nowLong)+"' and (indexedStatus=1 or indexedStatus=3 or indexedStatus=20 or (indexedStatus=0 and type in(3,6)))" ;
						PagesUnit unit	= new PagesUnit(start,nowLong);
						setUnAnalysedUnit(list,sql,true,unit);
						units.add(unit);
						//���ȡ�ó���һ��ʱ������ֱ�ӷ���
						if(nowLong - start > timeGap){
							return list;
						}
					}//������С��һ����Сʱ�䵥λMIN_MILL���������С��MIN_MILL���������ʱ��Σ����´����һ������ǰȡһ��timeGapʵ�ֲ�©�������ߴ���MIN_MILL��С��һ��timeGap��������Ҵ�ʱ�Ѿ�add��һ��unit��֮����Ҫ��2��timeGapӦ���Ǿ���ʹÿ��ȡ�õ�page��Ŀ��ͬ��
				//���ȡ�õ���һ��ʱ����������ǰȡһ��ʱ����
					long end = WebPageAnalyzer.singalCatch.analysisUnits.get(WebPageAnalyzer.singalCatch.analysisUnits.size()-1).getStart();
					//�������������һ��������¼��������һ��������¼������һ���ľ��볬��1��ʱ�����������һ���Ľ�����ǰȡһ��ʱ����
					if(WebPageAnalyzer.singalCatch.analysisUnits.size() == 1||
							end - WebPageAnalyzer.singalCatch.analysisUnits.get(WebPageAnalyzer.singalCatch.analysisUnits.size()-2).getEnd()>timeGap){
						sql = "SELECT id,title,webSite,webSiteplate,url,downloadDate,summary,type,publishDate,sitePriority,newsLevel FROM "+ tableName +" where downloadDate <='"+AnalysisUtil.getTimeString(end)+"' and downloadDate>'"+AnalysisUtil.getTimeString(end-timeGap)+"' and (indexedStatus=1 or indexedStatus=3 or indexedStatus=20 or (indexedStatus=0 and type in(3,6)))" ;
						if(WebPageAnalyzer.singalCatch.analysisUnits.size() == 1&&end<START_LONG){
							return list;
						}
						PagesUnit unit_add	= new PagesUnit(end-timeGap,end);
						setUnAnalysedUnit(list,sql,true,unit_add);
						units.add(unit_add);
					}
					//���������һ����¼�Ľ���ȡ����һ����¼�Ŀ�ʼ����С��һ��timeGap
					else {
						long second_start = WebPageAnalyzer.singalCatch.analysisUnits.get(WebPageAnalyzer.singalCatch.analysisUnits.size()-2).getEnd();
						sql = "SELECT id,title,webSite,webSiteplate,url,downloadDate,summary,type,publishDate,sitePriority,newsLevel FROM "+ tableName +" where downloadDate <='"+AnalysisUtil.getTimeString(end)+"' and downloadDate>'"+AnalysisUtil.getTimeString(second_start)+"' and (indexedStatus=1 or indexedStatus=3 or indexedStatus=20 or (indexedStatus=0 and type in(3,6)))" ;
						PagesUnit unit_add	= new PagesUnit(second_start,end);
						setUnAnalysedUnit(list,sql,true,unit_add);
						units.add(unit_add);
					}
						
				}
			}
		return list;
	}
	
	/**
	 * 
	 * ���eventnews, eventnews_person, eventnews_region, eventnews_dup 4����
	 * 
	 */
	public void truncateTable() {
		PoolConection pl = null;
		try {
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			String sql = "SHOW TABLES LIKE \"%eventnews%\"";
			ResultSet rs = pl.executeQuery(sql);
			int i=0;
			rs.last();
			int rows = rs.getRow();
			String tables[]=new String[rows];
			rs.beforeFirst();
			while(rs.next())
			{
				tables[i] = "truncate table "+rs.getString(1);i++;
			}			
			rs.close();
			for(int j=0;j<tables.length;j++)
			{
				sql=tables[j];pl.execute(sql);
			}
			// ��ҳ�Ƿ�����Ϊ0

//			sql = "truncate table wdyq_pageaflag";
//			sql = addPrefixToTableName(sql);
//			pl.execute(sql);
		} catch (Exception e) {
			System.err.println(e);
		} finally {
			if (pl != null)
				pl.close();
		}
	}

	
	public String getDocPath(int pageId)
	{
		PoolConection pl = null;
		String path=null;
		try {
			pl = new PoolConection(poolconcfg_server.getDrv(),poolconcfg_server.getUrl(),poolconcfg_server.getUsername(),poolconcfg_server.getPassword(),0);
			String sql = "select rootPath from doc_map where pageId_start <" +pageId +" and "+pageId+" <pageId_end";
			ResultSet rs = pl.executeQuery(sql);
			while(rs.next())
			{
				path = rs.getString("rootPath");
			}
			rs.close();
		} catch (Exception e) {
			System.err.println(e);
		} finally {
			if (pl != null)
				pl.close();
		}
		return path;
	}
	
	/**
	 * ���ڱ���Ĳ���
	 */
	
	/**
	 * ����eventID������Event����
	 * @author syq
	 * @param eventId
	 * @param eventName
	 * @return
	 */
	public Event getEventByEventId(int eventId, String eventName) {
		PoolConection pl = null;

		try {
			pl = new PoolConection(poolconcfg_ontology.getDrv(),poolconcfg_ontology.getUrl(),poolconcfg_ontology.getUsername(),poolconcfg_ontology.getPassword(),2);	
			String sql = "SELECT keywordGroupID, keywordGroupName,sequence,parentKeywordGroupId,preForbidden,postForbidden"
				+ " FROM keywordgroup_en WHERE eventID = '"
				+ eventId
				+ "'";
			sql = addPrefixToTableName(sql);
			ResultSet rs = pl.executeQuery(sql); // �����йظ�event��ȫ��concept
			ArrayList<KeywordGroup> groups = new ArrayList<KeywordGroup>();		
			
			while(rs.next())
			{
				int groupId = rs.getInt("keywordGroupID");
				String name = rs.getString("keywordGroupName");
				int seq = rs.getInt("sequence");
				int childflag=rs.getInt("parentKeywordGroupId");
				String preForbidden=rs.getString("preForbidden");
				String postForbidden=rs.getString("postForbidden");
				KeywordGroup kg=getKeywordGroupByID(groupId, name, seq,preForbidden,postForbidden,childflag);//�Ҹø�����������keyword
				if(childflag==1)	//������Ӹ����ȡ���Ӹ���
				{

//					System.out.println("�Ӹ�����Ӹ����ȡ��ʼ");
					List<KeywordGroup> childKeywordGroup = new ArrayList<KeywordGroup>();  //�����Ӹ����id
					childKeywordGroup = getChildKeyWordGroup(groupId,groups);	
//					System.out.println("�Ӹ�����Ӹ����ȡ��"+childKeywordGroup);
					kg.setChildKeywordGroup(childKeywordGroup);
				}
				
				groups.add(kg);
			}
			rs.close();
			
			//��ȡ�¼��е�ģ��
			sql = "SELECT templateID, templateName, polarity, polaritygroup,templateRule FROM template_en WHERE eventID="+ eventId;
			sql = addPrefixToTableName(sql);
			ArrayList<Template> templates = new ArrayList<Template>();
			rs = pl.executeQuery(sql);
			while (rs.next()) {				
				int templateId = rs.getInt("templateID");
				String templateName = rs.getString("templateName");
				int polarity = rs.getInt("polarity");
				int polarityGroup = rs.getInt("polarityGroup");							
				String templateRule=rs.getString("templateRule").replaceAll(" ", "");
				Template t = new Template(templateId, templateName,templateRule);
				t.setTemplatePolarity(polarity);
				t.setPolarityWordPosition(polarityGroup);
				t.setownerEventId(eventId);
				
				ArrayList<KeywordGroup> concept_t=new ArrayList<KeywordGroup>();
				Pattern templateP = Pattern.compile("(\\{.*?\\}|\\(|\\)|\\[|\\]|\\+)");
				String[] templaterule = templateP.split(templateRule);
				for(int i=0;i<templaterule.length;i++)
				{
					if(!templaterule[i].isEmpty())
					{
						boolean flag=true;
						Pattern p=Pattern.compile("([a-zA-Z]*?)\\.([a-zA-Z]*)");    //���ģ���к������ݿ���Ϣ����ȡ���ݿ�ĸ���
						Matcher m=p.matcher(templaterule[i].trim());
						while(m.find())
						{		
							String basename=m.group(1).toString().trim()+"."+m.group(2).toString().trim();
							System.out.println(basename);
							for(KeywordGroup k:groups)
							{
								if(k.getKeywordGroupName().equals(basename))
								{
									flag=false;concept_t.add(k);
									break;
								}
							}
							if(flag)
							{
								KeywordGroup database_kg =getKeywordByDataBase(m.group(1),m.group(2));
								groups.add(database_kg);
								concept_t.add(database_kg);
							}
						}
						for(KeywordGroup k:groups)//Ϊʲôֻ���¼���˽�и�����ȡ����Ϊ������ģ��ƥ��ʱ���ᵥ������������
						{
							if(k.getKeywordGroupName().equals(templaterule[i].trim()))
							{
								concept_t.add(k);flag=true;
								break;
							}
							
						}
					}
				}
			
				t.setConcepts(concept_t);
				templates.add(t);
			}
			rs.close();
			return new Event(eventId, eventName,groups, templates);
		} catch (Exception e) {
			System.err.println(e);
			return null;
		} finally {
			if (pl != null)
				pl.close();
		}
	}
	/**
	 * ͨ��id������ظ�����Ĺؼ���
	 *
	 * @param groupId
	 * @return
	 */
	public KeywordGroup getKeywordGroupByID(int groupId, String name, int seq,String preForbidden,String postForbidden,int childflag) {
		PoolConection pl = null;

		try {
			Pattern p=Pattern.compile("(.*?)\\.(.*?)");    //����Ӹ����к������ݿ���Ϣ����ȡ���ݿ�ĸ���
			Matcher m=p.matcher(name);
			if(m.matches())
			{
				KeywordGroup database_kg =getKeywordByDataBase(m.group(1),m.group(2));
				database_kg.setKeywordGroupID(groupId);
				return database_kg;
			}
			else {
				pl = new PoolConection(poolconcfg_ontology.getDrv(),poolconcfg_ontology.getUrl(),poolconcfg_ontology.getUsername(),poolconcfg_ontology.getPassword(),2);

				String sql = "SELECT keywordId,keywordName FROM keyword_en WHERE keywordGroupID ="
						+ groupId;
				sql = addPrefixToTableName(sql);
				ArrayList<Keyword> keywords = new ArrayList<Keyword>();
				
				ResultSet rs = pl.executeQuery(sql);
				while (rs.next()) {
					String keywordName = rs.getString("keywordName").trim();
					if (postForbidden != null) // ��ȡconcept�е�keywords������������ǰ���ƺͺ����ƣ������������
					{
						if (keywordName.endsWith(")")) {
							keywordName = keywordName.substring(0,
									keywordName.length() - 1)
									+ "|" + postForbidden + ")";
						} else {
							keywordName += "(?!" + postForbidden + ")";
						}
					}
					if (preForbidden != null) {
						if (keywordName.startsWith("(")) {
							StringBuffer s = new StringBuffer();
							s.append(keywordName.substring(0,
									keywordName.indexOf(")"))
									+ "|");
							s.append(preForbidden + ")");
							s.append(keywordName.substring(
									keywordName.indexOf(")") + 1,
									keywordName.length()));
							keywordName = s.toString();
						} else {
							String preExp = "(?<!" + preForbidden + ")";
							preExp += keywordName;
							keywordName = preExp;
						}
					}
					Keyword keyword = new Keyword(rs.getInt("keywordId"),
							keywordName, "");

					keywords.add(keyword);
				}
				rs.close();

				return new KeywordGroup(groupId, name, seq, keywords, childflag);
			}
			
		} catch (Exception e) {
			System.err.println(e);
			return null;
		} finally {
			if (pl != null)
				pl.close();
		}
	}
	/**
	 * @author syq
	 * @param groupId ������Id
	 */
    public	List<KeywordGroup> getChildKeyWordGroup(int groupId,ArrayList<KeywordGroup> groups)
    {
    	PoolConection pl = null;
    	
		try {
			pl = new PoolConection(poolconcfg_ontology.getDrv(),poolconcfg_ontology.getUrl(),poolconcfg_ontology.getUsername(),poolconcfg_ontology.getPassword(),2);
			List<KeywordGroup> childKeywordGroup=new ArrayList<KeywordGroup>();
	    	String sql="SELECT keywordGroupID FROM keywordgroup_en  WHERE keywordGroupID IN (SELECT childKeywordGroupId FROM wdyq_keywordgroup_relation WHERE parentKeywordGroupId="+groupId+")";
			sql = addPrefixToTableName(sql);
			ResultSet rs;
			rs = pl.executeQuery(sql);
			while(rs.next())
			{
				int childgroupId = rs.getInt("keywordGroupID");				
				boolean flag = false;
				for(KeywordGroup k:groups)
				{
					if(k.getKeywordGroupID()==childgroupId)
					{
						childKeywordGroup.add(k);flag=true;break;
					}
				}
				if(!flag)
				{
					Event publicgroups = WebPageAnalyzer.conceptPublic;
					for(KeywordGroup k:publicgroups.getConcepts())
					{	
						if(k.getKeywordGroupID()==childgroupId)
						{
							childKeywordGroup.add(k);flag=true;break;
						}
					}
//					if(!flag){
//					System.err.println(publicgroups.getConcepts().size()+"errConcept:"+groupId+"haha"+childgroupId+getKeywordGroupByID(groupId, "", 1, "", "",0).getKeywordGroupName());System.exit(0);
//					}
				}
			}
			rs.close();
			return childKeywordGroup;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally {
			if (pl != null)
				pl.close();
		}		
		
		
    }
	/**
	 * 
	 * @param tableName
	 * @param fieldName
	 * @return
	 */
	public KeywordGroup getKeywordByDataBase(String tableName,String fieldName)
	{
		PoolConection pl = null;
		
		try {
			pl = new PoolConection(poolconcfg_ontology.getDrv(),poolconcfg_ontology.getUrl(),poolconcfg_ontology.getUsername(),poolconcfg_ontology.getPassword(),2);
			String sql="select * from "+tableName;
			sql = addPrefixToTableName(sql,"target_");
		//	ArrayList<String> keywords = new ArrayList<String>();
			ArrayList<Keyword> keywords = new ArrayList<Keyword>();
//			ArrayList<String> tag = new ArrayList<String>();
			
			ResultSet rs = pl.executeQuery(sql);
			while (rs.next()) {		
				int id = rs.getInt("id");
				String field = rs.getString(fieldName.trim());
				String aliase = rs.getString("alias");
				String tags = rs.getString("tag");
				String keywordName = field.trim();
				//����б�������ϱ���
				if(aliase!=null&&aliase.trim().length()>0){
				 keywordName = keywordName+"|"+aliase.trim();
				}
				Keyword keyword  = new Keyword(id,keywordName,tableName);
				keyword.setTags(tags);
				keywords.add(keyword);
			}
			rs.close();
			KeywordGroup groupsDataBase	=new  KeywordGroup();
			groupsDataBase.setKeywordGroupName(tableName+"."+fieldName.trim());
			groupsDataBase.setKeywords(keywords);
			groupsDataBase.setType(1);                 //��ʾ�����ݿ����
		//	groupsDataBase.setTags(tag);
			return groupsDataBase;
		} catch (Exception e) {
			System.err.println(e);
			return null;
		} finally {
			if (pl != null)
				pl.close();
		}		
	}
	
	
	/**
	   * ��������¼������б�
	   * @author syq
	   * @return 
	   */
		public EventList getEventList() {

			PoolConection pl = null;
			ArrayList<Event> events = new ArrayList<Event>();

			try {
				pl = new PoolConection(poolconcfg_ontology.getDrv(),poolconcfg_ontology.getUrl(),poolconcfg_ontology.getUsername(),poolconcfg_ontology.getPassword(),2);
				String sql = "SELECT eventID, eventName, parentEventId, status FROM event_en";
				sql = addPrefixToTableName(sql);
				ResultSet rs = pl.executeQuery(sql);
				while (rs.next()) {
					int eId = rs.getInt("eventID");
					String eName = rs.getString("eventName");
					Event event = getEventByEventId(eId, eName);
					event.setStatus(rs.getInt("status"));
					event.setParentEventId(rs.getInt("parentEventId"));
					events.add(event);
				}
				rs.close();
				
				
				return new EventList(events);
			} catch (Exception e) {
				System.err.println(e);
//				e.printStackTrace();
				return null;
			} finally {
				if (pl != null)
					pl.close();
			}

		}
	
	
	
//	/**
//	 * 
//	 * @return �õ�����������޸�ʱ��
//	 */
//	public Date getUpdateDateOfEvent(){
//		PoolConection pl = null;
//		Date date = null;
//		try {
//			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
//			String sql = "select eventlastmodtime from update ";
//			sql = addPrefixToTableName(sql);
//			pl.prepareSql(sql);
//			ResultSet rs = pl.executeQuery();
//			
//			if (rs.next()) {
//				date = rs.getDate("eventlastmodtime");
//			}
//			
//		rs.close();
//
//		} catch (Exception e) {
//			System.err.println(e);
//		} finally {
//			if (pl != null)
//				pl.close();
//		}
//		return date;
//	}
	
	/**
	 * �����ݿ��������ǰ׺
	 *
	 */
	protected static String addPrefixToTableName(String sql) {
		String prefix = AnalysisProperties.getInstance().getTablePrefix();
		if (prefix == null || prefix.length() == 0)
			return sql;
		Matcher m = PT_fromTable.matcher(sql);
		if (m.find()) {
			StringBuilder sb = new StringBuilder("");
			sb.append(sql.substring(0, m.start(1)));
			String tables = m.group(1);
			String[] tablelist = tables.split(",");
			for (int i = 0; i < tablelist.length; i++) {
				StringTokenizer st = new StringTokenizer(tablelist[i],
						" \t\f\r\n");
				if (st.hasMoreTokens()) {
					if (i > 0)
						sb.append(", ");
					sb.append(prefix);
					sb.append(st.nextToken());
					if (st.hasMoreTokens())
						sb.append(" " + st.nextToken());
				}
			}
			sb.append(sql.substring(m.end(1)));
			return sb.toString();
		} else {
			boolean bFind = false;
			m = PT_insertTable.matcher(sql);
			if (m.find())
				bFind = true;
			else {
				m = PT_updateTable.matcher(sql);
				if (m.find())
					bFind = true;
				else {
					m = PT_truncateTable.matcher(sql);
					bFind = m.find();
				}
			}
			if (bFind) {
				StringBuilder sb = new StringBuilder("");
				sb.append(sql.substring(0, m.start(1)));
				sb.append(prefix);
				sb.append(m.group(1));
				sb.append(sql.substring(m.end(1)));
				return sb.toString();
			} else
				return sql;
		}
	}
	
	/**
	 * �����ݿ��������ǰ׺
	 *
	 */
	protected static String addPrefixToTableName(String sql,String prefix) {
	//	String prefix = AnalysisProperties.getInstance().getTablePrefix();
		if (prefix == null || prefix.length() == 0)
			return sql;
		Matcher m = PT_fromTable.matcher(sql);
		if (m.find()) {
			StringBuilder sb = new StringBuilder("");
			sb.append(sql.substring(0, m.start(1)));
			String tables = m.group(1);
			String[] tablelist = tables.split(",");
			for (int i = 0; i < tablelist.length; i++) {
				StringTokenizer st = new StringTokenizer(tablelist[i],
						" \t\f\r\n");
				if (st.hasMoreTokens()) {
					if (i > 0)
						sb.append(", ");
					sb.append(prefix);
					sb.append(st.nextToken());
					if (st.hasMoreTokens())
						sb.append(" " + st.nextToken());
				}
			}
			sb.append(sql.substring(m.end(1)));
			return sb.toString();
		} else {
			boolean bFind = false;
			m = PT_insertTable.matcher(sql);
			if (m.find())
				bFind = true;
			else {
				m = PT_updateTable.matcher(sql);
				if (m.find())
					bFind = true;
				else {
					m = PT_truncateTable.matcher(sql);
					bFind = m.find();
				}
			}
			if (bFind) {
				StringBuilder sb = new StringBuilder("");
				sb.append(sql.substring(0, m.start(1)));
				sb.append(prefix);
				sb.append(m.group(1));
				sb.append(sql.substring(m.end(1)));
				return sb.toString();
			} else
				return sql;
		}
	}


  
	
	/**
	 * 
	 * @author syq 
	 * �����ݿ��в���һ��eventnews_�����ļ�¼
	 * @param eventnewsId ����Id
	 * @param tableName ����          
	 * @param tableId ������Id��������ϢId
	 */
/*	public void addEventNewsTable(String eventnewsId, String tableName, int tableId) {
		PoolConection pl = null;

		try {
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			// ����insert into eventnews_person(eventnewsID,personID),tableName����person
			String sql = "insert into eventnews_" + tableName + "(id,eventnewsID,"
					+ tableName + "ID,provinceID) values('";
			sql += UUID.randomUUID().toString().replaceAll("-", "") + "','";
			sql += eventnewsId + "',";
			sql += tableId;
			sql += ","+(""+tableId).substring(0, 2)+")";
			sql = addPrefixToTableName(sql);
			pl.execute(sql);

		} catch (Exception e) {
			System.err.println(e);
		} finally {
			if (pl != null)
				pl.close();
		}
	}*/
	public void addEventNewsTable2(String eventnews_regionId,String eventnewsId, String tableName, int tableId,int provinceId,int eventId,int templateId){
		PoolConection pl = null;
		if(eventnews_regionId==null)
			eventnews_regionId = UUID.randomUUID().toString().replaceAll("-", "");
		try {
			pl = new PoolConection(poolconcfg_app2.getDrv(),poolconcfg_app2.getUrl(),poolconcfg_app2.getUsername(),poolconcfg_app2.getPassword(),3);
			// ����insert into eventnews_person(eventnewsID,personID),tableName����person
			String sql = "insert into eventnews_" + tableName + "(id,eventnewsID,"
					+ tableName + "ID,provinceID,eventID,templateID) values('";
			sql += eventnews_regionId + "','";
			sql += eventnewsId + "',";
			sql += tableId;
			sql += ","+provinceId+","+eventId+","+templateId+")";
			sql = addPrefixToTableName(sql);
			pl.execute(sql);

		} catch (Exception e) {
			System.err.println(e);
		} finally {
			if (pl != null)
				pl.close();
		}
	}
	
	public String addEventNewsRegionTable(String eventnewsId, String tableName, int tableId,int provinceId,int eventId,int templateId) {
		PoolConection pl = null;
		
		try {
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			// ����insert into eventnews_person(eventnewsID,personID),tableName����person
			String sql = "insert into eventnews_" + tableName + "(id,eventnewsID,"
					+ tableName + "ID,provinceID,eventID,templateID) values('";
			String eventnews_regionId=UUID.randomUUID().toString().replaceAll("-", "");
			sql += eventnews_regionId + "','";
			sql += eventnewsId + "',";
			sql += tableId;
			sql += ","+provinceId+","+eventId+","+templateId+")";
			sql = addPrefixToTableName(sql);
			pl.execute(sql);
			return eventnews_regionId;
		} catch (Exception e) {
			System.err.println(e);
			return "-1";
		} finally {
			if (pl != null)
				pl.close();
		}
	}
//	/**
//	 * 
//	 * @author syq 
//	 * �����ݿ��в���һ��eventnews_all_�����ļ�¼
//	 * @param eventnewsID
//	 * @param tablename����          
//	 * @param tableId
//	 */
//	public void addEventNewsAllTable(int eventnewsId, String tableName, int tableId) {
//		PoolConection pl = null;
//
//		try {
//			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
//			// ����insert into eventnews_person(eventnewsID,personID),tableName����person
//			String sql = "insert into eventnews_all_" + tableName + "(eventnewsID,"
//					+ tableName + "ID) values(";
//			sql += eventnewsId + ",";
//			sql += tableId;
//			sql += ")";
//			sql = addPrefixToTableName(sql);
//			pl.execute(sql);
//
//		} catch (Exception e) {
//			System.err.println(e);
//		} finally {
//			if (pl != null)
//				pl.close();
//		}
//	}	
//	
	

//	/**
//	 * ������������б�
//	 * 
//	 * @author hx
//	 * @return
//	 */
//
//	public PersonList getPersonList() {
//
//		PoolConection pl = null;
//		ArrayList<Person> persons = new ArrayList<Person>();
//
//		try {
//			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
//			String sql = "SELECT id,name,post,region_id FROM person";
//			sql = addPrefixToTableName(sql);
//			ResultSet rs = pl.executeQuery(sql);
//
//			while (rs.next()) {
//				// int eId = rs.getInt("eventnewsID"));
//				int pId = rs.getInt("id");
//				String name = rs.getString("name");
//				String post = rs.getString("post");
//				int rId = rs.getInt("region_id");
//
//				persons.add(new Person(pId, name, post, rId));
//			}
//			rs.close();
//			return new PersonList(persons);
//		} catch (Exception e) {
//			System.err.println(e);
//			return null;
//		} finally {
//			if (pl != null)
//				pl.close();
//		}
//
//	}

	/*
	 * ������е����б�
	 * 
	 * @author hx
	 * 
	 * @return
	 */
	public RegionList getRegionList(int provinceId) {
		PoolConection pl = null;

		try {
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);

			String sql = "SELECT  regionID,provinceID,regionName,regionLevel,parentID,regionAbbr FROM region where regionID LIKE '"+provinceId+"%'";
			sql = addPrefixToTableName(sql);
			ArrayList<Region> regions = new ArrayList<Region>();
			ResultSet rs = pl.executeQuery(sql);
			while (rs.next()) {
				// int eId = rs.getInt("eventnewsID"));
				int plId = rs.getInt("regionID");
				int prId = rs.getInt("provinceID");
				String name = rs.getString("regionName");
				int pL = rs.getInt("regionLevel");
				int paId = rs.getInt("parentID");
				String rAb = rs.getString("regionAbbr");

				regions.add(new Region(plId, prId, name, pL, paId, rAb));
			}

			rs.close();
			return new RegionList(regions);
		} catch (Exception e) {
			System.err.println(e);
			return null;
		} finally {
			if (pl != null)
				pl.close();
		}
		
	}
	
	/**
	 * �޸ģ�2012.2.11
	 * 
	 * @author lotus ��������ظ��ļ�¼,�Ƚ����ݺ��¼�id,������eventnews���ظ��ļ�¼
	 * @param content
	 * @return id
	 */
	public int checkDup(String content, int eventId) {
		EventNews anews = null;
		PoolConection pl = null;
		int id=-1;
		try {
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
//			String sql = "select * from eventnews where content=? and eventID=?";
			String sql = "select * from eventnews where summary=? and eventID=?";
			sql = addPrefixToTableName(sql);
			pl.prepareSql(sql);
			pl.setString(content);
			pl.setInt(eventId);

			ResultSet rs = pl.executeQuery();

			if (rs.next()) {
				anews = new EventNews();
				anews.setTitle(rs.getString("title"));
//				anews.setContent(rs.getString("content"));
				anews.setContent(rs.getString("summary"));
				anews.setEventId(rs.getInt("eventID"));
				anews.setSite(rs.getString("webSite"));
				anews.setUrl(rs.getString("url"));
				anews.setId(rs.getString("id"));
				anews.setPreciseDate(rs.getDate("time"));
				id = rs.getInt("id");
			}

			rs.close();
			pl.close();
		//	return anews;
			
			
		} catch (Exception e) {
			System.err.println(e);
		//	return null;
			return -1;
		} finally {
			if (pl != null)
				pl.close();
		}
		return id;
	}
	
	
	/**
	 * ��ȡall��dayDate�ն�Ӧ�ļ�¼
	 * �����޸����ر�׼��ͬһ�졢ͬһ���ط���ͬһ����죬ͬ�����¼�
	 * 2014-6-17 �޸��ظ���ṹ����¼�ظ���Ϣ
	 * @author Bys
	 */
	public ArrayList<DupNewsAll> getAllForJiangXi(Date dayDate) {
		return getAllForJiangXi(dayDate,"eventnews");
	}
	public ArrayList<DupNewsAll> getAllForJiangXi2(Date dayDate) {
		return getAllForJiangXi2(dayDate,"eventnews");
	}
	
	public ArrayList<DupNewsAll> getAllMainInfoForJiangXi(Date dayDate) {
//		if(true)
//			return new ArrayList<DupNewsAll>();
		class DupNewsRegions{
			DupNewsAll dupNews;
			HashMap<Integer, LinkedList<Integer>> province2Region=new HashMap<Integer, LinkedList<Integer>>();
			public DupNewsRegions(DupNewsAll dupNews){
				this.dupNews=dupNews;
			}
			public void addRegion(int regionId){
				int regionLevel=(regionId+"").length()/2;
				int provinceId=Integer.parseInt((regionId+"").substring(0, 2));
				LinkedList<Integer> maxLevelRegions=province2Region.get(provinceId);
				if(maxLevelRegions==null){
					maxLevelRegions=new LinkedList<Integer>();
					maxLevelRegions.add(regionLevel);
					maxLevelRegions.add(regionId);
					province2Region.put(provinceId, maxLevelRegions);
				}else {
					if(regionLevel>maxLevelRegions.get(0)){
						maxLevelRegions.clear();
						maxLevelRegions.add(regionLevel);
						maxLevelRegions.add(regionId);
					}else if(regionLevel==maxLevelRegions.get(0)){
						maxLevelRegions.add(regionId);
					}
				}
			}
			public void setDupNewsRegionIds(){
				LinkedList<Integer> regionIds=new LinkedList<Integer>();
				for(LinkedList<Integer> regions:province2Region.values()){
					regions.remove(0);
					regionIds.addAll(regions);
				}
				dupNews.setRegionIds(regionIds);
			}
		}
		HashMap<String, DupNewsRegions> dupNewsRegionsMap=new HashMap<String, DupNewsRegions>();
		ArrayList<DupNewsAll> dupAlls = new ArrayList<DupNewsAll>();
		PoolConection pl = null;
		JiangxiUtil ju = new JiangxiUtil();
		try{
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			String date = "";
			 date = sdf.format(dayDate);// ��ʽ��ʱ�䵽��
			sdf=null;
			String sql="select eventnews.id id,groupSeedId,publishDate,groupCount,eventnews.eventId eventId,eventnews.provinceID provinceId,regionId,title,summary from wdyq_eventnews eventnews,wdyq_eventnews_region eventnews_region where publishDate>='"+date+" 00:00:00' and publishDate<='"+date+" 23:59:59' and is_mainInfo = '1'"+" and eventnews.provinceId=eventnews_region.provinceId and eventnews.id=eventnews_region.eventnewsId";
			pl.prepareSql(sql);
			ResultSet rs=pl.executeQuery();
			while(rs.next()){
				DupNewsRegions dupNewsRegions=null;
				if((dupNewsRegions=dupNewsRegionsMap.get(rs.getString("id")))!=null){
					dupNewsRegions.addRegion(rs.getInt("regionId"));
				}else {
					DupNewsAll dna = new DupNewsAll();
					dna.setMainInfoId(rs.getString("id"));
					dna.setId(rs.getString("groupSeedId"));//��ͬ����ɷ�Ϊ������Ϣ������Ϣ���ָ��seed������ģ�����Ϣ�������
					dna.setDupCount(rs.getInt("groupCount"));
					dna.setMainInfoDate(rs.getTimestamp("publishDate"));
					//----------------------------------
					dna.setProvinceId(rs.getInt("provinceID"));
					
					dna.setEventId(rs.getInt("eventId"));
					//------------------------------------
					
					dna.setTitle(rs.getString("title"));
					dna.setCoretext(ju.getCoretext(rs.getString("summary")));
					dupNewsRegions=new DupNewsRegions(dna);
					dupNewsRegions.addRegion(rs.getInt("regionId"));
					dupNewsRegionsMap.put(dna.getMainInfoId(), dupNewsRegions);
				}
				
			}
			rs.close();
			for(DupNewsRegions dupNewsRegions:dupNewsRegionsMap.values()){
				dupNewsRegions.setDupNewsRegionIds();
				dupAlls.add(dupNewsRegions.dupNews);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (pl != null)
				pl.close();
		}
		return dupAlls;
	}
	
	/**
	 * ��ȡtableName��Ӧ��dayDate�ն�Ӧ�ļ�¼
	 * @author Bys
	 * @param dayDate
	 * @param tableName
	 * @return
	 */
	public ArrayList<DupNewsAll> getAllForJiangXi(Date dayDate,String tableName) {
			ArrayList<DupNewsAll> dupAlls = new ArrayList<DupNewsAll>();
			
			PoolConection pl = null;
			JiangxiUtil ju = new JiangxiUtil();
			try {				
				pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);

				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				String date = "";
				 date = sdf.format(dayDate);// ��ʽ��ʱ�䵽��
				sdf=null;
//				String searchSeedIdSql = "select id,groupSeedId,publishDate,groupCount,provinceID from "+tableName+" where publishDate like '"+date+"%"+"' and is_mainInfo = '1'";
				String searchSeedIdSql = "select id,groupSeedId,publishDate,groupCount,provinceID from "+tableName+" where publishDate>'"+date+" 00:00:00' and publishDate<'"+date+" 23:59:59' and is_mainInfo = '1'";				searchSeedIdSql = addPrefixToTableName(searchSeedIdSql);
				pl.prepareSql(searchSeedIdSql);
				ResultSet rs = pl.executeQuery();
				while (rs.next()) {
					DupNewsAll dna = new DupNewsAll();
					dna.setMainInfoId(rs.getString("id"));
					dna.setId(rs.getString("groupSeedId"));//��ͬ����ɷ�Ϊ������Ϣ������Ϣ���ָ��seed������ģ�����Ϣ�������
//					if(dna.getId().equals("-1")){
//						dna.setId(dna.getMainInfoId());
//					}
					dna.setDupCount(rs.getInt("groupCount"));
					dna.setMainInfoDate(rs.getTimestamp("publishDate"));
					//----------------------------------
					dna.setProvinceId(rs.getInt("provinceID"));
					
					dna.setEventId(rs.getInt("eventId"));
					//------------------------------------
					
					dna.setTitle(rs.getString("title"));
					dna.setCoretext(ju.getCoretext(rs.getString("summary")));
					dna.setRegionIds(getEventRegionIds(dna.getMainInfoId()));
					
					dupAlls.add(dna);
				}
				rs.close();
				
				
				
//				int count = (seedIds.size()+99)/100;
				for(int i=0;i<dupAlls.size();i++){
					DupNewsAll dna = dupAlls.get(i);
//					String sql = "select id,content,title from eventnews where id = "+dna.getId();
					String sql = "select id,summary,title from eventnews where provinceId="+dna.getProvinceId()+" and id ='"+dna.getId()+"'";
					sql = addPrefixToTableName(sql);
					pl.prepareSql(sql);

					rs = pl.executeQuery();
					if (rs.next()) {
//						dna.setCoretext(ju.getCoretext(rs.getString("content")));
						dna.setCoretext(ju.getCoretext(rs.getString("summary")));
						dna.setTitle(rs.getString("title"));
//						HashSet<String> regioneventIds = getEventRegionIds(dna.getId());
//						List<Integer> regionList = new ArrayList<Integer>();
//						for(String regioneventId:regioneventIds){
//							int rgId = ju.toID(regioneventId);
//							if(rgId!=-1){
//								regionList.add(rgId);
//							}
//						}
//						dna.setRegionIds(regionList);
						dna.setRegionIds(getEventRegionIds(dna.getId()));
					}else {
						System.err.println("sqlError:"+sql);
					}
				}
				pl.close();
				ju=null;
			} catch (SQLException e) {
				e.printStackTrace();
				System.exit(-1);
			} finally {
				if (pl != null)
					pl.close();
			}
			return dupAlls;
	}
	
	public ArrayList<DupNewsAll> getAllForJiangXi2(Date dayDate,String tableName) {
		ArrayList<DupNewsAll> dupAlls = new ArrayList<DupNewsAll>();
		
		PoolConection pl = null;
		JiangxiUtil ju = new JiangxiUtil();
		try {				
			pl = new PoolConection(poolconcfg_app2.getDrv(),poolconcfg_app2.getUrl(),poolconcfg_app2.getUsername(),poolconcfg_app2.getPassword(),3);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			String date = "";
			 date = sdf.format(dayDate);// ��ʽ��ʱ�䵽��
			sdf=null;
//			String searchSeedIdSql = "select id,groupSeedId,publishDate,groupCount,provinceID from "+tableName+" where publishDate like '"+date+"%"+"' and is_mainInfo = '1'";
			String searchSeedIdSql = "select id,groupSeedId,publishDate,groupCount,provinceID from "+tableName+" where publishDate>'"+date+" 00:00:00' and publishDate<'"+date+" 23:59:59' and is_mainInfo = '1'";
			searchSeedIdSql = addPrefixToTableName(searchSeedIdSql);
			pl.prepareSql(searchSeedIdSql);
			ResultSet rs = pl.executeQuery();
			while (rs.next()) {
				DupNewsAll dna = new DupNewsAll();
				dna.setMainInfoId(rs.getString("id"));
				dna.setId(rs.getString("groupSeedId"));//��ͬ����ɷ�Ϊ������Ϣ������Ϣ���ָ��seed������ģ�����Ϣ�������
//				if(dna.getId().equals("-1")){
//					dna.setId(dna.getMainInfoId());
//				}
				dna.setDupCount(rs.getInt("groupCount"));
				dna.setMainInfoDate(rs.getTimestamp("publishDate"));
				//----------------------------------
				dna.setProvinceId(rs.getInt("provinceID"));
				//------------------------------------
				dupAlls.add(dna);
			}
			rs.close();
			
//			int count = (seedIds.size()+99)/100;
			for(int i=0;i<dupAlls.size();i++){
				DupNewsAll dna = dupAlls.get(i);
//				String sql = "select id,content,title from eventnews where id = "+dna.getId();
				String sql = "select id,summary,title from eventnews where provinceId="+dna.getProvinceId()+" and id ='"+dna.getId()+"'";
				sql = addPrefixToTableName(sql);
				pl.prepareSql(sql);

				rs = pl.executeQuery();
				while (rs.next()) {
//					dna.setCoretext(ju.getCoretext(rs.getString("content")));
					dna.setCoretext(ju.getCoretext(rs.getString("summary")));
					dna.setTitle(rs.getString("title"));
//					HashSet<String> regioneventIds = getEventRegionIds(dna.getId());
//					List<Integer> regionList = new ArrayList<Integer>();
//					for(String regioneventId:regioneventIds){
//						int rgId = ju.toID(regioneventId);
//						if(rgId!=-1){
//							regionList.add(rgId);
//						}
//					}
					dna.setRegionIds(getEventRegionIds(dna.getId()));
				}
			}
			pl.close();
			ju=null;
		} catch (Exception e) {
			System.err.println(e);

		} finally {
			if (pl != null)
				pl.close();
		}
		return dupAlls;
}
	
	
	
	/**
	 * ��ӵķ���
	 * 
	 * @author syq 2013/07/19 ͨ�����������Ҷ�Ӧ��RegionId
	 * 
	 */
	public Region getRegionByRegionName(String regionAbbr) {
		PoolConection pl = null;
		Region region=null;
		try {
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			String sql = "SELECT regionId,provinceID,regionName,regionLevel,parentID,regionAbbr FROM region r WHERE  regionAbbr='"
					+ regionAbbr + "'";
//			String sql = "SELECT regionId FROM region r WHERE  regionAbbr= '"+ regionAbbr + "'";
			sql = addPrefixToTableName(sql);
			ResultSet rs = pl.executeQuery(sql);
			
			while (rs.next()) {
				int regionID = rs.getInt("regionID");
				int provinceID = rs.getInt("provinceID");
				String regionName = rs.getString("regionName");
				int regionLevel = rs.getInt("regionLevel");
				int parentID = rs.getInt("parentID");
				region = new Region(regionID, provinceID, regionName,
						regionLevel, parentID, regionAbbr);
				System.out.println(regionID+"-"+regionAbbr);

			}
			rs.close();

		} catch (Exception e) {
			System.err.println(e);

		} finally {
			if (pl != null)
				pl.close();
		}

		return region;
	}
	
	/**
	 * ��ӵķ���
	 * 2014/03/30 ͨ��RegionId���Ҷ�Ӧ��region
	 * @author Bys 
	 * 
	 */
	public Region getRegionById(int id){
		PoolConection pl = null;
		Region region=null;
		try {
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			String sql = "SELECT provinceID,regionName,regionLevel,parentID,regionAbbr FROM region r WHERE  regionId="
					+ id;
//			String sql = "SELECT regionId FROM region r WHERE  regionAbbr= '"+ regionAbbr + "'";
			sql = addPrefixToTableName(sql);
			ResultSet rs = pl.executeQuery(sql);
			
			while (rs.next()) {
				String regionAbbr = rs.getString("regionAbbr");
				int provinceID = rs.getInt("provinceID");
				String regionName = rs.getString("regionName");
				int regionLevel = rs.getInt("regionLevel");
				int parentID = rs.getInt("parentID");
				region = new Region(id, provinceID, regionName,
						regionLevel, parentID, regionAbbr);

			}
			rs.close();

		} catch (Exception e) {
			System.err.println(e);

		} finally {
			if (pl != null)
				pl.close();
		}

		return region;
	}
	
	
	public HashSet<String> getEventRegionIdsOld(String id) {
		HashSet<String> hs = new HashSet<String>();
		PoolConection pl = null;
		int maxLevel=0;
		//StringBuffer s = new StringBuffer("(");
		try {
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);

		//	String sql = "select regionAbbr,regionLevel from eventnews_region er,region r where er.regionid=r.regionid and eventnewsid=?";
			String sql = "select regionLevel from eventnews_region er,region r where er.regionid=r.regionid and eventnewsid=?";
			sql = addPrefixToTableName(sql);
			pl.prepareSql(sql);
			pl.setString(id);
			ResultSet rs = pl.executeQuery();
		
			while (rs.next()) {
				int regionLevel = rs.getInt("regionLevel");
				if(regionLevel>maxLevel){
					maxLevel = regionLevel;
				}
			
			}
			sql = "select regionAbbr,regionLevel from eventnews_region er,region r where er.regionid=r.regionid and eventnewsid=?";	
			sql = addPrefixToTableName(sql);
			pl.prepareSql(sql);
			pl.setString(id);
		     rs = pl.executeQuery();
		
			while (rs.next()) {
				int regionLevel = rs.getInt("regionLevel");
				String regionAbbr = rs.getString("regionAbbr");
				if(regionLevel==maxLevel)
				    hs.add(regionAbbr);
								
			}
			rs.close();

		} catch (Exception e) {
			System.err.println(e);
		} finally {
			if (pl != null)
				pl.close();
		}
		return hs;

	}

	
	public List<Integer> getEventRegionIds(String id){
		LinkedList<Integer> regionIds=new LinkedList<Integer>();
		HashMap<Integer, LinkedList<Integer>> province2Region = new HashMap<Integer, LinkedList<Integer>>();
		PoolConection pl = null;
		try {
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			String sql = "select regionId from eventnews_region where eventnewsid=?";	
			sql = addPrefixToTableName(sql);
			pl.prepareSql(sql);
			pl.setString(id);
			ResultSet rs = pl.executeQuery();
			while(rs.next()){
				int regionId = rs.getInt("regionId");
				int regionLevel=(regionId+"").length()/2;
				int provinceId=Integer.parseInt((regionId+"").substring(0, 2));
				LinkedList<Integer> maxLevelRegions=province2Region.get(provinceId);
				if(maxLevelRegions==null){
					maxLevelRegions=new LinkedList<Integer>();
					maxLevelRegions.add(regionLevel);
					maxLevelRegions.add(regionId);
					province2Region.put(provinceId, maxLevelRegions);
				}else {
					if(regionLevel>maxLevelRegions.get(0)){
						maxLevelRegions.clear();
						maxLevelRegions.add(regionLevel);
						maxLevelRegions.add(regionId);
					}else if(regionLevel==maxLevelRegions.get(0)){
						maxLevelRegions.add(regionId);
					}
				}
			}
			rs.close();
			for(LinkedList<Integer> regions:province2Region.values()){
				regions.remove(0);
				regionIds.addAll(regions);
			}
		}catch (Exception e) {
			System.err.println(e);
		} finally {
			if (pl != null)
				pl.close();
		}
		return regionIds;
	}
	
	
	/**
	 * �����ظ�����
	 * @author Bys
	 * @param eventNews
	 * @param count
	 * @param seedId
	 * @param mainInfoId
	 * @return
	 */
	public String addEventNews(EventNews eventNews,int count,String seedId,boolean isMainInfo,String mainInfoId,int provinceID,Page page){
		Date insert_start=new Date();
		
		String id=addEventNews(eventNews,count,seedId,isMainInfo,mainInfoId,"eventnews",provinceID);
		
		Date insert_end=new Date();
		if(aprop.isTestTime())
			page.insertTimes.add(TimeUtil.decimalFormat.format(((double)(insert_end.getTime()-insert_start.getTime()))/1000));
//			try {
//				Files.append(TimeUtil.statTimeBySecond(insert_start, insert_end)+"\r\n", new File(aprop.getLogLocaltion()+"/insertTime.record"), Charsets.UTF_8);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		return id;
	}
	public String addEventNews2(String id,EventNews eventNews,int count,String seedId,boolean isMainInfo,String mainInfoId,int provinceID){
		return addEventNews2(id,eventNews,count,seedId,isMainInfo,mainInfoId,"eventnews",provinceID);
	}
	/**
	 * ���ظ��������tableName��Ӧ����
	 * @param eventNews
	 * @param count
	 * @param seedId
	 * @param isMainInfo
	 * @param mainInfoId
	 * @param tableName
	 * @return
	 */
	public synchronized String addEventNews(EventNews eventNews,int count,String seedId,boolean isMainInfo,String mainInfoId,String tableName,int provinceID) {
		String id = UUID.randomUUID().toString().replaceAll("-", "");
		PoolConection pl = null;

		try {
			System.out.println(eventNews.getPageId()+" new PoolConection start");
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			System.out.println(eventNews.getPageId()+" new PoolConection end");
			
			//��������
			while(!pl.transaction_start());
			
			String updateSql = "";
			//���������������Ϣ������ǰ������Ϣ��־λ��0
			if(isMainInfo)
				updateSql = "update "+tableName+" set is_mainInfo = '0' where provinceId="+provinceID+" and id = '"+mainInfoId+"'";
			else
				//�����������Ϣ��������Ϣ��count��+1
				updateSql = "update eventnews set groupCount = "+count+" where provinceId="+provinceID+" and id = '"+mainInfoId+"'";
			updateSql = addPrefixToTableName(updateSql);
			System.out.println(eventNews.getPageId()+" update eventnews start");
			pl.execute(updateSql);
			System.out.println(eventNews.getPageId()+" update eventnews end");
//			String insertSql = "insert into "+tableName+"(id,title,content,webSite,eventID,templateId,url,produceDate,polarity,srcType,sitePriority,newsLevel,pageId,webSiteplate,publishDate,groupCount,groupSeedId,is_mainInfo,provinceID) "
//					+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			String insertSql = "insert into "+tableName+"(id,title,summary,webSite,eventID,templateId,url,produceDate,polarity,type,sitePriority,newsLevel,pageId,webSiteplate,publishDate,groupCount,groupSeedId,is_mainInfo,provinceID) "
					+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			insertSql = addPrefixToTableName(insertSql);
			pl.prepareSql(insertSql);
			pl.setString(id);
			pl.setString(eventNews.getTitle());
			if(eventNews.getContent().length()>255)
				pl.setString(eventNews.getContent().substring(0,253)+"��");
			else
				pl.setString(eventNews.getContent());
			pl.setString(eventNews.getSite());
			pl.setInt(eventNews.getEventId());
			pl.setInt(eventNews.getTemplateID());
			pl.setString(eventNews.getUrl());
			pl.setDate(new Date());
			pl.setInt(eventNews.getPolarity());
			pl.setInt(eventNews.getSrcType());
			pl.setInt(eventNews.getSitePriority());
			pl.setInt(eventNews.getNewsLevel());
			pl.setString(eventNews.getPageId());
			pl.setString(eventNews.getWebSiteplate());
			pl.setDate(eventNews.getPreciseDate());
			if(isMainInfo)
				pl.setInt(count);
			else
				pl.setInt(1);
			pl.setString(seedId);
			if(isMainInfo)
				pl.setString("1");
			else
				pl.setString("0");
			pl.setInt(provinceID);
			pl.execute();
			
			pl.transaction_commit();
//			id = getIdentity(pl);
			return id;
		} catch (Exception e) {
			System.err.println(e);
			try {
				pl.rollback();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return "-1";
		} finally {
			if (pl != null)
				pl.close();
		}
	}
	
	public synchronized String addEventNews2(String id,EventNews eventNews,int count,String seedId,boolean isMainInfo,String mainInfoId,String tableName,int provinceID) {
		PoolConection pl = null;
		if(id==null)
			id = UUID.randomUUID().toString().replaceAll("-", "");
		try {
			
			pl = new PoolConection(poolconcfg_app2.getDrv(),poolconcfg_app2.getUrl(),poolconcfg_app2.getUsername(),poolconcfg_app2.getPassword(),3);
			//��������
			while(!pl.transaction_start());
			
			String updateSql = "";
			//���������������Ϣ������ǰ������Ϣ��־λ��0
			if(isMainInfo)
				updateSql = "update "+tableName+" set is_mainInfo = '0' where id = '"+mainInfoId+"'";
			else
				//�����������Ϣ��������Ϣ��count��+1
				updateSql = "update eventnews set groupCount = "+count+" where id = '"+mainInfoId+"'";
			updateSql = addPrefixToTableName(updateSql);
			pl.execute(updateSql);
			
//			String insertSql = "insert into "+tableName+"(id,title,content,webSite,eventID,templateId,url,produceDate,polarity,srcType,sitePriority,newsLevel,pageId,webSiteplate,publishDate,groupCount,groupSeedId,is_mainInfo,provinceID) "
//					+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			String insertSql = "insert into "+tableName+"(id,title,summary,webSite,eventID,templateId,url,produceDate,polarity,type,sitePriority,newsLevel,pageId,webSiteplate,publishDate,groupCount,groupSeedId,is_mainInfo,provinceID) "
					+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			insertSql = addPrefixToTableName(insertSql);
			pl.prepareSql(insertSql);
			pl.setString(id);
			pl.setString(eventNews.getTitle());
			if(eventNews.getContent().length()>255)
				pl.setString(eventNews.getContent().substring(0,253)+"��");
			else
				pl.setString(eventNews.getContent());
			pl.setString(eventNews.getSite());
			pl.setInt(eventNews.getEventId());
			pl.setInt(eventNews.getTemplateID());
			pl.setString(eventNews.getUrl());
			pl.setDate(new Date());
			pl.setInt(eventNews.getPolarity());
			pl.setInt(eventNews.getSrcType());
			pl.setInt(eventNews.getSitePriority());
			pl.setInt(eventNews.getNewsLevel());
			pl.setString(eventNews.getPageId());
			pl.setString(eventNews.getWebSiteplate());
			pl.setDate(eventNews.getPreciseDate());
			if(isMainInfo)
				pl.setInt(count);
			else
				pl.setInt(1);
			pl.setString(seedId);
			if(isMainInfo)
				pl.setString("1");
			else
				pl.setString("0");
			pl.setInt(provinceID);
			pl.execute();
			
			pl.transaction_commit();
//			id = getIdentity(pl);
			return id;
		} catch (Exception e) {
			System.err.println(e);
			try {
				pl.rollback();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return "-1";
		} finally {
			if (pl != null)
				pl.close();
		}
	}
	
	/**
	 * ������ظ�����
	 * @author Bys
	 * @param eventNews
	 * @return
	 */
	public String addEventNews(EventNews eventNews,int provinceID,Page page){
		Date insert_start=new Date();
		
		String id=addEventNews(eventNews,"eventnews",provinceID);
		
		Date insert_end=new Date();
		if(aprop.isTestTime())
			page.insertTimes.add(TimeUtil.decimalFormat.format(((double)(insert_end.getTime()-insert_start.getTime()))/1000));
//			try {
//				Files.append(TimeUtil.statTimeBySecond(insert_start, insert_end)+"\r\n", new File(aprop.getLogLocaltion()+"/insertTime.record"), Charsets.UTF_8);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		return id;
	}
	
	public String addEventNews2(String id,EventNews eventNews,int provinceID){
		return addEventNews2(id,eventNews,"eventnews",provinceID);
	}
	
	public String addEventNews(EventNews eventNews,String tableName,int provinceID) {
		String id = UUID.randomUUID().toString().replaceAll("-", "");
		PoolConection pl = null;

		try {
			System.out.println(eventNews.getPageId()+" new PoolConection start");
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			System.out.println(eventNews.getPageId()+" new PoolConection end");
//			String sql = "insert into "+tableName+"(id,title,content,webSite,eventID,templateId,url,produceDate,polarity,srcType,sitePriority,newsLevel,pageId,webSiteplate,publishDate,groupCount,groupSeedId,is_mainInfo,provinceID) "
//					+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			String sql = "insert into "+tableName+"(id,title,summary,webSite,eventID,templateId,url,produceDate,polarity,type,sitePriority,newsLevel,pageId,webSiteplate,publishDate,groupCount,groupSeedId,is_mainInfo,provinceID) "
					+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			sql = addPrefixToTableName(sql);
			pl.prepareSql(sql);
			System.out.println(eventNews.getPageId()+" insert start");
			pl.setString(id);
			pl.setString(eventNews.getTitle());
			if(eventNews.getContent().length()>255)
				pl.setString(eventNews.getContent().substring(0,253)+"��");
			else
				pl.setString(eventNews.getContent());
			pl.setString(eventNews.getSite());
			pl.setInt(eventNews.getEventId());
			pl.setInt(eventNews.getTemplateID());
			pl.setString(eventNews.getUrl());
			pl.setDate(new Date());
			pl.setInt(eventNews.getPolarity());
			pl.setInt(eventNews.getSrcType());
			pl.setInt(eventNews.getSitePriority());
			pl.setInt(eventNews.getNewsLevel());
			pl.setString(eventNews.getPageId());
			pl.setString(eventNews.getWebSiteplate());
			pl.setDate(eventNews.getPreciseDate());
			pl.setInt(1);
//			pl.setInt(-1);
			pl.setString(id);
			pl.setString("1");
			pl.setInt(provinceID);
			pl.execute();
//			id = getIdentity(pl);
			System.out.println(eventNews.getPageId()+" insert end");
			return id;
		} catch (Exception e) {
			System.err.println(e);
			return "-1";
		} finally {
			if (pl != null)
				pl.close();
		}
	}
	
	public String addEventNews2(String id,EventNews eventNews,String tableName,int provinceID) {

		PoolConection pl = null;
		if(id==null)
			id = UUID.randomUUID().toString().replaceAll("-", "");
		try {
			
			pl = new PoolConection(poolconcfg_app2.getDrv(),poolconcfg_app2.getUrl(),poolconcfg_app2.getUsername(),poolconcfg_app2.getPassword(),3);
//			String sql = "insert into "+tableName+"(id,title,content,webSite,eventID,templateId,url,produceDate,polarity,srcType,sitePriority,newsLevel,pageId,webSiteplate,publishDate,groupCount,groupSeedId,is_mainInfo,provinceID) "
//					+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			String sql = "insert into "+tableName+"(id,title,summary,webSite,eventID,templateId,url,produceDate,polarity,type,sitePriority,newsLevel,pageId,webSiteplate,publishDate,groupCount,groupSeedId,is_mainInfo,provinceID) "
					+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			sql = addPrefixToTableName(sql);
			pl.prepareSql(sql);
			pl.setString(id);
			pl.setString(eventNews.getTitle());
			if(eventNews.getContent().length()>255)
				pl.setString(eventNews.getContent().substring(0,253)+"��");
			else
				pl.setString(eventNews.getContent());
			pl.setString(eventNews.getSite());
			pl.setInt(eventNews.getEventId());
			pl.setInt(eventNews.getTemplateID());
			pl.setString(eventNews.getUrl());
			pl.setDate(new Date());
			pl.setInt(eventNews.getPolarity());
			pl.setInt(eventNews.getSrcType());
			pl.setInt(eventNews.getSitePriority());
			pl.setInt(eventNews.getNewsLevel());
			pl.setString(eventNews.getPageId());
			pl.setString(eventNews.getWebSiteplate());
			pl.setDate(eventNews.getPreciseDate());
			pl.setInt(1);
//			pl.setInt(-1);
			pl.setString(id);
			pl.setString("1");
			pl.setInt(provinceID);
			pl.execute();
//			id = getIdentity(pl);
			return id;
		} catch (Exception e) {
			System.err.println(e);
			return "-1";
		} finally {
			if (pl != null)
				pl.close();
		}
	}
	
	public void updateSubjectCount(){
		PoolConection pl = null;
//		ResultSet rs=null;
		try {
			if(aprop.getAppNum()==1)
				pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			else {
				pl = new PoolConection(poolconcfg_app2.getDrv(),poolconcfg_app2.getUrl(),poolconcfg_app2.getUsername(),poolconcfg_app2.getPassword(),3);
			}
			Set<Integer> proIds=WebPageAnalyzer.tempProSubMap.keySet();
			for(Integer proId:proIds){
				String sql="select countUpdated,countTotal from pe_t_subject where provinceid="+proId;
				ResultSet rs=pl.executeQuery(sql);
				rs.next();
				int updateNum=rs.getInt("countUpdated");
				int totalNum=rs.getInt("countTotal");
				updateNum+=WebPageAnalyzer.tempProSubMap.get(proId);
				totalNum+=WebPageAnalyzer.tempProSubMap.get(proId);
				sql="update pe_t_subject set countUpdated="+updateNum+", lastUpdatedTime=now()"+" where provinceid="+proId;
				pl.execute(sql);
				sql="update pe_t_subject set countTotal="+totalNum+" where provinceid="+proId;
				pl.execute(sql);
				
				rs.close();
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (pl != null)
				pl.close();
		}
	}
	public void cleanSubjectCount(){
		PoolConection pl = null;
		try {
			if(aprop.getAppNum()==1)
				pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			else {
				pl = new PoolConection(poolconcfg_app2.getDrv(),poolconcfg_app2.getUrl(),poolconcfg_app2.getUsername(),poolconcfg_app2.getPassword(),3);
			}
			String sql="update pe_t_subject set countUpdated=0 , lastUpdatedTime=now() where provinceid<>-1";
			pl.execute(sql);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (pl != null)
				pl.close();
		}
	}
	public void initProSubCount(){
		PoolConection pl = null;
		ResultSet rs=null;
		try {
			if(aprop.getAppNum()==1)
				pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			else {
				pl = new PoolConection(poolconcfg_app2.getDrv(),poolconcfg_app2.getUrl(),poolconcfg_app2.getUsername(),poolconcfg_app2.getPassword(),3);
			}
			String sql="select provinceid from  pe_t_subject where provinceid<>-1";
			rs=pl.executeQuery(sql);
			while(rs.next()){
				WebPageAnalyzer.proSubCountMap.put(rs.getInt("provinceid"), 0);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
//			if(rs!=null)
//				rs.close();
			if (pl != null)
				pl.close();
		}
	}
	public int queryAnalysis(Date date){	
		int num=0;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int day=calendar.get(Calendar.DAY_OF_MONTH);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = sdf.format(date);
	    if(0<day&&day<=10)
	    {
	    	num = 1;	    	
	    }
	    else if(10<day&&day<=20)
	    {
	    	num=2;
	    }else
	    {
	    	num=3;
	    }
	    String tableName = "wdyq_pages_"+dateString.substring(0,4)+""+dateString.substring(5,7)+"_"+num;
	    String sql = "select count(*) FROM "+tableName+" where ";
		return num;
	}
	
	/**
	 * ��ձ����е��쵼��
	 */
	public void cleanLeader(){
		PoolConection pl = null;
		int keywordGroupID = -2;
		try {
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);
			String sql = "select keywordGroupID from wdyq_keywordgroup_en where keywordGroupName='�쵼'";
			ResultSet rs = pl.executeQuery(sql);
			if (rs.next()) {
				keywordGroupID = rs.getInt("keywordGroupID");
			}
			rs.close();		
			sql = "delete from wdyq_keyword_en where keywordGroupID = "+keywordGroupID;
			pl.execute(sql);
			pl.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (pl != null)
				pl.close();
		}
	}
	

	
	/**
	 * 
	 * 2014/5/9
	 * @author add by zhou
	 * 
	 */
	public RegionList get_ALL_RegionList() {
		PoolConection pl = null;

		try {
			pl = new PoolConection(poolconcfg.getDrv(),poolconcfg.getUrl(),poolconcfg.getUsername(),poolconcfg.getPassword(),1);

			String sql = "SELECT  regionID,provinceID,regionName,regionLevel,parentID,regionAbbr FROM region";
			sql = addPrefixToTableName(sql);
			ArrayList<Region> regions = new ArrayList<Region>();
			ResultSet rs = pl.executeQuery(sql);
			while (rs.next()) {
				// int eId = rs.getInt("eventnewsID"));
				int plId = rs.getInt("regionID");
				int prId = rs.getInt("provinceID");
				String name = rs.getString("regionName");
				int pL = rs.getInt("regionLevel");
				int paId = rs.getInt("parentID");
				String rAb = rs.getString("regionAbbr");

				regions.add(new Region(plId, prId, name, pL, paId, rAb));
			}

			rs.close();
			return new RegionList(regions);
		} catch (Exception e) {
			System.err.println(e);
			return null;
		} finally {
			if (pl != null)
				pl.close();
		}
		
	}
	
	
	public Page getPageByID(String id){
		Page page=new Page();
		PoolConection conn=null;
		Statement stm=null;
		ResultSet rs=null;
		try {
			conn=new PoolConection(poolconcfg_server.getDrv(),poolconcfg_server.getUrl(),poolconcfg_server.getUsername(),poolconcfg_server.getPassword(),0);;
			String sql = "SELECT id,title,webSite,webSiteplate,url,downloadDate,summary,type,publishDate,sitePriority,newsLevel FROM wdyq_pages where id='"+id+"'" ;
//			String sql = "SELECT id,title,webSite,webSiteplate,url,downloadDate,summary,type,publishDate,sitePriority,newsLevel FROM wdyq_pages where downloadDate >'"+downloadSdf.format(new Date(nowLong-60*1000*2))+"' and (indexedStatus=1 or indexedStatus=3 or indexedStatus=20)";
			rs=conn.executeQuery(sql);
			if(rs.next()){
//				System.out.println(rs.getString("downloaddate"));
				String summary = rs.getString("summary");
				page.setId(rs.getString("id"));
				page.setType(rs.getInt("type"));
				//�����΢��������ֱ�Ӷ�summary
				if(page.getType()==3||page.getType()==6){
					if(summary==null){
						page.setTitle("");
					}else{
						int byteLength = summary.getBytes("UTF-8").length;
						if(byteLength>200){							
							while(byteLength>180){
								summary = summary.substring(0,summary.length()-1);
								byteLength = summary.getBytes("UTF-8").length;
							}
							summary += "����";
						}
						page.setTitle(summary);
					}
				}
				else {
					page.setTitle(rs.getString("title"));
				}
//				page.setFn(rs.getString("fn"));
				page.setWebSite(rs.getString("webSite"));
				page.setUrl(rs.getString("url"));
				page.setDownloadDate(rs.getTimestamp("downloadDate"));
				page.setWebSiteplate(rs.getString("webSiteplate"));
				page.setPublishDate(rs.getTimestamp(("publishDate")));
				page.setSummary(rs.getString("summary"));
				page.setNewsLevel(rs.getInt("newsLevel"));
				page.setSitePriority(rs.getInt("sitePriority"));
				page.setContent(new AnalysisUtil().getCoreContent_new(page));
//				if(rs.getInt("type")==3||rs.getInt("type")==6){
//					if(summary!=null)
//						page.setContent(rs.getString("summary"));
//					else
//						page.setContent("");
//				}else 
//					page.setText(storage.get(page.getId(),true));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(rs!=null)
				try {
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			if(conn!=null)
					conn.close();
				
		}
		return page;
	}
	
	public String insertCarEvent(String fileName,String eventName,String content,int polarity){
		String id=UUID.randomUUID().toString().replaceAll("-", "");
		PoolConection pl=null;
		try {
			pl = new PoolConection(poolconcfg_ontology.getDrv(),poolconcfg_ontology.getUrl(),poolconcfg_ontology.getUsername(),poolconcfg_ontology.getPassword(),2);
			String insertSql = "insert into wdyq_eventnews(id,fileName,eventName,content,polarity) values(?,?,?,?,?)";
			pl.prepareSql(insertSql);
			pl.setString(id);
			pl.setString(fileName);
			pl.setString(eventName);
			pl.setString(content);
			pl.setInt(polarity);;
			pl.execute();
		} catch (Exception e) {
			e.printStackTrace();
			id=null;
		} finally {
			if (pl != null)
				pl.close();
		}
		return id;
	}
	public String insertCarEvent(String id,String fileName,String firstCategary,String eventName,String content,int polarity){
		PoolConection pl=null;
		try {
			pl = new PoolConection(poolconcfg_ontology.getDrv(),poolconcfg_ontology.getUrl(),poolconcfg_ontology.getUsername(),poolconcfg_ontology.getPassword(),2);
			String insertSql = "insert into wdyq_eventnews(id,fileName,firstCategary,eventName,content,polarity) values(?,?,?,?,?,?)";
			pl.prepareSql(insertSql);
			pl.setString(id);
			pl.setString(fileName);
			pl.setString(firstCategary);
			pl.setString(eventName);
			pl.setString(content);
			pl.setInt(polarity);;
			pl.execute();
		} catch (Exception e) {
			e.printStackTrace();
			id=null;
		} finally {
			if (pl != null)
				pl.close();
		}
		return id;
	}
}
