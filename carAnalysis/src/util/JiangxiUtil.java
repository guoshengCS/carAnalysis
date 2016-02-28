package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dataAnalysis.WebPageAnalyzer;

import javabean.db.Region;

import snooway.dao.DataDao;

/**
 *JiangxiPostMatcher �����һЩ������
 * @author hx
 * 
 */
public class JiangxiUtil {
	
//	private static String negtiveWordList = "����|����|����|������|û��|��û|��δ|��δ|��δ|��δ|��δ|��Ҫ|��";
	DataDao da = WebPageAnalyzer.da;


	public int getMaxLevelofRegions(List<Integer> regionList){
		int maxId=0;
		
		for(int id:regionList){
			/*Region r = da.getRegionById(id);
			if(r!=null&&r.getRegionLevel()>maxId)
			  maxId = r.getRegionLevel();*/
			int regionLevel=(""+id).length()/2;
			if(regionLevel>maxId)
				maxId=regionLevel;
		}
		return maxId;
	}
	
	
	/**
	 * 
	 * @param regionList
	 * @return
	 */
	public List<Integer> getMaxLevelRegions(List<Integer> regionList) {
		/*List<Integer>  hs = new ArrayList<Integer>();
		if (regionList != null) {		
			int maxId = getMaxLevelRegions(regionList);
			for (int id : regionList) {
			Region  r = da.getRegionById(id);// �õ������㼶
		//	System.out.println("Region:"+r);
			if(r!=null&&r.getRegionLevel()==maxId)
			    hs.add(id);
				if((""+id).length()/2==maxId)
					hs.add(id);
			}	
			return hs;
		}
		return null;*/
		LinkedList<Integer> regionIds=new LinkedList<Integer>();
		HashMap<Integer, LinkedList<Integer>> province2Region = new HashMap<Integer, LinkedList<Integer>>();
		for(int regionId:regionList){
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
		for(LinkedList<Integer> regions:province2Region.values()){
			regions.remove(0);
			regionIds.addAll(regions);
		}
		return regionIds;
	}
	
	
	public int getMaxLevelRegionsFromName(List<String> regionList){
		int maxId=0;
		
		for(String str:regionList){
			Region r = da.getRegionByRegionName(str);
			if(r!=null&&r.getRegionLevel()>maxId)
			  maxId = r.getRegionLevel();
		}
		return maxId;
	}
	
	
	/**
	 * 
	 * @param regionList
	 * @return
	 */
	public HashSet<String> getFromName(List<String> regionList) {
		HashSet<String>  hs = new HashSet<String>();
		
		if (regionList != null) {		
			for (String str : regionList) {
			Region  r = da.getRegionByRegionName(str);// �õ������㼶
		//	System.out.println("Region:"+r);
			if(r!=null&&r.getRegionLevel()==getMaxLevelRegionsFromName(regionList))
			    hs.add(str);
			}	
			return hs;
		}
		return null;
	}
	
	

	public boolean compare(HashSet<String> h1,HashSet<String> h2){
	  if(h1.size()!=h2.size()) return false;
		
		if(h1.containsAll(h2)&&h2.containsAll(h1)) return true;
		
		else return false;
	
		
	}
	
	/**
	 * �õ������¼�
	 * @param text
	 * @return
	 */
	public String getCoretext(String text) {
		String _text = text;
/*		Pattern p1 = Pattern.compile("<span class=\"br\">");
		Matcher m1 = p1.matcher(text);
		int start = 0,end = text.length() ;
		if(m1.find()){
			start = m1.end();			
		}
		
		Pattern p2 = Pattern.compile("</span>");
		Matcher m2 = p2.matcher(text);
		while(m2.find()){
			end=m2.start();
			if(end>start) break;
		}
		
		String coreText =  text.substring(start, end);
	//	String filterReg = "<span class=\"br\">|<span class=\"bg\">|</span>";
		String filterReg1 = "<span class=\"br\">";
		String filterReg2 = "<span class=\"bg\">";
		String filterReg3 = "</span>";
		coreText=coreText.replace(filterReg1, "").replace(filterReg2, "").replace(filterReg3, "");
		return coreText;*/
		StringBuffer coreText=new StringBuffer("");
		String eventLabel="<span class=\"eventlabel\">";
		String areaLabel="<span class=\"arealabel\">";
		String endLabel="</span>";
		Pattern pattern=Pattern.compile(eventLabel);
		Matcher matcher=pattern.matcher(text);
		int start=0,end=0,count=0;//count��ջʹ��
		if(matcher.find()){
			count=1;
			pattern=Pattern.compile(endLabel+"|"+areaLabel+"|"+endLabel);
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
					return _text;
				}
			}
		}
		return coreText.toString();
		

	}
	/**
	 * �ж�ĳ�������Ƿ�����ĳ��ʡ
	 */
	public boolean isRegionOfProvince(String abbr,int provinceId){
		Region region = da.getRegionByRegionName(abbr);
	//	System.out.println("�ϵ�1"+region.getProvinceID());
		if(region!=null&&region.getProvinceID()==provinceId){
			return true;
		}
		return false;
		
	}
	
	
	
	
	
	public int opinionExtractor(String content, int templatePolarity,
			String negative, int strbegin, int x_length) {
		int polarity = templatePolarity;// //����ֵ��ʼΪ ģ�弫��

		if (strbegin > content.length())// ///////������������ ֱ�ӷ��� ����
			return polarity;

		Pattern p = Pattern.compile(negative);

		String contenttemp = "";// //////////ȡ���۴�ǰX����

		contenttemp = content.substring(Math.max(0, strbegin - x_length),
				strbegin);

		Matcher m = p.matcher(contenttemp);// /////������� ���� *-1

		if (m.find()) {
			polarity = polarity * (-1);
		}

		// ������۴�ǰ����һ���������֣����Է�ת
		if (contenttemp.indexOf("��") == x_length - 1) {
			polarity = polarity * (-1);
		}

		boolean flag = contenttemp.indexOf("��û��") != -1
				|| contenttemp.indexOf("�ǲ���") != -1;
		// ||contenttemp.indexOf("����һ��")!=-1
		// �� ������һ�㡱ȥ�˰� �о����˷�������ô��

		if (flag)// /���� ����û�С� ���ǲ��ǡ� �Ĵ���
			return 1;
		if (contenttemp.indexOf("����һ��") != -1)
			polarity = polarity * (-1);
		// System.out.println("������2��"+polarity);
		return polarity;
	}

	/**
	 * Most important: return true if this MatchedResult is to be killed *
	 * 
	 * @author hx 
	 * ���е���ȥ����,���Ϲ��򷵻�true�������Ϸ���false ����������и����ɡ�����ͨ��
	 * ��������и����أ��У��ݣ���,��ί����ͨ�� ������������У��������������������Ϻ�������ͨ�� else return false
	 */
	public boolean monitorRule(String text,String regionName)
	{

		int regionEnd = 0;
		int regionStart = 0;

		Pattern pattern = Pattern.compile(regionName);
		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) {
			regionEnd = matcher.end();
			regionStart = matcher.start();
		}
		String rule = text.substring(Math.max(0, regionStart-3), regionEnd
				+ Math.min(text.length() - regionEnd, 6));// ������ǰ3��6����
		// ����������и����ɡ�����ͨ��

		// System.out.println(rule);
		if (rule.indexOf("��") == 0 || rule.indexOf("��") == 0||rule.contains("����") ||rule.contains("����")||rule.contains("����")||rule.contains("��̳")||rule.contains("����")||rule.contains("��")||rule.contains("Ƶ��")) {
			// �����**�ɣ�ֱ��return
			System.out.println("��վ��ַ�ų�����");
			return true;
		}
		return false;
	}
	
	
	/**
	 * @author Bys
	 * @param name
	 * @return
	 */
	public int toID(String name){
		Region r = da.getRegionByRegionName(name);
		if(r!=null)
			return r.getRegionID();
		else 
			return -1;
	}

//	public static void main(String[] args) {
////		JiangxiUtil yu = new JiangxiUtil();
//		//	PoolConection.init("DataAccess.properties");
//	//	List<String> regionList = new ArrayList<String>();
//	//	regionList.add("����");
//	//	regionList.add("����");
//		
//	//	System.out.println(yu.get(regionList));
//		
//		
////		String str = "<span class=\"br\"><span class=\"bg\">����</span>���г�</span>���ܲ������£���а�ţ� ���Ǵ�<span class=\"bg\">����</span>�г�����12345�������";
////		System.out.println(yu.getCoretext(str));
//		
//		
//	}

}
