package dataAnalysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import javabean.DupNewsAll;
import javabean.MatchedResult;
import javabean.db.Event;
import javabean.db.EventNews;
import javabean.db.Page;
import javabean.db.Template;
import snooway.dao.DataDao;
import util.AnalysisProperties;
import util.AnoDupNewsCache;
import util.DupNewsCache;
import util.JiangxiUtil;
import util.TimeUtil;
import whu.nlp.extracter.ExtractRegion;
import whu.nlp.extracter.beans.ResultRegion;
import whu.nlp.extracter.util.MarkRegion;

/**
 * �����¼�ƥ��Ľ�������½��н�һ����ƥ��
 * 
 */
public class JiangXiPostMatcher implements PostMatcher {

	private static String restrictWordList = "��(?!��|֤|��|����|��|��|Ч|�˹�)|û(?!��?����)|δ(?!ȡ��|��|��|��)|���|��|��|��ֹ|Ԥ��|�ٱ�|���|(?<!��)��(?!��|��|��|��(?!��)|��|��|��|ȫ|��ʵ|��|��|��|��|��|��|��|��|��|��|��|֪|��|��|��Ϊ|��|��|��|��|��|��|ʵ|����|�߱�|��|֧��|����|���|��)|�ϴ�|�ϲ�|�����鴦|�Ͻ�|����|����|��|[�ž�]��|����|����|Ԥ��|����|��[��С��]|����|��ֹ|����|��|���|ȡ��|����|����|�Ʋ�|����|����|[����]��|��̭|���|�鴦|�ƻ�|ץ��|����|������|(?!��)����|����|���|����|�ϴ�|�Ų�|��?��";
	private static String restricsInEvent="��|��|��|��|@|#|��Դ��|\n";
	private AnalysisProperties aprops = AnalysisProperties.getInstance();

	private DataDao da = WebPageAnalyzer.da;

	private void updateSeed(DupNewsAll seed, EventNews evn, Integer provinceId,
			List<Integer> regionList, Page page) {
		int eId = evn.getEventId();
		int templateId = evn.getTemplateID();
		
		Date block_start = new Date();
		
		synchronized (seed) {
			
			Date block_end = new Date();
			page.blockT+=block_end.getTime()-block_start.getTime();
			
			String id = "-1";
			seed.setDupCount(seed.getDupCount() + 1);
			if (seed.getMainInfoDate().before(page.getPublishDate())) {
				id = da.addEventNews(evn, seed.getDupCount(), seed.getId(),
						true, seed.getMainInfoId(), provinceId, page);

				if (!id.equals("-1")) {
					// da.addEventNews2(id,
					// evn,seed.getDupCount(),seed.getId(),true,seed.getMainInfoId(),provinceId);
					seed.setMainInfoDate(evn.getPreciseDate());
					seed.setMainInfoId(id);
					for (int j = 0; j < regionList.size(); j++) {
						Integer regionId = regionList.get(j);
						da.addEventNewsRegionTable(
								id, "region", regionId, provinceId, eId,
								templateId);
						// if(!eventnews_regionId.equals("-1"))
						// da.addEventNewsTable2(eventnews_regionId, id,
						// "region",regionId,provinceId,eId, templateId);
						System.out.println(page.getId()
								+ " insert EventNews_all_Region: " + regionId);
					}
					// for(Integer regionId:province2Region.get(provinceId)){
					// da.addEventNewsTable(id, "region",regionId);
					// System.out.println(page.getId()+" insert EventNews_all_Region: "+
					// regionId);
					// }
				}
			} else {
				id = da.addEventNews(evn, seed.getDupCount(), seed.getId(),
						false, seed.getMainInfoId(), provinceId, page);
				if (!id.equals("-1")) {
					// da.addEventNews2(id,
					// evn,seed.getDupCount(),seed.getId(),false,seed.getMainInfoId(),provinceId);

					for (int j = 0; j < regionList.size(); j++) {
						Integer regionId = regionList.get(j);
						da.addEventNewsRegionTable(id, "region", regionId,
								provinceId, eId, templateId);
						// if(!eventnews_regionId.equals("-1"))
						// da.addEventNewsTable2(eventnews_regionId, id,
						// "region",regionId,provinceId,eId, templateId);
						System.out.println(page.getId()
								+ " insert EventNews_all_Region: " + regionId);
					}
					// for(Integer regionId:province2Region.get(provinceId)){
					// da.addEventNewsTable(id, "region",regionId);
					// System.out.println(page.getId()+" insert EventNews_all_Region: "+
					// regionId);
					// }
				}
			}
		}
	}

