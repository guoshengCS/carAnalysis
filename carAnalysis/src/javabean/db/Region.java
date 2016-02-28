package javabean.db;




/**
 * ����������
 * @author hanxin
 */
public class Region {
	private int regionID;//����ID����������ʡ���С��أ�IDĬ��ʹ����������
	private int provinceID;//��������ʡ�ݵ�ID
	private String regionName;//��������
	private int regionLevel;//��������123�ֱ����ʡ����
	private int parentID;//��һ��������ID�������������Ϊʡ����IDΪ0
	private String regionAbbr;
	
	public Region(int regionID){
		this.regionID=regionID;	
	}
	public Region(int regionID,int provinceID,String regionName,int regionLevel,int parentID,String regionAbbr){
		this.regionID=regionID;	
		this.provinceID=provinceID;
		this.regionName=regionName;
		this.regionLevel=regionLevel;
		this.parentID=parentID;
		this.regionAbbr=regionAbbr;
		
	}
	
	public void setRegionID(int regionID) {
		this.regionID = regionID;
	}
	public int getRegionID() {
		return regionID;
	}
	public void setProvinceID(int provinceID) {
		this.provinceID = provinceID;
	}
	public int getProvinceID() {
		return provinceID;
	}
	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}
	public String getRegionName() {
		return regionName;
	}
	public void setRegionLevel(int regionLevel) {
		this.regionLevel = regionLevel;
	}
	public int getRegionLevel() {
		return regionLevel;
	}
	public void setParentID(int parentID) {
		this.parentID = parentID;
	}
	public int getParentID() {
		return parentID;
	}
	
	

	public void setRegionAbbr(String regionAbbr) {
		this.regionAbbr = regionAbbr;
	}
	public String getRegionAbbr() {
		return regionAbbr;
	}

}
