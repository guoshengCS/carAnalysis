package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import javabean.DupNewsAll;

import dataAnalysis.WebPageAnalyzer;


/**
 * �ظ����黺�棬Ĭ�Ͻ����3�������������Ϣ�������ڴ棬������ֱ�����ڴ��бȽ�
 * @author Bys
 *
 */
public class DupNewsCache {
	private final static int CACHE_DAY = AnalysisProperties.getInstance().getCacheDays();
//	private static Map<String, ArrayList<DupNewsCache>> instanceMap = new HashMap<String, ArrayList<DupNewsCache>>();
	private static Map<String, DupNewsCache[]> instanceMap = new HashMap<String, DupNewsCache[]>();
//	private static Map<String, DupNewsCache> instanceMap = new HashMap<String, DupNewsCache>(CACHE_DAY,1);
//	private ArrayList< LinkedList<DupNewsAll> > cache = null;
	private ArrayList<DupNewsAll> cache = null;
	
//	private static Map<String, Long[]> cacheDisMap=new HashMap<String, Long[]>();
//	private static long[][] cacheDis=new long[WebPageAnalyzer.aprop.getCacheDays()][WebPageAnalyzer.aprop.getCacheNum()];
	public HashMap<String, DupNewsAll> titleMap=new HashMap<String,DupNewsAll>();
	public HashMap<String, DupNewsAll> textMap=new HashMap<String,DupNewsAll>();

	@SuppressWarnings("unused")
	private Date lastVisit = null;

	private DupNewsCache() {
//		cache = new ArrayList< LinkedList<DupNewsAll> >();
		cache = new ArrayList<DupNewsAll>();
	}
	