	private void createSeed(DupNewsAll newNewsAll, EventNews evn,
			Integer provinceId, List<Integer> regionList, Page page) {
		System.out.println(page.getId()+" addEventNews start");
		String id = da.addEventNews(evn, provinceId, page);
		System.out.println(page.getId()+" addEventNews end");
		if (!id.equals("-1")) {
			// da.addEventNews2(id, evn, provinceId);
			newNewsAll.setId(id);
			newNewsAll.setMainInfoId(id);
			newNewsAll.setMainInfoDate(page.getPublishDate());
			newNewsAll.setDupCount(1);
			newNewsAll.setProvinceId(provinceId);
			evn.setId(id);
			for (int j = 0; j < regionList.size(); j++) {
				Integer regionId = regionList.get(j);
				da.addEventNewsRegionTable(id, "region", regionId, provinceId,
						evn.getEventId(), evn.getTemplateID());
				// da.addEventNewsTable2(eventnews_regionId, id,
				// "region",regionId,provinceId,eId, templateId);
				System.out.println(page.getId()
						+ " insert EventNews_all_Region: " + regionId);
			}
			// for(Integer regionId:province2Region.get(provinceId)){
			// da.addEventNewsTable(id, "region",regionId);
			// System.out.println(page.getId()+" insert EventNews_all_Region: "+
			// regionId);
			// }
		}
	}

