package javabean;

import java.io.Serializable;
import java.util.Vector;

import util.AnalysisUtil;


/**
 * ������ȡ���µĻ�����Ԫ������������Χ����ֹid�ʹ˷�Χ��δ����������id
 * @author Bys
 *
 */
public class PagesUnit implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -9158343477498981489L;
	private long start;
	private long end;
	private Vector<String> unanalysis;
	
	public PagesUnit(long start,long end){
		start = AnalysisUtil.formatTimelong(start);
		end = AnalysisUtil.formatTimelong(end);
		
		if(start <= end){
			this.start = start;
			this.end = end;
		}
		else{
			this.start = end;
			this.end = start;
		}
		unanalysis = new Vector<String>();
	}
	
	public long getStart() {
		return start;
	}
	public void setStart(long start) {
		this.start = start;
	}
	
	public long getEnd() {
		return end;
	}
	public void setEnd(long end) {
		this.end = end;
	}
	
	public void addUnanalysis(String id){
		if(!unanalysis.contains(id))
			unanalysis.add(id);
		else{
			System.err.println(id+"("+id+") between ("+AnalysisUtil.getTimeString(start)+","+AnalysisUtil.getTimeString(end)+")�Ѿ����ڣ�");
			System.err.println("���ڷ�Χ"+AnalysisUtil.getTimeString(start)+"-"+AnalysisUtil.getTimeString(end));
		}
	}
	
	public void addUnanalysis(Vector<String> unanalysis){
		for(String id:unanalysis){
//		if(!this.unanalysis.contains(id))
			this.unanalysis.add(id);
		}
	}
	
	public Vector<String> getUnanalysis(){
		return unanalysis;
	}

	
	
//	public void setUnanalysis(List<Integer> unanalysis){
//		this.unanalysis =  Collections.synchronizedList(unanalysis);
//	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
//		return unanalysis.toString();
		return AnalysisUtil.getTimeString(start)+"-"+AnalysisUtil.getTimeString(end)+" "+unanalysis.size();
	}
}
