package javabean;



/**
 * ĳ����������Ӧ�Ĵ�
 * @author hx
 * 
 */
public class Keyword {
	private int keywordId;    //����Ǹ����Ǵ����ݿ��ж�ȡ�ģ���keywordId�����ݿ�����Ӧ��id

	private String keywordName;//����Ǳ��嶨����ֱ���Ǹ���������������ݿⶨ�壬name = ����.�ֶ����������
	
	private String tags;
	private String tableName;              //���嶨�������ݿ⣬�ʹ����ݿ�ı�����Ϊ�˺��ڷ����д���ݿ�

	
	public Keyword(int keywordId, String keywordName, String tableName) {
		super();
		this.keywordId = keywordId;
		this.keywordName = keywordName;
		this.tableName = tableName;
	}
	
	
	public int getKeywordId() {
		return keywordId;
	}
	public void setKeywordId(int keywordId) {
		this.keywordId = keywordId;
	}
	public String getKeywordName() {
		return keywordName;
	}
	public void setKeywordName(String keywordName) {
		this.keywordName = keywordName;
	}
	

	public void setTags(String tags) {
		this.tags = tags;
	}


	public String getTags() {
		return tags;
	}


	public String getTableName() {
		return tableName;
	}


	public void setTableName(String tableName) {
		this.tableName = tableName;
	}


/*	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return keywordName;
	}*/



	
	

}
