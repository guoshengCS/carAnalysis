package javabean;

import javabean.db.Template;


/**
 * ��ȡ����concept����ģ�����ı��е���ʼλ��
 * @author syq 
 */
public class MatchedResult {
	private int start;
	private int end;
	private Template t;
	
	public MatchedResult()
    {
    	this.start=-1;
    	this.end=-1;
    	t = new Template();
    }
	
    public MatchedResult(int start,int end)
    {
    	this.start=start;
    	this.end=end;
    }
	
    public MatchedResult(int start,int end,Template t)
    {
    	this.start=start;
    	this.end=end;
    	this.t = t;
    }
	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}
	
	public Template getT() {
		return t;
	}

	public void setT(Template t) {
		this.t = t;
	}

	public boolean contains(MatchedResult mr){
		if(mr.getEnd()<=this.end+mr.getEnd()-mr.getStart()&&mr.getStart()>=this.start)
			return true;
		else
			return false;
	}
	
	public boolean cross(MatchedResult mr){
		if(mr.start>=this.start&&mr.start<this.end)
			return true;
		else if(mr.start<this.start&&mr.end>this.start)
			return true;
		else
			return false;
	}

	@Override
	public String toString() {
		return "MatchedResult [start=" + start + ", end=" + end + ", t=" + t
				+ "]";
	}
	
	
}
