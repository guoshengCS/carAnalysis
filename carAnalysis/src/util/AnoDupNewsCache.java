package util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javabean.DupNewsAll;

import dataAnalysis.WebPageAnalyzer;


/**
 * �ظ����黺�棬Ĭ�Ͻ����3�������������Ϣ�������ڴ棬������ֱ�����ڴ��бȽ�
 * @author Bys
 *
 */
public class AnoDupNewsCache {
	private final static int CACHE_DAY = AnalysisProperties.getInstance().getCacheDays();
	
	private static Map<String, AnoDupNewsCache> instanceMap = new HashMap<String, AnoDupNewsCache>(CACHE_DAY,1);
//	private ArrayList< LinkedList<DupNewsAll> > cache = null;
	private ArrayList<DupNewsAll> cache = null;

	@SuppressWarnings("unused")
	private Date lastVisit = null;

	private AnoDupNewsCache() {
//		cache = new ArrayList< LinkedList<DupNewsAll> >();
		cache = new ArrayList<DupNewsAll>();
	}
	
	/**
	 * �������ڻ�ȡ����ȥ�ػ�����
	 * @param date
	 * @return
	 */
	public static AnoDupNewsCache getInstance(Date date){
		AnoDupNewsCache instance = instanceMap.get(WebPageAnalyzer.dateFormat.format(date));
//		if(instance == null){
//			instance = addCache(date);
//		}
		return instance;
	}
	
	
	public static AnoDupNewsCache addCache(Date date){
		if(!instanceMap.containsKey(WebPageAnalyzer.dateFormat.format(date))){
			AnoDupNewsCache instance = new AnoDupNewsCache();
			instance.cache = WebPageAnalyzer.da.getAllForJiangXi2(date);
			instance.lastVisit = new Date();
			instanceMap.put(WebPageAnalyzer.dateFormat.format(date), instance);
		}
		return instanceMap.get(date);
	}
	
	public static boolean removeCache(Date date){
		if(instanceMap.containsKey(WebPageAnalyzer.dateFormat.format(date))){
			instanceMap.remove(WebPageAnalyzer.dateFormat.format(date));
			return true;
		}
		return false;
	}
	
	public DupNewsAll checkDup(DupNewsAll news){
		//û�е��컺��ֱ�Ӳ�ȥ��
		if(cache == null)
			return null;
		else{
			//�����ȸ���regionIds���ж��ֲ��ң��õ���ѡ���
			List<DupNewsAll> candidate = searchDup(news.getRegionIds());
			//���ں�ѡ����в����Ƿ����ظ���
			int index = candidate.indexOf(news);
			if(index != -1)
				return candidate.get(index);
			else {
				return null;
			}
		}
	}
	
	public DupNewsAll get(int index){
		return cache.get(index);
	}

	/**
	 * ���ֲ��Һ�regionIds��ȫ��ͬ���ظ���¼
	 * @param regionIds
	 * @return
	 */
	public List<DupNewsAll> searchDup(String regionIds){
		return cache;
	}
	
	
	public void add(DupNewsAll news) {
		cache.add(news);
	}
	
}