	public int postMatcher(MatchedResult mr, Page page, Event e)
			throws Exception {
		String text = page.getContent();
		Template t = mr.getT();
		int start = mr.getStart();
		int end = mr.getEnd();
		// String[] group = mr.getGroup();
		int first = Math.max(0, start - 30);
		int last = Math.min(text.length(), end + 30);
		String coretext = text.substring(start, end); // ƥ�䵽���¼�
		Pattern corep=PatternAgent.getPattern(restricsInEvent);
		Matcher corem=corep.matcher(coretext);
		if(corem.find()){
			System.out.println("��Χ�����޶��ʣ����ˣ�����");
			return WebPageAnalyzer.TemMatchStop;
		}
		int resfirst = Math.max(0, start - 6); // �ж��Ƿ�����޶��ʣ�ȡ�¼�ǰ�����4����
		int reslast = Math.min(text.length(), end + 4);
		String restext = text.substring(resfirst, reslast);
		// String restext = text.substring(resfirst, start)+text.substring(end,
		// reslast);
		Pattern resp = PatternAgent.getPattern(restrictWordList);
		Matcher resm = resp.matcher(restext);
		if (resm.find()) {
			// ���ƥ�䵽���޶��ʣ��򷵻�
			System.out.println("��Χ�����޶��ʣ����ˣ�����");
			return WebPageAnalyzer.TemMatchStop;
		}

		// String
		// alltext="�Ʒ�������J�����ң�J����ж������ˣ����˱�J�����ߡ���ҽ�����ף����������м�������ǰ�������ģJ���ͻ�¼�����ǧ���ں�JC";
		// ���
		StringBuffer sb = new StringBuffer();
		sb.append(text.substring(first, start));

		sb.append("<span class=\"eventlabel\">" + coretext + "</span>");
		sb.append(text.substring(end, last));
		String eContent = sb.toString();

		System.out.println(page.getId() + "     templateId:"
				+ "----------------" + t);
		int eId = e.getEventID();
		int templateId = t.getTemplateId();

		int polarity = -1;
		// ����Ƿ��쵼ר��,���м����ж�
		// if (e.getStatus() == 0) {
		// if (group.length >= 2) {
		// String middle=null;
		// Pattern np=null;
		// Matcher nm =null;
		// for (int j = 1; j <= group.length; j += 2) {
		// middle=null;np=null;nm=null;
		// if(group[j - 1]!=null){
		// middle = group[j - 1];
		// np= PatternAgent.getPattern(negtiveWordList);
		// nm= np.matcher(middle);
		// if (nm.find()) {
		// // ��һ��return�ĵط������ƥ�䵽���¼�������ģ��򷵻�
		// return WebPageAnalyzer.NoStop;
		// }
		// }
		// }
		// }
		// }
		// // �쵼����ȡ
		// else if (e.getStatus() == 1) {
		// for (int j = 0; j < group.length; j += 1) {
		// if(group[j]!=null){
		// Pattern p = PatternAgent.getPattern(AnalysisList.pl);
		// if(p==null) return WebPageAnalyzer.NoStop;
		// Matcher m = p.matcher(group[j]);
		// if (m.find()) {
		// System.out.println("ƥ�䵽�쵼����" + m.group());
		// if (dupPersonList.contains(m.group()))
		// break;
		// dupPersonList.add(m.group());
		// personName = m.group();
		// }
		// }
		// }
		// }
		//
		// // ʱ���ȡ
		// Date preciseDate = ModifyTime.getDate(page, text, coretext);
		// System.out.println("ʱ���ȡ��ɣ������е�����ȡ");
		// //������ȡ
		JiangxiUtil ju = new JiangxiUtil();

		List<Integer> regionList = new ArrayList<Integer>();

		// List<City> result = extractCT.extractALL(alltext);

		Date regionDoc_start = new Date();
		System.out.println(page.getId()+" ExtractRegion start");
		List<ResultRegion> result = ExtractRegion.extract(eContent);
		System.out.println(page.getId()+" ExtractRegion end");
		Date regionDoc_end = new Date();
		if (WebPageAnalyzer.aprop.isTestTime())
			page.regionTimes
					.add(TimeUtil.decimalFormat.format(((double) (regionDoc_end
							.getTime() - regionDoc_start.getTime())) / 1000));
		// Files.append(TimeUtil.statTimeBySecond(regionDoc_start,
		// regionDoc_end)+"\r\n", new
		// File(WebPageAnalyzer.aprop.getLogLocaltion()+"/regionDocTime.record"),
		// Charsets.UTF_8);

		if (result.isEmpty()) {
			System.out.println("����Ϊ�գ���");
		}
		if (!result.isEmpty()) {
			// if(result.isEmpty()){System.out.println("������Ϊ�գ���");}
			// for(City city:result){
			for (ResultRegion city : result) {
				/*
				 * // regionName = city.getName(); //
				 * System.out.println(regionName); // if(regionName.length()>2){
				 * //
				 * if(regionName.endsWith("ʡ")||regionName.endsWith("��")||regionName
				 * .endsWith("��")) // { // regionName =
				 * regionName.replaceAll("ʡ|��|��", ""); // } // } // regionId =
				 * ju.toID(regionName); if
				 * ((!regionList.contains(regionId)&&ju.isRegionOfProvince
				 * (regionName,
				 * aprops.getProvinceId())&&(!ju.monitorRule(alltext,
				 * regionName)))) {
				 * 
				 * // regionList.add(regionName);
				 * regionNameList.add(regionName); if(regionId!=-1)
				 * regionList.add(regionId);
				 */
				// ����
				for (int cityId : city.getRegion_ID()) {
					if (aprops.getProvinceId() == -1
							|| ("" + cityId).startsWith(""
									+ aprops.getProvinceId())) {
						if (!regionList.contains(cityId))
							regionList.add(cityId);
					}
				}

				// }
			}
			// String abbr = alltext.substring(city.getStart(), city.getEnd());
			// System.out.println(abbr);
			// eContent = eContent.replaceAll(abbr,
			// "<span class=\"bg\">" + abbr + "</span>");
			System.out.println(page.getId()+" MarkRegion start");
			eContent = MarkRegion.mark(eContent, result);
			System.out.println(page.getId()+" MarkRegion end");
		}

		System.out.println(page.getId()+" ������ȡ���");
		result.clear();
		result = null;

		HashMap<Integer, ArrayList<Integer>> province2Region = new HashMap<Integer, ArrayList<Integer>>();
		for (int j = 0; j < regionList.size(); j++) {
			int regionId = regionList.get(j);
			int provinceId = Integer.parseInt(("" + regionId).substring(0, 2));
			if (province2Region.get(provinceId) == null) {
				ArrayList<Integer> regions = new ArrayList<Integer>();
				regions.add(regionId);
				province2Region.put(provinceId, regions);
			} else {
				province2Region.get(provinceId).add(regionId);
			}
		}
		if (e.getStatus() == 0 && regionList.size() > 0) {
			System.out.println(page.getId()+" һ���¼�");
			if (aprops.isAll_dup()) {
				System.out.println(page.getId()+ " isAll_dup");
				EventNews evn = new EventNews(page.getTitle(), eContent,
						page.getWebSite(), eId, templateId, page.getUrl(),
						page.getPublishDate(), polarity, page.getType(),
						page.getSitePriority(), page.getNewsLevel());
				evn.setPageId(page.getId());
				evn.setWebSiteplate(page.getWebSiteplate());
				System.out.println(page.getId()+ " EventNews have newed");
				if (page.getPublishDate() != null) {
					System.out.println(page.getId()+ " publishDate not null");
					List<Integer> maxLevelRegions=ju.getMaxLevelRegions(regionList);
					Collections.sort(maxLevelRegions);
					String regionIdsStr="";
					for(int id:maxLevelRegions){
						regionIdsStr+="|"+id;
					}
					DupNewsCache cache = DupNewsCache.getInstance(page
							.getPublishDate(),regionIdsStr,eId);
//					DupNewsCache cache = DupNewsCache.getInstance(page
//							.getPublishDate(),ju.getMaxLevelRegions(regionList));
//					DupNewsCache cache = DupNewsCache.getInstance(page
//							.getPublishDate());
					DupNewsAll seed = null;
					HashMap<String, DupNewsAll> dupNewsTitleMap=null;
					HashMap<String, DupNewsAll> dupNewsTextMap=null;
					
					if(aprops.getCacheHash()==1){
						if(cache!=null){
							dupNewsTitleMap=cache.titleMap;
							dupNewsTextMap=cache.textMap;
							for (Integer provinceId : province2Region.keySet()) {
								DupNewsAll newNewsAll = new DupNewsAll("-1",page.getTitle(),ju.getCoretext(eContent),regionIdsStr, provinceId,e.getEventID());
								synchronized (dupNewsTitleMap) {
									if(dupNewsTitleMap.containsKey(newNewsAll.strWithTitle())){
										seed=dupNewsTitleMap.get(newNewsAll.strWithTitle());
									}else{
										synchronized (dupNewsTextMap) {
											if(dupNewsTextMap.containsKey(newNewsAll.strWithText())){
												seed=dupNewsTextMap.get(newNewsAll.strWithText());
											}else {
												createSeed(newNewsAll, evn, provinceId,regionList, page);
												dupNewsTextMap.put(newNewsAll.strWithText(), newNewsAll);
												dupNewsTitleMap.put(newNewsAll.strWithTitle(), newNewsAll);
											}
										}
									}
								}
								if(seed!=null){
									updateSeed(seed, evn, provinceId, regionList,page);
								}
								if (aprops.isOld()) {
									Integer oldProSubCount = null;
									if ((oldProSubCount = WebPageAnalyzer.tempProSubMap
											.get(provinceId)) != null)
										WebPageAnalyzer.tempProSubMap.put(provinceId,
												oldProSubCount + 1);
								}
							}
							return WebPageAnalyzer.EventMatchStop;
						}

						

					}else{
					
						if (cache != null) {
							System.out.println(page.getId()+ " cache not null");
//							Hashing.md5().hashString("4123|235", Charsets.UTF_8).asInt()%m;
							
							

//							if (WebPageAnalyzer.aprop.isTestTime())
//								page.blockTime = TimeUtil.decimalFormat
//										.format(((double) (block_end
//												.getTime() - block_start
//												.getTime())) / 1000);
							for (Integer provinceId : province2Region.keySet()) {
								// ��cache
								Date block_start = new Date();
								
								synchronized (cache) {

									Date block_end = new Date();
									page.blockT+=block_end.getTime()-block_start.getTime();
									
									//============================================
									//Ѱ��cache���Ƿ����ظ�
//									DupNewsAll newNewsAll = new DupNewsAll("-1",
//											page.getTitle(),
//											ju.getCoretext(eContent),
//											ju.getMaxLevelRegions(regionList), provinceId);
									DupNewsAll newNewsAll = new DupNewsAll("-1",
											page.getTitle(),
											ju.getCoretext(eContent),
											regionIdsStr, provinceId,e.getEventID());

									Date chkDup_start = new Date();

									seed = cache.checkDup(newNewsAll);
									//============================================
									Date chkDup_end = new Date();
									if (WebPageAnalyzer.aprop.isTestTime())
										page.chkDupTimes.add(TimeUtil.decimalFormat
												.format(((double) (chkDup_end
														.getTime() - chkDup_start
														.getTime())) / 1000));
									// Files.append(TimeUtil.statTimeBySecond(chkDup_start,
									// chkDup_end)+"\r\n", new
									// File(WebPageAnalyzer.aprop.getLogLocaltion()+"/chkDupTime.record"),
									// Charsets.UTF_8);
									
									//���û���ظ��ģ��򴴽�һ��seed�������cache
									if (seed == null) {
										System.out.println(page.getId()+ " createSeed start");
										createSeed(newNewsAll, evn, provinceId,regionList, page);
										System.out.println(page.getId()+ " createSeed end");
										System.out.println(page.getId()+ " insert EventNewsALL: "+ eContent);
										cache.add(newNewsAll);
									}
									// return WebPageAnalyzer.EventMatchStop;
								}
								//����cache
								//���seed��Ϊ�գ������ظ��ģ�����seed�����·�����ͨ����seedͬ��
								if (seed != null) {
									System.out.println(page.getId()+ " updateSeed start");
									updateSeed(seed, evn, provinceId, regionList,page);
									System.out.println(page.getId()+ " updateSeed end");
								}
								
								if (WebPageAnalyzer.aprop.isOld()) {
									Integer oldProSubCount = null;
									if ((oldProSubCount = WebPageAnalyzer.tempProSubMap
											.get(provinceId)) != null)
										WebPageAnalyzer.tempProSubMap.put(provinceId,
												oldProSubCount + 1);
								}
								
							}
							return WebPageAnalyzer.EventMatchStop;
						}
					}
					
					
					
					
					// if(WebPageAnalyzer.aprop.getAppNum()!=1){
					// AnoDupNewsCache
					// anoCache=AnoDupNewsCache.getInstance(page.getPublishDate());
					// if(anoCache!=null){
					// synchronized (anoCache){
					// for(Integer provinceId:province2Region.keySet()){
					// DupNewsAll newNewsAll=new
					// DupNewsAll("-1",page.getTitle(),ju.getCoretext(eContent),ju.get(regionList),provinceId);
					// DupNewsAll seed = anoCache.checkDup(newNewsAll);
					// if(seed!=null){
					// String id = "-1";
					// seed.setDupCount(seed.getDupCount()+1);
					// if(seed.getMainInfoDate().before(page.getPublishDate())){
					// id
					// =da.addEventNews2(null,evn,seed.getDupCount(),seed.getId(),true,seed.getMainInfoId(),provinceId);
					// Integer oldProSubCount=null;
					// if((oldProSubCount=WebPageAnalyzer.tempProSubMap.get(provinceId))!=null)
					// WebPageAnalyzer.tempProSubMap.put(provinceId,oldProSubCount+1);
					// if(!id.equals("-1")){
					// // da.addEventNews2(id,
					// evn,seed.getDupCount(),seed.getId(),true,seed.getMainInfoId(),provinceId);
					// seed.setMainInfoDate(evn.getPreciseDate());
					// seed.setMainInfoId(id);
					// for(int j=0;j<regionList.size();j++){
					// Integer regionId=regionList.get(j);
					// da.addEventNewsTable2(null,id,
					// "region",regionId,provinceId,eId, templateId);
					// //
					// System.out.println(page.getId()+" insert EventNews_all_Region: "+
					// regionId);
					// }
					// // for(Integer regionId:province2Region.get(provinceId)){
					// // da.addEventNewsTable(id, "region",regionId);
					// //
					// System.out.println(page.getId()+" insert EventNews_all_Region: "+
					// regionId);
					// // }
					// }
					// }else{
					// id =
					// da.addEventNews2(null,evn,seed.getDupCount(),seed.getId(),false,seed.getMainInfoId(),provinceId);
					// Integer oldProSubCount=null;
					// if((oldProSubCount=WebPageAnalyzer.tempProSubMap.get(provinceId))!=null)
					// WebPageAnalyzer.tempProSubMap.put(provinceId,oldProSubCount+1);
					//
					// if(!id.equals("-1")){
					// // da.addEventNews2(id,
					// evn,seed.getDupCount(),seed.getId(),false,seed.getMainInfoId(),provinceId);
					//
					// for(int j=0;j<regionList.size();j++){
					// Integer regionId=regionList.get(j);
					// da.addEventNewsTable2(null,id,
					// "region",regionId,provinceId,eId, templateId);
					// //
					// System.out.println(page.getId()+" insert EventNews_all_Region: "+
					// regionId);
					// }
					// // for(Integer regionId:province2Region.get(provinceId)){
					// // da.addEventNewsTable(id, "region",regionId);
					// //
					// System.out.println(page.getId()+" insert EventNews_all_Region: "+
					// regionId);
					// // }
					// }
					// }
					// }else{
					// String id = da.addEventNews2(null,evn,provinceId);
					// Integer oldProSubCount=null;
					// if((oldProSubCount=WebPageAnalyzer.tempProSubMap.get(provinceId))!=null)
					// WebPageAnalyzer.tempProSubMap.put(provinceId,oldProSubCount+1);
					// if (!id.equals("-1")){
					// // da.addEventNews2(id, evn, provinceId);
					// newNewsAll.setId(id);
					// newNewsAll.setMainInfoId(id);
					// newNewsAll.setMainInfoDate(page.getPublishDate());
					// newNewsAll.setDupCount(1);
					// newNewsAll.setProvinceId(provinceId);
					// anoCache.add(newNewsAll);
					// System.out.println(page.getId()+" insert EventNewsALL: "
					// + eContent);
					// // evn.setId(id);
					// for(int j=0;j<regionList.size();j++){
					// Integer regionId=regionList.get(j);
					// da.addEventNewsTable2(null,id,
					// "region",regionId,provinceId,eId, templateId);
					// // da.addEventNewsTable2(eventnews_regionId, id,
					// "region",regionId,provinceId,eId, templateId);
					// //
					// System.out.println(page.getId()+" insert EventNews_all_Region: "+
					// regionId);
					// }
					// // for(Integer regionId:province2Region.get(provinceId)){
					// // da.addEventNewsTable(id, "region",regionId);
					// //
					// System.out.println(page.getId()+" insert EventNews_all_Region: "+
					// regionId);
					// // }
					// }
					// }
					// }
					// }
					// }
					// }

//					return WebPageAnalyzer.EventMatchStop;
				}
				System.out.println(page.getId()+ " cache|publishDate is null");
				for (Integer provinceId : province2Region.keySet()) {
					String id = da.addEventNews(evn, provinceId, page);
					if (!id.equals("-1")) {
						// da.addEventNews2(id, evn, provinceId);
						for (int j = 0; j < regionList.size(); j++) {
							Integer regionId = regionList.get(j);
							String eventnews_regionId = da
									.addEventNewsRegionTable(id, "region",
											regionId, provinceId, eId,
											templateId);
							// da.addEventNewsTable2(eventnews_regionId, id,
							// "region",regionId,provinceId,eId, templateId);
							System.out.println(page.getId()
									+ " insert EventNews_all_Region: "
									+ regionId);
						}
						// for(Integer
						// regionId:province2Region.get(provinceId)){
						// da.addEventNewsTable(id, "region",regionId);
						// System.out.println(page.getId()+" insert EventNews_all_Region: "+
						// regionId);
						// }
					}
				}
				/*if (WebPageAnalyzer.aprop.getAppNum() != 1) {
					for (Integer provinceId : province2Region.keySet()) {
						String id = da.addEventNews2(null, evn, provinceId);
						Integer oldProSubCount = null;
						if ((oldProSubCount = WebPageAnalyzer.tempProSubMap
								.get(provinceId)) != null)
							WebPageAnalyzer.tempProSubMap.put(provinceId,
									oldProSubCount + 1);
						if (!id.equals("-1")) {
							// da.addEventNews2(id, evn, provinceId);
							for (int j = 0; j < regionList.size(); j++) {
								Integer regionId = regionList.get(j);
								da.addEventNewsTable2(null, id, "region",
										regionId, provinceId, eId, templateId);
								// da.addEventNewsTable2(eventnews_regionId, id,
								// "region",regionId,provinceId,eId,
								// templateId);
								// System.out.println(page.getId()+" insert EventNews_all_Region: "+
								// regionId);
							}
							// for(Integer
							// regionId:province2Region.get(provinceId)){
							// da.addEventNewsTable(id, "region",regionId);
							// System.out.println(page.getId()+" insert EventNews_all_Region: "+
							// regionId);
							// }
						}
					}
				}*/

				return WebPageAnalyzer.EventMatchStop;
			}
		}
		regionList.clear();
		regionList = null;
		return WebPageAnalyzer.NoStop;
	}
}