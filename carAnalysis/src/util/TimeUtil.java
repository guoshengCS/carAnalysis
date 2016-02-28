package util;

import java.util.*;
import java.text.*;

/**
 * һЩʱ��ת������
 *
 */
public class TimeUtil {
	public static DecimalFormat decimalFormat = new DecimalFormat("#0.000");
	public static String statTimeBySecond(Date start,Date end){
		DecimalFormat df = new DecimalFormat("#0.000");
		return df.format((double)(end.getTime()-start.getTime())/1000);
	}
	
	/** 
	* �ַ���ת�������� 
	* @param str 
	* @return date 
	*/  
	public static Date StrToDate(String str) {  
	   SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");  
	   Date date = null;  
	    try {
			date = format.parse(str);
		} catch (java.text.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	   return date;  
	}  
	
	/** 
	* ����ת�����ַ���
	* @param date 
	* @return str 
	*/  
	public static String DateToStr(Date date) {  
	   SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");  
	   String str = null;  
	   str = format.format(date);  
	   return str;  
	}  
	
	public static String getDate() {
		java.sql.Date date = new java.sql.Date(System.currentTimeMillis());
		return date.toString();
	}

	public static void getPreciseCurrentTime() {
		long startTime =System.currentTimeMillis();  // ��ȡ��ʼʱ��
		long endTime = System.currentTimeMillis(); // ��ȡ����ʱ��
		System.out.println("��������ʱ�䣺 " + (endTime - startTime) + "ms");
		/*
		 * long startTime=System.currentTimeMillis(); //��ȡ��ʼʱ�� ����doSomeThing();
		 * //���ԵĴ���� ����long endTime=System.currentTimeMillis(); //��ȡ����ʱ��
		 */
	}
	
	/*public static long getConsumeTime(Object oj){
		long startTime =System.currentTimeMillis(); 
		Object o=oj;
		long endTime = System.currentTimeMillis(); // ��ȡ����ʱ��
		long between =endTime-startTime;
		return between;
	}*/

	@SuppressWarnings("unused")
	public static String getTime() {
		Date now = new Date();

		Calendar cal = Calendar.getInstance();

		DateFormat d1 = DateFormat.getDateInstance(); // Ĭ�����ԣ�����µ�Ĭ�Ϸ��MEDIUM��񣬱��磺2008-6-16
														// 20:54:53��
		String str1 = d1.format(now);
		DateFormat d2 = DateFormat.getDateTimeInstance();
		String str2 = d2.format(now);
		DateFormat d3 = DateFormat.getTimeInstance();
		String str3 = d3.format(now);
		DateFormat d4 = DateFormat.getInstance(); // ʹ��SHORT�����ʾ���ں�ʱ��
		String str4 = d4.format(now);
		DateFormat d5 = DateFormat.getDateTimeInstance(DateFormat.FULL,
				DateFormat.FULL); // ��ʾ���ڣ��ܣ�ʱ�䣨��ȷ���룩
		String str5 = d5.format(now);
		DateFormat d6 = DateFormat.getDateTimeInstance(DateFormat.LONG,
				DateFormat.LONG); // ��ʾ���ڡ�ʱ�䣨��ȷ���룩
		String str6 = d6.format(now);
		DateFormat d7 = DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT); // ��ʾ���ڣ�ʱ�䣨��ȷ���֣�
		String str7 = d7.format(now);
		DateFormat d8 = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
				DateFormat.MEDIUM); // ��ʾ���ڣ�ʱ�䣨��ȷ���֣�
		String str8 = d8.format(now);// ��SHORT�����ȣ����ַ�ʽ�����
		/*
		 * System.out.println("��Date��ʽ��ʾʱ��: " +
		 * now);//�˷�����ʾ�Ľ����Calendar.getInstance().getTime()һ��
		 * 
		 * 
		 * System.out.println("��DateFormat.getDateInstance()��ʽ��ʱ���Ϊ��" + str1);
		 * System.out.println("��DateFormat.getDateTimeInstance()��ʽ��ʱ���Ϊ��" +
		 * str2); System.out.println("��DateFormat.getTimeInstance()��ʽ��ʱ���Ϊ��" +
		 * str3); System.out.println("��DateFormat.getInstance()��ʽ��ʱ���Ϊ��" +
		 * str4);
		 * 
		 * System.out.println(
		 * "��DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.FULL)��ʽ��ʱ���Ϊ��"
		 * + str5);System.out.println(
		 * "��DateFormat.getDateTimeInstance(DateFormat.LONG,DateFormat.LONG)��ʽ��ʱ���Ϊ��"
		 * + str6);System.out.println(
		 * "��DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT)��ʽ��ʱ���Ϊ��"
		 * + str7);System.out.println(
		 * "��DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM)��ʽ��ʱ���Ϊ��"
		 * + str8);
		 */
		return str2;
	}

	/**
	 * ����YYYY-MM-DD�������ַ�������,��������������������
	 * @param start
	 * @param end
	 * @return
	 * @throws ParseException
	 */
	public long getDistDates(String start,String end) throws ParseException  
	{
	      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); 
	      Date startDate = sdf.parse(start);   
	      Date endDate = sdf.parse(end);
	      return getDistDates(startDate,endDate);
	}  
	
	/**
	 * ��������������������
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws ParseException
	 */
	public long getDistDates(Date startDate,Date endDate)  
	{
		long totalDate = 0;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		long timestart = calendar.getTimeInMillis();
		calendar.setTime(endDate);
		long timeend = calendar.getTimeInMillis();
		totalDate = Math.abs((timeend - timestart))/(1000*60*60*24);
		return totalDate;
	} 
	
	/**
	 * ��ȡ��_fromdate���_monthCount���µ�����
	 * @param _fromdate YYYY-MM-DD
	 * @param _monthCount 
	 * @return
	 * @throws ParseException
	 */
	public String getDistMonths(String _fromdate,int _monthCount) throws ParseException
	{  
		String resultDate = "";
		int year,month,date;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(_fromdate));
		year = calendar.get(Calendar.YEAR);
		date = calendar.get(Calendar.DAY_OF_MONTH);
		month = calendar.get(Calendar.MONTH) + 1 + _monthCount;
		int c = new Integer((month-1)/12);
		month = month%12;
		if(month == 0)
		month = 12;
		year += c;
		resultDate = year + "-" + month + "-" + date;
		return resultDate;
	}
	
	/**
	 * ����ÿ���µ������������鷵��
	 * @param months
	 * @param date
	 * @return
	 * @throws ParseException
	 */
	public String[] countMonthDates(int months,Date date) throws ParseException  
	{  
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	    String _date = date == null ? sdf.format(new Date()) : sdf.format(date);//�������������ʱ����ӽ�������  

	    return countMonthDates(months,_date);  
	}	

	/**
	 * ����ÿ���µ������������鷵��
	 * @param months
	 * @param date
	 * @return
	 * @throws ParseException
	 */ 
	public String[] countMonthDates(int months,String date) throws ParseException  
	{  
	    String[] results = null;//���  
	    String _today = date == null ? new SimpleDateFormat("yyyy-MM-dd").format(new Date()) : date;//�������������ʱ����ӽ�������  
	    int _months = months > 0 ? months : 24;//�����������������������δ�����������24��  
	    String startDate = getDistMonths(_today,0);//����������ڵ�YYYY-MM-DD��ʽ���ַ�������  
	    results = new String[_months];
	    for(int i = 1; i <= _months; i++)  
	    {  
	        String nextMonthDate = getDistMonths(_today,i);//ÿ���µĽ�������  
	        String desc = startDate + " �� " + nextMonthDate;  
	        long dates = getDistDates(startDate,nextMonthDate);//��������
	        results[i-1] = desc + " ���� " + dates + " �죡";
	        startDate = nextMonthDate;//��ǰ�µĽ���������Ϊ��һ���µ���ʼ����  
	    }  
	    return results;  
	}

	public static void main(String args[]) {
//		getPreciseCurrentTime();
//		Date date = new Date();
//		String str = DateToStr(date);
//		Date newDate = StrToDate(str);
//		System.out.println(newDate);
		System.out.println(statTimeBySecond(new Date(), new Date(new Date().getTime()+12345L)));
	}
}
