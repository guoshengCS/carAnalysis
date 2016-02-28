package javabean;

import java.util.ArrayList;

import javabean.db.Region;

import dataAnalysis.WithReglExpr;

/**
 * �����б�
 * @author hanxin
 */
public class RegionList implements WithReglExpr{
	ArrayList<Region> regions=new ArrayList<Region>();

	public RegionList(ArrayList<Region> regions) {
		this.regions = regions;
	}

	/**
	 *���ɵ�����������ʽ������eventnews����ƥ��
	 *
	 */
	
	public String generateExp() {	
		if (regions.size() > 0) {
			String exp = "(";
			for (Region p : regions) {
				String s=p.getRegionAbbr();
				if (s!=null&&(!s.trim().equals(""))) {
					exp += s + "|";
				}
			}
			exp = exp.substring(0, exp.length() - 1);
			exp += ")";
			return exp;
		} else
			return null;
	}
	
	
	
	public ArrayList<Region> getRegions() {
		return regions;
	}

	public void setRegions(ArrayList<Region> regions) {
		this.regions = regions;
	}
	
	public void addRegion(Region e)
	{
		this.regions.add(e);
	}
}

