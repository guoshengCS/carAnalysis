package dataAnalysis;




import javabean.MatchedResult;
import javabean.db.Event;
import javabean.db.Page;



/**
 * ����ƥ�����ӿ�
 * @author Bys
 *
 */
public interface PostMatcher {

	
	/**
	 * @author hx
	 * 
	 * @param MatchedReslut
	 * 
	 * @param DownloadDoc
	 * 
	 * @param text
	 * 
	 * @return 1�������ȡ������һ���¼����򷵻�1,��EventMatchStop 0����û�г鵽�������飬NoStop
	 */
	
	/**
	 * 1�������ȡ������һ���¼����򷵻�1,��EventMatchStop 0����û�г鵽�������飬NoStop
	 * @param mr �¼���ƥ����
	 * @param page ��ƥ����ĵ�
	 * @param e
	 * @return
	 * @throws Exception
	 * * @author hx
	 */
	public int postMatcher(MatchedResult mr,Page page,Event e) throws Exception;

	
}