	public static int getCacheIndex(List<Integer> regionIds){
		int cacheIndex=0;
		/*int sum=0;
		for(Integer regionId:regionIds){
//			regions.append(regionId+"");
			sum+=regionId;
		}
		cacheIndex=sum%WebPageAnalyzer.aprop.getCacheNum();
		try {
			Files.append(cacheIndex+"N"+sum+"\r\n",new File(WebPageAnalyzer.aprop.getLogLocaltion()+"/cacheDis.record"), Charsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		Collections.sort(regionIds);
		String regionIdsStr="";
		for(int id:regionIds){
			regionIdsStr+="|"+id;
		}
		return getCacheIndex(regionIdsStr);
	}
	public static int getCacheIndex(String regionIdStr){
		int cacheIndex=0;
		/*String[] regionIds=regionIdStr.substring(1).split("\\|");
		int sum=0;
		for(String regionId:regionIds){
			sum+=Integer.parseInt(regionId);
		}
		cacheIndex=sum%WebPageAnalyzer.aprop.getCacheNum();
		try {
			Files.append(cacheIndex+"O"+sum+"\r\n",new File(WebPageAnalyzer.aprop.getLogLocaltion()+"/cacheDis.record"), Charsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		if(regionIdStr==null){
//			System.err.println("regionIdStrΪ�գ�");
			return 0;
		}
		cacheIndex=Math.abs(Hashing.md5().hashString(regionIdStr, Charsets.UTF_8).asInt())%WebPageAnalyzer.aprop.getCacheNum();
		if(WebPageAnalyzer.aprop.isTestTime())
			try {
				Files.append(cacheIndex+"O"+regionIdStr+"\r\n",new File(WebPageAnalyzer.aprop.getLogLocaltion()+"/cacheDis.record"), Charsets.UTF_8);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return cacheIndex;
	}
	public static int getCacheIndex(String regionIdStr,DupNewsAll dupNewsAll){
		int cacheIndex=0;
		/*String[] regionIds=regionIdStr.substring(1).split("\\|");
		int sum=0;
		for(String regionId:regionIds){
			sum+=Integer.parseInt(regionId);
		}
		cacheIndex=sum%WebPageAnalyzer.aprop.getCacheNum();
		try {
			Files.append(cacheIndex+"O"+sum+"\r\n",new File(WebPageAnalyzer.aprop.getLogLocaltion()+"/cacheDis.record"), Charsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		if(regionIdStr==null){
			System.err.println("regionIdStrΪ�գ�"+dupNewsAll.getId());
			return 0;
		}
		cacheIndex=Math.abs(Hashing.md5().hashString(regionIdStr, Charsets.UTF_8).asInt())%WebPageAnalyzer.aprop.getCacheNum();
		if(WebPageAnalyzer.aprop.isTestTime())
			try {
				Files.append(cacheIndex+"O"+regionIdStr+"\r\n",new File(WebPageAnalyzer.aprop.getLogLocaltion()+"/cacheDis.record"), Charsets.UTF_8);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return cacheIndex;
	}
	
	/**
	 * ��������,regionIds��������ȡ����ȥ�ػ�����
	 * @param date
	 * @return
	 */
	public static DupNewsCache getInstance(Date date,List<Integer> regionIds){
		DupNewsCache[] caches=instanceMap.get(WebPageAnalyzer.dateFormat.format(date));
		if (caches==null)
			return null;
		DupNewsCache instance=caches[getCacheIndex(regionIds)];
//		if(instance == null){
//			instance = addCache(date);
//		}
		return instance;
	}
	public static DupNewsCache getInstance(Date date,String regionIds){
		DupNewsCache[] caches=instanceMap.get(WebPageAnalyzer.dateFormat.format(date));
		if (caches==null)
			return null;
		DupNewsCache instance=caches[getCacheIndex(regionIds)];
//		if(instance == null){
//			instance = addCache(date);
//		}
		return instance;
	}
	public static DupNewsCache getInstance(Date date,String regionIds,int eventId){
		DupNewsCache[] caches=instanceMap.get(WebPageAnalyzer.dateFormat.format(date));
		if (caches==null)
			return null;
		
		int cacheIndex=getCacheIndex(eventId+regionIds);
		
//		if(WebPageAnalyzer.aprop.isTestTime()){
//			Long disCount=cacheDisMap.get(WebPageAnalyzer.dateFormat.format(date))[cacheIndex];
//			synchronized (disCount) {
//				disCount++;
//			}		
//		}
		
		DupNewsCache instance=caches[cacheIndex];
//		if(instance == null){
//			instance = addCache(date);
//		}
		return instance;
	}
	
	/*public static HashMap<String, DupNewsAll> getDupNewsTitleMapofDate(Date date){
		return instanceTitleMap.get(WebPageAnalyzer.dateFormat.format(date));
	}
	public static HashMap<String, DupNewsAll> getDupNewsTextMapofDate(Date date){
		return instanceTextMap.get(WebPageAnalyzer.dateFormat.format(date));
	}*/
	
	public static DupNewsCache[] addCache(Date date){
		if(!instanceMap.containsKey(WebPageAnalyzer.dateFormat.format(date))){
			DupNewsCache[] caches=new DupNewsCache[WebPageAnalyzer.aprop.getCacheNum()];
//			ArrayList<DupNewsAll> mainNews=WebPageAnalyzer.da.getAllForJiangXi(date);
			ArrayList<DupNewsAll> mainNews=WebPageAnalyzer.da.getAllMainInfoForJiangXi(date);
//			Long[] cacheDis=new Long[WebPageAnalyzer.aprop.getCacheNum()];
			Date now=new Date();
			for(int i=0;i<caches.length;i++){
				caches[i]=new DupNewsCache();
				caches[i].cache=new ArrayList<DupNewsAll>();
				caches[i].lastVisit=now;
				
//				cacheDis[i]=new Long(0);
			}
			if(AnalysisProperties.getInstance().getCacheHash()==1){
				for(DupNewsAll dupNewsAll:mainNews){
					int cacheIndex=getCacheIndex(dupNewsAll.getEventId()+dupNewsAll.getRegionIds(),dupNewsAll);
//					caches[cacheIndex].cache.add(dupNewsAll);
					caches[cacheIndex].titleMap.put(dupNewsAll.strWithTitle(), dupNewsAll);
					caches[cacheIndex].textMap.put(dupNewsAll.strWithText(), dupNewsAll);
//					if(WebPageAnalyzer.aprop.isTestTime())
//						cacheDis[cacheIndex]++;
				}
			}else{
				for(DupNewsAll dupNewsAll:mainNews){
					int cacheIndex=getCacheIndex(dupNewsAll.getEventId()+dupNewsAll.getRegionIds(),dupNewsAll);
					caches[cacheIndex].cache.add(dupNewsAll);
				}
			}
			
			instanceMap.put(WebPageAnalyzer.dateFormat.format(date), caches);
//			if(WebPageAnalyzer.aprop.isTestTime())
//				cacheDisMap.put(WebPageAnalyzer.dateFormat.format(date), cacheDis);	
		}
		return instanceMap.get(WebPageAnalyzer.dateFormat.format(date));
	}
	
	public static boolean removeCache(Date date){
		if(instanceMap.containsKey(WebPageAnalyzer.dateFormat.format(date))){
			instanceMap.remove(WebPageAnalyzer.dateFormat.format(date));
			
//			cacheDisMap.remove(WebPageAnalyzer.dateFormat.format(date));
			
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
