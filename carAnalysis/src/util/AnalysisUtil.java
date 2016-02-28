package util;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;



import md.base.fs.FileSystem;
import md.base.fs.FileSystemFactory;
import md.base.storage.WebPageStorage;
import javabean.db.KeywordGroup;
import javabean.db.Page;
import javabean.db.Template;

/**
 * ������Ҫ�õ���һЩ���߷���
 * @author Bys
 *
 */
public class AnalysisUtil {

	static AnalysisProperties aprop = AnalysisProperties.getInstance(); 
//	private  String pageRootPath = null;
	public static final SimpleDateFormat downloadSdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//	private static WebPageStorage storage = new WebPageStorage(true);
	private static WebPageStorage storage = new WebPageStorage();
	static{
		try {
			/**
			 * ���ض�ȡ��
			 */
//			FileSystem fs = FileSystemFactory.createLargeScaleLocalFileSystem(aprop.getDocUrl());
			/**
			 * 
			 */
			System.out.println("DocUrl:"+aprop.getDocUrl());
//			FileSystem fs = FileSystemFactory.createWebFileSystem(aprop.getDocUrl(), "key");
//			storage.setFileSystem(fs);
			String baseUrl=aprop.getDocUrl();
			storage.useHttpFileSystem(baseUrl);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
//    private static String fileSystem = aprop.getFileSystem();

	/**
	 * ��ȡpage��Ӧ��content������΢�������page��content�����ļ�ϵͳ��
	 * @param page ��Ҫ��ȡcontent��page
	 * @return ��ȡ��������
	 */
	public String getCoreContent_new(Page page){
		String content = null;
		if(page.getType()==3||page.getType()==6){
			return page.getSummary();
		
		}
		else{
			try {
				System.out.println("pageId:"+page.getId());
				content = storage.get(page.getId(),true);
//				System.out.println(content);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return content;
		}
	}
	
	/**
	 * ��һ�������α�ʾ��ʱ��ת��Ϊ������ʱ�����ʽ���ַ���
	 * @param timeLong �����α�ʾ��ʱ��
	 * @return yyyy-MM-dd HH:mm:ss ��ʽ���ַ���
	 */
	public static String getTimeString(Long timeLong){
		return downloadSdf.format(new Date(timeLong));
	}
	
	/**
	 * ��һ�������α�ʾ��ʱ���ʽ��������
	 * @param timeLong �����α�ʾ��ʱ��
	 * @return ��ʽ�����ʱ��
	 */
	public static long formatTimelong(Long timeLong){
		try {
			return downloadSdf.parse(getTimeString(timeLong)).getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * �ж�ģ����һ���Ե�ģ�廹�ǰ������ݿ��ֶε�ģ�壬type=0��ʾһ���Եģ�type=1��ʾ�и����Ǵ����ݿ��ȡ��
	 * @param tr
	 * @return ��Ҫ�����ݿ��ж�ȡ�ĸ���
	 */
	public KeywordGroup getTemplateType(Template tr){
		 ArrayList<KeywordGroup> groups = tr.getConcepts();
		 for(KeywordGroup group:groups){
			 
			 int type = group.getType();
			 System.out.println(group.getKeywordGroupName()+": "+type);
			 if(type == 1) return group;
		 }
		 
		 return null;	
	}
	
}


