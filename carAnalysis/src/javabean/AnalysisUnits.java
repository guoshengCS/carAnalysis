package javabean;


import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import util.AnalysisProperties;
import util.AnalysisUtil;


/**
 * ��¼�����������е�Ԫ
 * @author Bys
 *
 */
public class AnalysisUnits implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7121931133965367661L;
	private Vector <PagesUnit> analysisUnits;
	
	public AnalysisUnits(){
		analysisUnits = new Vector<PagesUnit>();
	}
	
	
	
	/**
	 * ��id��С�����˳������µĵ�Ԫ��������Ժϲ���ϲ���ص�Ԫ
	 * @param unit
	 */
	public void add(PagesUnit unit){
		if (unit.getStart()>unit.getEnd()) {
			System.err.println((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())+":������Ԫ�������");
			return;
		}
		boolean doMerge = false;
		if(analysisUnits.size() == 0){
			analysisUnits.add(unit);
		}
		else{
			//�Ӻ���ǰѰ�ҿ��Բ����λ�ӣ������һ��֮����ҵ���һ��֮��
			for(int i = analysisUnits.size()-1;i>=0;i--){
				if(analysisUnits.get(i).getStart()<unit.getStart()){

					//2��Χ�н���
					if(analysisUnits.get(i).getEnd()>unit.getStart()){
						System.err.println((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())+":������Ԫ�������");
						AnalysisProperties.storeRecord(this, (new SimpleDateFormat("MM-dd-HH-mm-ss")).format(new Date())+".log");
						unit.setStart(analysisUnits.get(i).getEnd()+1);
					}
						
					doMerge = false;
					//����ͺ���һ����Ԫ�ڽ���ϲ�2����Ԫ
					if(i<analysisUnits.size()-1&&analysisUnits.get(i+1).getStart()==unit.getEnd()){
						analysisUnits.get(i+1).setStart(unit.getStart());
						analysisUnits.get(i+1).addUnanalysis(unit.getUnanalysis());
						doMerge = true;
					}
					//�����ǰһ����Ԫ�ڽ�Ҳ�ϲ�2����Ԫ
					if(analysisUnits.get(i).getEnd()==unit.getStart()){
						if(!doMerge){
							analysisUnits.get(i).setEnd(unit.getEnd());
							analysisUnits.get(i).addUnanalysis(unit.getUnanalysis());
							doMerge = true;
						}
						//����Ѿ��ͺ�һ����Ԫ�ϲ������ֿ��Ժ�ǰһ���ϲ�����ϲ�3����Ԫ
						else{
							analysisUnits.get(i+1).setStart(analysisUnits.get(i).getStart());
							analysisUnits.get(i+1).addUnanalysis(analysisUnits.get(i).getUnanalysis());
							analysisUnits.remove(i);
						}
					}
					//�����ǰ�󶼲��ܺϲ���ֱ�Ӳ���
					if(!doMerge)
						analysisUnits.add(i+1, unit);
					return;
				}
			}
			//������Ժ͵�һ���ϲ�
			if(analysisUnits.get(0).getStart()==unit.getEnd()){
				analysisUnits.get(0).setStart(unit.getStart());
				analysisUnits.get(0).addUnanalysis(unit.getUnanalysis());
			}
			//����͵�һ����¼�г�ͻ
			else if(analysisUnits.get(0).getStart()<unit.getEnd()){
				System.err.println((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())+":������Ԫ�������");
				AnalysisProperties.storeRecord(this, (new SimpleDateFormat("MM-dd-HH-mm-ss")).format(new Date())+".log");
				unit.setEnd(analysisUnits.get(0).getStart()-1);
			}
			else
				analysisUnits.add(0, unit);
		}
	}
	
	/**
	 * �ӷ�����Ԫ���Ƴ�һƪ�ĵ�����Ϊ���ĵ��ѱ���������
	 * @param id ��Ҫ�Ƴ����ĵ�id
	 * @param downloadDate ����ʱ�䣬�������Ҹ�id�ĵ����ڵ�Ԫ
	 */
	public synchronized void removePage(String id,Date downloadDate){
		long time_num = AnalysisUtil.formatTimelong(downloadDate.getTime());
		System.out.println("id("+id+")׼�������downloadDate:"+AnalysisUtil.getTimeString(downloadDate.getTime()));
		for(int i = analysisUnits.size()-1;i>=0;i--){
			if(time_num<=analysisUnits.get(i).getEnd()&&time_num>analysisUnits.get(i).getStart()){
				synchronized(analysisUnits.get(i).getUnanalysis()){
					if(!analysisUnits.get(i).getUnanalysis().contains(id)){
						System.err.println("id"+id+"�����ڣ�"+analysisUnits.get(i).getUnanalysis().size());
						return;
					}
					else{
						analysisUnits.get(i).getUnanalysis().remove(analysisUnits.get(i).getUnanalysis().indexOf(id));
						System.out.println("id"+id+"���Ƴ���"+analysisUnits.get(i).getUnanalysis().size());
						return;
					}
				}
			}
				
		}
		System.err.println("id"+id+"���ڷ�Χ�ڣ�");

	}
	
	public PagesUnit get(int index){
		return analysisUnits.get(index);
	}
	
	public int size(){
		return analysisUnits.size();
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return analysisUnits.toString();
	}
}
